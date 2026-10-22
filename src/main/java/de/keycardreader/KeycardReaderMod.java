package de.keycardreader;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class KeycardReaderMod implements ModInitializer {
    /** Tag that selects the visual/model. */
    public static final String CARD_TAG = "KeyCard";
    /** Per-card identity. Different values prevent otherwise identical cards from stacking. */
    public static final String CARD_SERIAL_TAG = "KeyCardSerial";

    /** Preset-Codes (Hex-Codes prüft {@link CardCodes}). */
    public static final Map<String, String> CARD_MODELS = CardCodes.PRESET_MODELS;

    /** Wie weit (in Chunks) um jeden Spieler Reader gesucht und synchronisiert werden. */
    private static final int SCAN_RADIUS_CHUNKS = 4;

    private static final SoundEvent BEEP = SoundEvent.createVariableRangeEvent(
            Identifier.fromNamespaceAndPath("minecraft", "block.note_block.pling"));

    /** Spieler, die gerade einen Reader geöffnet haben (UUID -> Position des Droppers). */
    private static final Map<UUID, BlockPos> OPEN_READERS = new HashMap<>();

    /** Zuletzt an den Spieler geschickte Reader-Liste (um nur bei Änderung zu senden). */
    private static final Map<UUID, List<ReaderStatusPayload.Entry>> LAST_SENT = new HashMap<>();
    /** Zuletzt bekannter Zustand "liegt eine Karte im Trichter?" pro Reader (für den Piepton). */
    private static final Map<ReaderKey, Boolean> LAST_HAS_CARD = new HashMap<>();

    private record ReaderKey(ResourceKey<Level> dimension, long pos) {}
    private record Scanned(Level level, ReaderStatusPayload.Entry entry) {}

    @Override
    public void onInitialize() {
        ReaderConfig.load();

        PayloadTypeRegistry.clientboundPlay().register(ReaderPayload.TYPE, ReaderPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ReaderStatusPayload.TYPE, ReaderStatusPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenCardifyPayload.TYPE, OpenCardifyPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CardifyRequestPayload.TYPE, CardifyRequestPayload.CODEC);

        // Auswahl aus dem /cardify-Menü
        ServerPlayNetworking.registerGlobalReceiver(CardifyRequestPayload.TYPE,
                (payload, context) -> createCard(context.player(),
                        payload.code(), payload.name(), payload.serial()));

        // Rechtsklick auf einen Dropper: Reader-Info sofort schicken (oder Anzeige ausblenden).
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            BlockPos pos = hitResult.getBlockPos();
            if (level.getBlockEntity(pos) instanceof DropperBlockEntity dropper) {
                if (isReader(dropper)) {
                    OPEN_READERS.put(sp.getUUID(), pos);
                    sendCards(sp, level, pos);
                } else {
                    OPEN_READERS.remove(sp.getUUID());
                    ServerPlayNetworking.send(sp, new ReaderPayload(false, List.of()));
                }
            }
            return InteractionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int tick = server.getTickCount();
            if (tick % 10 == 0) updateOpenReaders(server);   // Menü-Anzeige alle 0,5 s
            if (tick % 10 == 5) scanReaders(server, tick);    // Lampe/Karten außen + Piepton
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // /cardify öffnet nur das Menü; die Karte entsteht erst nach "Erstellen".
            dispatcher.register(Commands.literal("cardify")
                .executes(context -> {
                    if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                        context.getSource().sendFailure(Component.literal("Dieser Befehl kann nur ein Spieler benutzen."));
                        return 0;
                    }
                    ItemStack held = player.getMainHandItem();
                    if (held.isEmpty() || !held.is(Items.PAPER)) {
                        player.sendSystemMessage(Component.literal("Halte Papier in der Haupthand, bevor du /cardify benutzt."));
                        return 0;
                    }
                    ServerPlayNetworking.send(player, OpenCardifyPayload.INSTANCE);
                    return Command.SINGLE_SUCCESS;
                }));

            dispatcher.register(Commands.literal("discardify")
                .executes(context -> {
                    if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                        context.getSource().sendFailure(Component.literal("Dieser Befehl kann nur ein Spieler benutzen."));
                        return 0;
                    }
                    ItemStack stack = player.getMainHandItem();
                    if (stack.isEmpty() || !stack.is(Items.PAPER)) {
                        player.sendSystemMessage(Component.literal("Halte eine Keycard in der Haupthand."));
                        return 0;
                    }
                    CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
                    if (custom == null) {
                        player.sendSystemMessage(Component.literal("Dieses Papier ist keine Keycard."));
                        return 0;
                    }
                    CompoundTag tag = custom.copyTag();
                    String code = tag.getString(CARD_TAG).orElse("");
                    if (!CardCodes.isValid(code)) {
                        player.sendSystemMessage(Component.literal("Dieses Papier ist keine gültige Keycard."));
                        return 0;
                    }
                    tag.remove(CARD_TAG);
                    tag.remove(CARD_SERIAL_TAG);
                    if (tag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
                    else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    CardCodes.removeVisual(stack);
                    stack.remove(DataComponents.CUSTOM_NAME);
                    player.sendSystemMessage(Component.literal("Keycard wurde wieder zu normalem Papier."));
                    return Command.SINGLE_SUCCESS;
                }));
        });
    }

    /** Wendet die Menü-Auswahl auf das Papier in der Haupthand an (alles wird serverseitig geprüft). */
    private static void createCard(ServerPlayer player, String code, String name, String serial) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !held.is(Items.PAPER)) {
            player.sendSystemMessage(Component.literal("Halte Papier in der Haupthand, um eine Keycard zu erstellen."));
            return;
        }
        code = code.toUpperCase(Locale.ROOT);
        if (!CardCodes.isValid(code)) {
            player.sendSystemMessage(Component.literal("Ungültiger Kartencode."));
            return;
        }
        if (serial.length() < 4 || serial.length() > 51 || !serial.matches("[A-Za-z0-9_-]+")) {
            player.sendSystemMessage(Component.literal("Der Spezialcode muss 4–51 Zeichen lang sein und darf nur Buchstaben, Zahlen, _ oder - enthalten."));
            return;
        }
        name = name.trim();
        if (name.isEmpty()) name = code;
        if (name.length() > 50) name = name.substring(0, 50);

        int count = held.getCount();
        // Der ganze Papierstapel wird zu einem Stapel identischer Karten (gleicher Spezialcode).
        CompoundTag tag = new CompoundTag();
        tag.putString(CARD_TAG, code);
        tag.putString(CARD_SERIAL_TAG, serial);
        held.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        CardCodes.applyVisual(held, code);
        held.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(CardCodes.nameStyle(code)));

        player.sendSystemMessage(Component.literal(count + " Keycard(s) erstellt: " + name + " (" + code
                + ") | Spezialcode: " + serial));
    }

    private static boolean isReader(DropperBlockEntity dropper) {
        return dropper.getCustomName() != null
                && ReaderConfig.isReaderName(dropper.getCustomName().getString());
    }

    // ------------------------------------------------------------------ Menü-Anzeige

    /** Solange das Reader-Menü offen ist, regelmäßig aktualisieren (Karte rein/raus). */
    private static void updateOpenReaders(MinecraftServer server) {
        if (OPEN_READERS.isEmpty()) return;
        Iterator<Map.Entry<UUID, BlockPos>> it = OPEN_READERS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, BlockPos> e = it.next();
            ServerPlayer sp = server.getPlayerList().getPlayer(e.getKey());
            if (sp == null || !(sp.containerMenu instanceof DispenserMenu)) {
                it.remove();
                continue;
            }
            Level level = sp.level();
            if (!(level.getBlockEntity(e.getValue()) instanceof DropperBlockEntity d) || !isReader(d)) {
                it.remove();
                ServerPlayNetworking.send(sp, new ReaderPayload(false, List.of()));
                continue;
            }
            sendCards(sp, level, e.getValue());
        }
    }

    /** Sammelt alle gültigen Keycards (je Variante einmal) aus dem Trichter direkt unter dem Reader. */
    private static void sendCards(ServerPlayer player, Level level, BlockPos readerPos) {
        List<ItemStack> found = new ArrayList<>();
        BlockEntity below = level.getBlockEntity(readerPos.below());
        if (below instanceof HopperBlockEntity hopper) {
            for (int i = 0; i < hopper.getContainerSize(); i++) {
                ItemStack stack = hopper.getItem(i);
                String code = codeOf(stack);
                if (code == null) continue;
                boolean duplicate = false;
                for (ItemStack other : found) {
                    if (ItemStack.isSameItemSameComponents(other, stack)) { duplicate = true; break; }
                }
                if (duplicate) continue;
                ItemStack copy = stack.copyWithCount(1);
                // Ohne eigenen Namen würde der Tooltip nur "Papier" zeigen -> Code als Namen setzen.
                if (copy.get(DataComponents.CUSTOM_NAME) == null) {
                    copy.set(DataComponents.CUSTOM_NAME, Component.literal(code).withStyle(CardCodes.nameStyle(code)));
                }
                found.add(copy);
            }
        }
        ServerPlayNetworking.send(player, new ReaderPayload(true, found));
    }

    // ------------------------------------------------------------------ Außenanzeige + Piepton

    /**
     * Sucht Reader um alle Spieler, schickt jedem Spieler die Liste (Lampe + Karten außen am Reader)
     * und spielt einen Piepton, wenn eine Karte in den Trichter gelegt wurde.
     */
    private static void scanReaders(MinecraftServer server, int tick) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        Set<UUID> online = new HashSet<>();
        for (ServerPlayer sp : players) online.add(sp.getUUID());
        LAST_SENT.keySet().retainAll(online);
        if (players.isEmpty()) {
            LAST_HAS_CARD.clear();
            return;
        }

        Map<ReaderKey, Scanned> scanned = new HashMap<>();
        Map<UUID, List<ReaderKey>> perPlayer = new HashMap<>();

        for (ServerPlayer sp : players) {
            Level level = sp.level();
            int pcx = sp.blockPosition().getX() >> 4;
            int pcz = sp.blockPosition().getZ() >> 4;
            List<ReaderKey> keys = new ArrayList<>();
            for (int dx = -SCAN_RADIUS_CHUNKS; dx <= SCAN_RADIUS_CHUNKS; dx++) {
                for (int dz = -SCAN_RADIUS_CHUNKS; dz <= SCAN_RADIUS_CHUNKS; dz++) {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(pcx + dx, pcz + dz);
                    if (chunk == null) continue;
                    for (BlockEntity be : new ArrayList<>(chunk.getBlockEntities().values())) {
                        if (!(be instanceof DropperBlockEntity d) || !isReader(d)) continue;
                        BlockPos pos = d.getBlockPos().immutable();
                        ReaderKey key = new ReaderKey(level.dimension(), pos.asLong());
                        scanned.computeIfAbsent(key,
                                k -> new Scanned(level, new ReaderStatusPayload.Entry(pos, codesBelow(level, pos))));
                        keys.add(key);
                    }
                }
            }
            perPlayer.put(sp.getUUID(), keys);
        }

        // Piepton: nur wenn der Reader vorher leer war und jetzt eine Karte enthält.
        for (Map.Entry<ReaderKey, Scanned> e : scanned.entrySet()) {
            boolean has = !e.getValue().entry().codes().isEmpty();
            Boolean before = LAST_HAS_CARD.get(e.getKey());
            if (ReaderConfig.soundEnabled() && before != null && !before && has) {
                BlockPos pos = e.getValue().entry().pos();
                e.getValue().level().playSound(null, pos, BEEP, SoundSource.BLOCKS, 0.6f, 1.6f);
            }
        }
        LAST_HAS_CARD.keySet().retainAll(scanned.keySet());
        for (Map.Entry<ReaderKey, Scanned> e : scanned.entrySet()) {
            LAST_HAS_CARD.put(e.getKey(), !e.getValue().entry().codes().isEmpty());
        }

        // Senden, wenn sich etwas geändert hat (zusätzlich alle 10 s zur Sicherheit).
        boolean forceResend = tick % 200 == 5;
        for (ServerPlayer sp : players) {
            List<ReaderStatusPayload.Entry> list = new ArrayList<>();
            for (ReaderKey key : perPlayer.get(sp.getUUID())) list.add(scanned.get(key).entry());
            list.sort(Comparator.comparingLong(en -> en.pos().asLong()));
            List<ReaderStatusPayload.Entry> previous = LAST_SENT.getOrDefault(sp.getUUID(), List.of());
            if (forceResend || !list.equals(previous)) {
                ServerPlayNetworking.send(sp, new ReaderStatusPayload(list));
                LAST_SENT.put(sp.getUUID(), list);
            }
        }
    }

    /** Bis zu 2 verschiedene Kartencodes aus dem Trichter unter dem Reader. */
    private static List<String> codesBelow(Level level, BlockPos readerPos) {
        Set<String> codes = new LinkedHashSet<>();
        if (level.getBlockEntity(readerPos.below()) instanceof HopperBlockEntity hopper) {
            for (int i = 0; i < hopper.getContainerSize() && codes.size() < 2; i++) {
                String code = codeOf(hopper.getItem(i));
                if (code != null) codes.add(code);
            }
        }
        return new ArrayList<>(codes);
    }

    /** Liefert den Kartencode, falls der Stack eine gültige Keycard ist, sonst null. */
    private static String codeOf(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(Items.PAPER)) return null;
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) return null;
        String code = custom.copyTag().getString(CARD_TAG).orElse("");
        return CardCodes.isValid(code) ? code : null;
    }
}

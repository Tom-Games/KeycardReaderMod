package de.keycardreader;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class KeycardReaderMod implements ModInitializer {
    /** Tag that selects the visual/model. */
    public static final String CARD_TAG = "KeyCard";
    /** Per-card identity. Different values prevent otherwise identical cards from stacking. */
    public static final String CARD_SERIAL_TAG = "KeyCardSerial";

    /** Name, den ein Dropper tragen muss, um als Reader zu gelten. */
    public static final String READER_NAME = "KeyCard einlegen";

    // Codes are deliberately whitelisted: arbitrary strings never select models.
    public static final Map<String, String> CARD_MODELS = Map.ofEntries(
        Map.entry("KC-B-B", "kc-b-b"), Map.entry("KC-B-G", "kc-b-g"),
        Map.entry("KC-B-P", "kc-b-p"), Map.entry("KC-B-R", "kc-b-r"),
        Map.entry("KC-B-Y", "kc-b-y"), Map.entry("KC-W-B", "kc-w-b"),
        Map.entry("KC-W-G", "kc-w-g"), Map.entry("KC-W-P", "kc-w-p"),
        Map.entry("KC-W-R", "kc-w-r"), Map.entry("KC-W-Y", "kc-w-y")
    );

    /** Spieler, die gerade einen Reader geöffnet haben (UUID -> Position des Droppers). */
    private static final Map<UUID, BlockPos> OPEN_READERS = new HashMap<>();

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(ReaderPayload.TYPE, ReaderPayload.CODEC);
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

        // Solange das Reader-Menü offen ist, alle 10 Ticks aktualisieren (Karte rein/raus).
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 10 != 0 || OPEN_READERS.isEmpty()) return;
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
                    if (!CARD_MODELS.containsKey(code)) {
                        player.sendSystemMessage(Component.literal("Dieses Papier ist keine gültige Keycard."));
                        return 0;
                    }
                    tag.remove(CARD_TAG);
                    tag.remove(CARD_SERIAL_TAG);
                    if (tag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
                    else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("minecraft", "paper"));
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
        String model = CARD_MODELS.get(code);
        if (model == null) {
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
        held.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("keycardreader", model));
        held.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(colorFor(code)));

        player.sendSystemMessage(Component.literal(count + " Keycard(s) erstellt: " + name + " (" + code
                + ") | Spezialcode: " + serial));
    }

    /** Namensfarbe passend zum letzten Buchstaben des Codes (B/G/P/R/Y). */
    public static ChatFormatting colorFor(String code) {
        return switch (code.charAt(code.length() - 1)) {
            case 'B' -> ChatFormatting.DARK_BLUE;
            case 'G' -> ChatFormatting.DARK_GREEN;
            case 'P' -> ChatFormatting.DARK_PURPLE;
            case 'R' -> ChatFormatting.DARK_RED;
            case 'Y' -> ChatFormatting.GOLD;
            default -> ChatFormatting.WHITE;
        };
    }

    private static boolean isReader(DropperBlockEntity dropper) {
        return dropper.getCustomName() != null
                && READER_NAME.equals(dropper.getCustomName().getString());
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
                    copy.set(DataComponents.CUSTOM_NAME, Component.literal(code).withStyle(colorFor(code)));
                }
                found.add(copy);
            }
        }
        ServerPlayNetworking.send(player, new ReaderPayload(true, found));
    }

    /** Liefert den Kartencode, falls der Stack eine gültige Keycard ist, sonst null. */
    private static String codeOf(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(Items.PAPER)) return null;
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) return null;
        String code = custom.copyTag().getString(CARD_TAG).orElse("");
        return CARD_MODELS.containsKey(code) ? code : null;
    }
}

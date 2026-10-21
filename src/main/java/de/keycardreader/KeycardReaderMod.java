package de.keycardreader;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;

import java.util.Locale;
import java.util.Map;

public final class KeycardReaderMod implements ModInitializer {
    /** Tag that selects the visual/model. */
    public static final String CARD_TAG = "KeyCard";
    /** Per-card identity. Different values prevent otherwise identical cards from stacking. */
    public static final String CARD_SERIAL_TAG = "KeyCardSerial";

    // Codes are deliberately whitelisted: arbitrary strings never select models.
    public static final Map<String, String> CARD_MODELS = Map.ofEntries(
        Map.entry("KC-B-B", "kc-b-b"), Map.entry("KC-B-G", "kc-b-g"),
        Map.entry("KC-B-P", "kc-b-p"), Map.entry("KC-B-R", "kc-b-r"),
        Map.entry("KC-B-Y", "kc-b-y"), Map.entry("KC-W-B", "kc-w-b"),
        Map.entry("KC-W-G", "kc-w-g"), Map.entry("KC-W-P", "kc-w-p"),
        Map.entry("KC-W-R", "kc-w-r"), Map.entry("KC-W-Y", "kc-w-y")
    );

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("cardify")
                .then(Commands.argument("code", StringArgumentType.word())
                    .then(Commands.argument("specialcode", StringArgumentType.word())
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

                            String code = StringArgumentType.getString(context, "code").toUpperCase(Locale.ROOT);
                            String model = CARD_MODELS.get(code);
                            if (model == null) {
                                player.sendSystemMessage(Component.literal("Ungültiger Kartencode. Erlaubt sind KC-B-B, KC-B-G, KC-B-P, KC-B-R, KC-B-Y, KC-W-B, KC-W-G, KC-W-P, KC-W-R und KC-W-Y."));
                                return 0;
                            }

                            String serialPrefix = StringArgumentType.getString(context, "specialcode");
                            if (serialPrefix.length() < 4 || serialPrefix.length() > 51
                                    || !serialPrefix.matches("[A-Za-z0-9_-]+")) {
                                player.sendSystemMessage(Component.literal("Der Spezialcode muss 4–51 Zeichen lang sein und darf nur Buchstaben, Zahlen, _ oder - enthalten."));
                                return 0;
                            }

                            int count = held.getCount();
                            // All cards from this command intentionally share the exact same special code.
                            // Keep the existing stack intact so the whole paper stack becomes one stack
                            // of identically identified keycards, rather than assigning generated IDs.
                            CompoundTag tag = new CompoundTag();
                            tag.putString(CARD_TAG, code);
                            tag.putString(CARD_SERIAL_TAG, serialPrefix);
                            held.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                            held.set(DataComponents.ITEM_MODEL,
                                    Identifier.fromNamespaceAndPath("keycardreader", model));

                            player.sendSystemMessage(Component.literal(count + " Keycard(s) erstellt: " + code
                                    + " | Spezialcode für alle Karten: " + serialPrefix));
                            return Command.SINGLE_SUCCESS;
                        }))));

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
                    stack.remove(DataComponents.ITEM_MODEL);
                    stack.remove(DataComponents.CUSTOM_NAME);
                    player.sendSystemMessage(Component.literal("Keycard wurde wieder zu normalem Papier."));
                    return Command.SINGLE_SUCCESS;
                }));
        });
    }

}

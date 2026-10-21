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
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("cardify")
                .then(Commands.argument("code", StringArgumentType.word())
                    .then(Commands.argument("specialcode", StringArgumentType.word())
                        .executes(context -> {
                            if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                                context.getSource().sendFailure(Component.literal("Dieser Befehl kann nur ein Spieler benutzen."));
                                return 0;
                            }

                            ItemStack stack = player.getMainHandItem();
                            if (stack.isEmpty() || !stack.is(Items.PAPER)) {
                                player.sendSystemMessage(Component.literal("Halte genau ein Papier in der Haupthand, bevor du /cardify benutzt."));
                                return 0;
                            }
                            // One physical item per command: otherwise every paper in a stack would receive
                            // the same serial and would still stack with each other.
                            if (stack.getCount() != 1) {
                                player.sendSystemMessage(Component.literal("Bitte teile zuerst genau ein Papier ab. Jede Keycard braucht einen eigenen Spezialcode."));
                                return 0;
                            }

                            String code = StringArgumentType.getString(context, "code").toUpperCase(Locale.ROOT);
                            String model = CARD_MODELS.get(code);
                            if (model == null) {
                                player.sendSystemMessage(Component.literal("Ungültiger Kartencode. Erlaubt sind KC-B-B, KC-B-G, KC-B-P, KC-B-R, KC-B-Y, KC-W-B, KC-W-G, KC-W-P, KC-W-R und KC-W-Y."));
                                return 0;
                            }

                            String serial = StringArgumentType.getString(context, "specialcode");
                            if (serial.length() < 4 || serial.length() > 64 || !serial.matches("[A-Za-z0-9_-]+")) {
                                player.sendSystemMessage(Component.literal("Der Spezialcode muss 4–64 Zeichen lang sein und darf nur Buchstaben, Zahlen, _ oder - enthalten."));
                                return 0;
                            }

                            CompoundTag tag = new CompoundTag();
                            CustomData old = stack.get(DataComponents.CUSTOM_DATA);
                            if (old != null) tag = old.copyTag();
                            tag.putString(CARD_TAG, code);
                            tag.putString(CARD_SERIAL_TAG, serial);
                            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

                            // The visual code, not the editable anvil name, selects the model.
                            stack.set(DataComponents.ITEM_MODEL,
                                    Identifier.fromNamespaceAndPath("keycardreader", model));

                            player.sendSystemMessage(Component.literal("Keycard erstellt: " + code + " | Spezialcode: " + serial));
                            return Command.SINGLE_SUCCESS;
                        })))));
    }
}

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
    public static final String CARD_TAG = "KeyCard";

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
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                            context.getSource().sendFailure(Component.literal("Dieser Befehl kann nur ein Spieler benutzen."));
                            return 0;
                        }

                        ItemStack stack = player.getMainHandItem();
                        if (stack.isEmpty() || !stack.is(Items.PAPER)) {
                            player.sendSystemMessage(Component.literal("Halte Papier in der Haupthand, bevor du /cardify benutzt."));
                            return 0;
                        }

                        String code = StringArgumentType.getString(context, "code").toUpperCase(Locale.ROOT);
                        String model = CARD_MODELS.get(code);
                        if (model == null) {
                            player.sendSystemMessage(Component.literal("Ungültiger Code. Erlaubt: KC-B-B, KC-B-G, KC-B-P, KC-B-R, KC-B-Y, KC-W-B, KC-W-G, KC-W-P, KC-W-R, KC-W-Y"));
                            return 0;
                        }

                        CompoundTag tag = new CompoundTag();
                        CustomData old = stack.get(DataComponents.CUSTOM_DATA);
                        if (old != null) tag = old.copyTag();
                        tag.putString(CARD_TAG, code);
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

                        // Native item model selection; the editable anvil display name is not used.
                        stack.set(DataComponents.ITEM_MODEL,
                                Identifier.fromNamespaceAndPath("keycardreader", model));

                        player.sendSystemMessage(Component.literal("Keycard erstellt: " + code));
                        return Command.SINGLE_SUCCESS;
                    }))));
    }
}

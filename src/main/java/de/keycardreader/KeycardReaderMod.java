package de.keycardreader;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class KeycardReaderMod implements ModInitializer {
    // The presence of this key marks an item as a keycard. Its value stores the card name.
    public static final String CARD_TAG = "KeyCard";

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("cardify")
                .then(Commands.argument("name", StringArgumentType.greedyString())
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                            context.getSource().sendFailure(Component.literal("Dieser Befehl kann nur ein Spieler benutzen."));
                            return 0;
                        }

                        ItemStack stack = player.getMainHandItem();
                        if (stack.isEmpty()) {
                            player.sendSystemMessage(Component.literal("Halte zuerst ein Item in der Haupthand."));
                            return 0;
                        }

                        String cardName = StringArgumentType.getString(context, "name").trim();
                        if (cardName.isEmpty()) {
                            player.sendSystemMessage(Component.literal("Bitte gib einen Namen an: /cardify <NAME>"));
                            return 0;
                        }

                        CompoundTag tag = new CompoundTag();
                        CustomData old = stack.get(DataComponents.CUSTOM_DATA);
                        if (old != null) {
                            tag = old.copyTag();
                        }
                        // Stored in item data, not the editable display name.
                        tag.putString(CARD_TAG, cardName);
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                        player.sendSystemMessage(Component.literal("Keycard erstellt: " + cardName));
                        return Command.SINGLE_SUCCESS;
                    }))));
    }
}

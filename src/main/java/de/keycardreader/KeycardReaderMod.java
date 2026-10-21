package de.keycardreader;

import com.mojang.brigadier.Command;
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
    public static final String CARD_TAG = "KeyCard";

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("cardify")
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
                    CompoundTag tag = new CompoundTag();
                    CustomData old = stack.get(DataComponents.CUSTOM_DATA);
                    if (old != null) tag = old.copyTag();
                    tag.putBoolean(CARD_TAG, true);
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    player.sendSystemMessage(Component.literal("KeyCard-Tag zum gehaltenen Item hinzugefügt."));
                    return Command.SINGLE_SUCCESS;
                })));
    }
}

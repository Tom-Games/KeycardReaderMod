package de.keycardreader;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

public final class KeycardReaderClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(
                BlockEntityTypes.DROPPER,
                KeycardReaderRenderer::new
        );

        ClientPlayNetworking.registerGlobalReceiver(ReaderPayload.TYPE,
                (payload, context) -> ReaderOverlay.update(payload));
        ReaderOverlay.register();

        ClientPlayNetworking.registerGlobalReceiver(OpenCardifyPayload.TYPE,
                (payload, context) -> Minecraft.getInstance().setScreen(new CardifyScreen()));
        CardifyScreen.registerHooks();
    }
}

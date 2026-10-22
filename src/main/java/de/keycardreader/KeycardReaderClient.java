package de.keycardreader;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
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
        ClientPlayNetworking.registerGlobalReceiver(ReaderStatusPayload.TYPE,
                (payload, context) -> ReaderStatusClient.update(payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ReaderStatusClient.clear());
        ReaderOverlay.register();

        ClientPlayNetworking.registerGlobalReceiver(OpenCardifyPayload.TYPE,
                (payload, context) -> Minecraft.getInstance().setScreenAndShow(new CardifyScreen()));
        CardifyScreen.registerHooks();
    }
}

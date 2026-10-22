package de.keycardreader;

import net.fabricmc.api.ClientModInitializer;
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
    }
}

package de.keycardreader;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

public final class KeycardReaderClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(
                BlockEntityTypes.DROPPER,
                KeycardReaderRenderer::new
        );
    }
}

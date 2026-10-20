package de.keycardreader;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Client-only renderer registration. */
public final class KeycardReaderClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Replace the dropper's block-entity renderer with one that adds the
        // named front overlay. The ordinary static block model remains vanilla.
        BlockEntityRenderers.register(BlockEntityType.DROPPER, KeycardReaderRenderer::new);
    }
}

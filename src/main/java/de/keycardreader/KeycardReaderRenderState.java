package de.keycardreader;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

final class KeycardReaderRenderState extends BlockEntityRenderState {
    boolean isReader;
    Direction facing = Direction.NORTH;
}

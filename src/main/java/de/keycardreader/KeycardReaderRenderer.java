package de.keycardreader;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class KeycardReaderRenderer
        implements BlockEntityRenderer<DropperBlockEntity, KeycardReaderRenderState> {

    private static final Identifier FRONT = Identifier.fromNamespaceAndPath(
            "keycardreader", "textures/block/keycard_reader_front.png");
    private static final RenderType FRONT_RENDER_TYPE = RenderTypes.entityCutout(FRONT);
    private static final String READER_NAME = "KeyCard einlegen";

    public KeycardReaderRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public KeycardReaderRenderState createRenderState() {
        return new KeycardReaderRenderState();
    }

    @Override
    public void extractRenderState(DropperBlockEntity blockEntity,
            KeycardReaderRenderState state, float tickProgress, Vec3 cameraPos,
            @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(
                blockEntity, state, tickProgress, cameraPos, crumblingOverlay);
        state.isReader = blockEntity.getCustomName() != null
                && READER_NAME.equals(blockEntity.getCustomName().getString());
        state.facing = blockEntity.getBlockState().getValue(DispenserBlock.FACING);
    }

    @Override
    public void submit(KeycardReaderRenderState state, PoseStack poseStack,
            SubmitNodeCollector queue, CameraRenderState cameraState) {
        if (!state.isReader) return;

        poseStack.pushPose();
        // Block-entity render coordinates start at the block's lower corner. Move to the
        // center because the face geometry below is expressed in -0.5..0.5 coordinates.
        poseStack.translate(0.5, 0.5, 0.5);
        queue.submitCustomGeometry(poseStack, FRONT_RENDER_TYPE, (pose, vertices) -> {
            // Build the quad directly on the block's actual facing face. This avoids rotation
            // winding/culling issues and prevents the image from appearing on a side face.
            switch (state.facing) {
                case NORTH -> face(vertices, pose, -0.501f, 0, 0, -1);
                case SOUTH -> face(vertices, pose, 0.501f, 0, 0, 1);
                case WEST -> face(vertices, pose, -0.501f, -1, 0, 0);
                case EAST -> face(vertices, pose, 0.501f, 1, 0, 0);
                case UP -> face(vertices, pose, 0.501f, 0, 1, 0);
                case DOWN -> face(vertices, pose, -0.501f, 0, -1, 0);
            }
        });
        poseStack.popPose();
    }

    /** Draw the reader texture on the selected face with an explicit outward normal. */
    private static void face(com.mojang.blaze3d.vertex.VertexConsumer out,
            PoseStack.Pose pose, float fixed, int nx, int ny, int nz) {
        // For vertical (north/south) faces, fixed is Z; for east/west faces, fixed is X;
        // for top/bottom faces, fixed is Y. Keep the image upright from outside each face.
        if (nz != 0) {
            float z = fixed;
            if (nz < 0) {
                vertex(out, pose, -0.5f,  0.5f, z, 0, 0, nx, ny, nz);
                vertex(out, pose,  0.5f,  0.5f, z, 1, 0, nx, ny, nz);
                vertex(out, pose,  0.5f, -0.5f, z, 1, 1, nx, ny, nz);
                vertex(out, pose, -0.5f, -0.5f, z, 0, 1, nx, ny, nz);
            } else {
                vertex(out, pose,  0.5f,  0.5f, z, 0, 0, nx, ny, nz);
                vertex(out, pose, -0.5f,  0.5f, z, 1, 0, nx, ny, nz);
                vertex(out, pose, -0.5f, -0.5f, z, 1, 1, nx, ny, nz);
                vertex(out, pose,  0.5f, -0.5f, z, 0, 1, nx, ny, nz);
            }
        } else if (nx != 0) {
            float x = fixed;
            if (nx > 0) {
                vertex(out, pose, x, 0.5f, -0.5f, 0, 0, nx, ny, nz);
                vertex(out, pose, x, 0.5f,  0.5f, 1, 0, nx, ny, nz);
                vertex(out, pose, x, -0.5f,  0.5f, 1, 1, nx, ny, nz);
                vertex(out, pose, x, -0.5f, -0.5f, 0, 1, nx, ny, nz);
            } else {
                vertex(out, pose, x, 0.5f,  0.5f, 0, 0, nx, ny, nz);
                vertex(out, pose, x, 0.5f, -0.5f, 1, 0, nx, ny, nz);
                vertex(out, pose, x, -0.5f, -0.5f, 1, 1, nx, ny, nz);
                vertex(out, pose, x, -0.5f,  0.5f, 0, 1, nx, ny, nz);
            }
        } else {
            float y = fixed;
            if (ny > 0) {
                vertex(out, pose, -0.5f, y,  0.5f, 0, 0, nx, ny, nz);
                vertex(out, pose,  0.5f, y,  0.5f, 1, 0, nx, ny, nz);
                vertex(out, pose,  0.5f, y, -0.5f, 1, 1, nx, ny, nz);
                vertex(out, pose, -0.5f, y, -0.5f, 0, 1, nx, ny, nz);
            } else {
                vertex(out, pose, -0.5f, y, -0.5f, 0, 0, nx, ny, nz);
                vertex(out, pose,  0.5f, y, -0.5f, 1, 0, nx, ny, nz);
                vertex(out, pose,  0.5f, y,  0.5f, 1, 1, nx, ny, nz);
                vertex(out, pose, -0.5f, y,  0.5f, 0, 1, nx, ny, nz);
            }
        }
    }

    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer out,
            PoseStack.Pose pose, float x, float y, float z, float u, float v,
            int nx, int ny, int nz) {
        out.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(0)
                .setLight(15728880) // Volles Licht, damit die Textur nicht vom Blockschatten verdunkelt wird
                .setNormal(pose, nx, ny, nz);
    }
}

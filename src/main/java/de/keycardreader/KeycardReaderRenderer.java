package de.keycardreader;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class KeycardReaderRenderer
        implements BlockEntityRenderer<DropperBlockEntity, KeycardReaderRenderState> {

    private static final Identifier FRONT = Identifier.fromNamespaceAndPath(
            "keycardreader", "textures/block/keycard_reader_front.png");
    // Full-bright packed light is supplied per vertex below so the reader texture is not darkened by world lighting.
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
        poseStack.translate(0.5, 0.5, 0.5);
        switch (state.facing) {
            case SOUTH -> poseStack.rotateDegrees(Axis.YP, 180.0F);
            case EAST -> poseStack.rotateDegrees(Axis.YP, -90.0F);
            case WEST -> poseStack.rotateDegrees(Axis.YP, 90.0F);
            case UP -> poseStack.rotateDegrees(Axis.XP, 90.0F);
            case DOWN -> poseStack.rotateDegrees(Axis.XP, -90.0F);
            default -> { }
        }

        queue.submitCustomGeometry(poseStack, FRONT_RENDER_TYPE, (pose, vertices) -> {
            float z = -0.501f;
            // Counter-clockwise from the visible side; avoids the previous reversed winding.
            vertex(vertices, pose, -0.5f,  0.5f, z, 0f, 0f, 0x00F000F0);
            vertex(vertices, pose,  0.5f,  0.5f, z, 1f, 0f, 0x00F000F0);
            vertex(vertices, pose,  0.5f, -0.5f, z, 1f, 1f, 0x00F000F0);
            vertex(vertices, pose, -0.5f, -0.5f, z, 0f, 1f, 0x00F000F0);
        });
        poseStack.popPose();
    }

    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer out,
            PoseStack.Pose pose, float x, float y, float z, float u, float v, int light) {
        out.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(0)
                .setLight(light)
                .setNormal(pose, 0f, 0f, -1f);
    }
}

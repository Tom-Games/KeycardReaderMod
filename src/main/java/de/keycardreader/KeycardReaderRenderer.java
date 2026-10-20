package de.keycardreader;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Adds a transparent textured plane over the output face of named droppers. */
public final class KeycardReaderRenderer implements BlockEntityRenderer<DispenserBlockEntity, KeycardReaderRenderState> {
    private static final ResourceLocation FRONT = ResourceLocation.fromNamespaceAndPath(
            "keycardreader", "textures/block/keycard_reader_front.png");
    private static final RenderType FRONT_RENDER_TYPE = RenderType.entityCutoutNoCull(FRONT);
    private static final String READER_NAME = "Keycard-Reader";

    public KeycardReaderRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public KeycardReaderRenderState createRenderState() {
        return new KeycardReaderRenderState();
    }

    @Override
    public void extractRenderState(DispenserBlockEntity blockEntity,
                                   KeycardReaderRenderState state,
                                   float tickProgress,
                                   Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, tickProgress, cameraPos, crumblingOverlay);
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
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            default -> { }
        }

        queue.submitCustomGeometry(poseStack, FRONT_RENDER_TYPE, (pose, vertices) -> {
            // A hair outside the vanilla face prevents z-fighting.
            float z = -0.501f;
            vertex(vertices, pose, -0.5f,  0.5f, z, 0f, 0f, state.lightCoords);
            vertex(vertices, pose, -0.5f, -0.5f, z, 0f, 1f, state.lightCoords);
            vertex(vertices, pose,  0.5f, -0.5f, z, 1f, 1f, state.lightCoords);
            vertex(vertices, pose,  0.5f,  0.5f, z, 1f, 0f, state.lightCoords);
        });
        poseStack.popPose();
    }

    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer out,
                               PoseStack.Pose pose,
                               float x, float y, float z,
                               float u, float v, int light) {
        out.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(0)
                .setLight(light)
                .setNormal(pose, 0f, 0f, -1f);
    }
}

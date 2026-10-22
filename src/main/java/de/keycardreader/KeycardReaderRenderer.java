package de.keycardreader;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Zeichnet auf der Vorderseite eines Readers (Dropper, den der Server als Reader meldet):
 * die Reader-Textur, oben links eine Lampe (grün = Karte im Trichter, rot = leer)
 * und oben rechts bis zu zwei kleine Karten aus dem Trichter darunter.
 */
public final class KeycardReaderRenderer
        implements BlockEntityRenderer<DropperBlockEntity, KeycardReaderRenderState> {

    private static final Map<Identifier, RenderType> RENDER_TYPES = new HashMap<>();

    private static final Identifier FRONT = tex("textures/block/keycard_reader_front.png");
    private static final Identifier LAMP_ON = tex("textures/block/reader_lamp_on.png");
    private static final Identifier LAMP_OFF = tex("textures/block/reader_lamp_off.png");
    private static final Identifier HEX_BASE_B = tex("textures/item/kc-hex-b.png");
    private static final Identifier HEX_BASE_W = tex("textures/item/kc-hex-w.png");
    private static final Identifier HEX_OVERLAY = tex("textures/item/kc-hex-overlay.png");

    // Abstand zur Blockfläche (damit sich die Ebenen nicht überlagern/flackern)
    private static final float D_FRONT = 0.001f;
    private static final float D_LAMP = 0.002f;
    private static final float D_CARD = 0.003f;
    private static final float D_CARD_OVERLAY = 0.004f;

    // Lage auf der Fläche in 1/16-Einheiten, von links oben gemessen
    private static final float LAMP_X = 1.6f, LAMP_Y = 1.6f, LAMP_SIZE = 2.8f;
    private static final float CARD_SIZE = 3.2f, CARD_Y = 1.0f, CARD_RIGHT = 15.0f;

    public KeycardReaderRenderer(BlockEntityRendererProvider.Context context) {}

    private static Identifier tex(String path) {
        return Identifier.fromNamespaceAndPath("keycardreader", path);
    }

    private static RenderType renderType(Identifier texture) {
        return RENDER_TYPES.computeIfAbsent(texture, RenderTypes::entityTranslucent);
    }

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
        // Der Server entscheidet, welche Dropper Reader sind (Name + Config) und was darunter liegt.
        List<String> codes = ReaderStatusClient.get(blockEntity.getBlockPos());
        state.isReader = codes != null;
        state.cards = codes == null ? List.of() : codes;
        state.lit = !state.cards.isEmpty();
        state.facing = blockEntity.getBlockState().getValue(DispenserBlock.FACING);
    }

    @Override
    public void submit(KeycardReaderRenderState state, PoseStack poseStack,
            SubmitNodeCollector queue, CameraRenderState cameraState) {
        if (!state.isReader) return;

        final Direction facing = state.facing;
        poseStack.pushPose();
        // Block-entity render coordinates start at the block's lower corner. Move to the
        // center because the geometry below is expressed in -0.5..0.5 coordinates.
        poseStack.translate(0.5, 0.5, 0.5);

        // 1) Reader-Front
        queue.submitCustomGeometry(poseStack, renderType(FRONT), (pose, out) ->
                rect(out, pose, facing, D_FRONT, 0f, 0f, 1f, 1f, 255, 255, 255));

        // 2) Lampe oben links
        queue.submitCustomGeometry(poseStack, renderType(state.lit ? LAMP_ON : LAMP_OFF), (pose, out) ->
                rect(out, pose, facing, D_LAMP,
                        LAMP_X / 16f, LAMP_Y / 16f, (LAMP_X + LAMP_SIZE) / 16f, (LAMP_Y + LAMP_SIZE) / 16f,
                        255, 255, 255));

        // 3) Karten oben rechts (erste Karte ganz rechts)
        int n = Math.min(2, state.cards.size());
        for (int i = 0; i < n; i++) {
            final String code = state.cards.get(i);
            final float x1 = (CARD_RIGHT - i * CARD_SIZE) / 16f;
            final float x0 = x1 - CARD_SIZE / 16f;
            final float y0 = CARD_Y / 16f;
            final float y1 = (CARD_Y + CARD_SIZE) / 16f;

            if (CardCodes.isHex(code)) {
                final int rgb = CardCodes.rgbOf(code);
                final Identifier base = code.charAt(3) == 'B' ? HEX_BASE_B : HEX_BASE_W;
                queue.submitCustomGeometry(poseStack, renderType(base), (pose, out) ->
                        rect(out, pose, facing, D_CARD, x0, y0, x1, y1, 255, 255, 255));
                queue.submitCustomGeometry(poseStack, renderType(HEX_OVERLAY), (pose, out) ->
                        rect(out, pose, facing, D_CARD_OVERLAY, x0, y0, x1, y1,
                                (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
            } else {
                String model = CardCodes.modelOf(code);
                if (model == null) continue;
                final Identifier icon = tex("textures/item/" + model + ".png");
                queue.submitCustomGeometry(poseStack, renderType(icon), (pose, out) ->
                        rect(out, pose, facing, D_CARD, x0, y0, x1, y1, 255, 255, 255));
            }
        }

        poseStack.popPose();
    }

    /**
     * Rechteck auf der Blockfläche. u/v: 0..1 auf der Fläche, von außen gesehen, u nach rechts, v nach unten.
     * Die Textur wird komplett (0..1) auf das Rechteck gelegt.
     */
    private static void rect(VertexConsumer out, PoseStack.Pose pose, Direction face, float depth,
            float u0, float v0, float u1, float v1, int r, int g, int b) {
        vertex(out, pose, face, depth, u0, v0, 0f, 0f, r, g, b);
        vertex(out, pose, face, depth, u0, v1, 0f, 1f, r, g, b);
        vertex(out, pose, face, depth, u1, v1, 1f, 1f, r, g, b);
        vertex(out, pose, face, depth, u1, v0, 1f, 0f, r, g, b);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, Direction face, float depth,
            float u, float v, float texU, float texV, int r, int g, int b) {
        float o = 0.5f + depth;
        float x, y, z;
        int nx = 0, ny = 0, nz = 0;
        switch (face) {
            case NORTH -> { x = 0.5f - u;  y = 0.5f - v; z = -o;         nz = -1; }
            case SOUTH -> { x = -0.5f + u; y = 0.5f - v; z = o;          nz = 1; }
            case EAST  -> { x = o;         y = 0.5f - v; z = 0.5f - u;   nx = 1; }
            case WEST  -> { x = -o;        y = 0.5f - v; z = -0.5f + u;  nx = -1; }
            case UP    -> { x = -0.5f + u; y = o;        z = -0.5f + v;  ny = 1; }
            default    -> { x = 0.5f - u;  y = -o;       z = -0.5f + v;  ny = -1; } // DOWN
        }
        out.addVertex(pose, x, y, z)
                .setColor(r, g, b, 255)
                .setUv(texU, texV)
                .setOverlay(OverlayTexture.NO_OVERLAY) // 0 wäre die rote Treffer-Overlay-Zeile
                .setLight(15728880) // Volles Licht, damit die Textur nicht vom Blockschatten verdunkelt wird
                .setNormal(pose, nx, ny, nz);
    }
}

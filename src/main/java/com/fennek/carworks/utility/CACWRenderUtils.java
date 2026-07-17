package com.fennek.carworks.utility;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class CACWRenderUtils {

    /**
     * Helper method to map local X and Z offsets to global space
     * based on the block's horizontal facing direction.
     * Assumes NORTH is the default base orientation.
     * Only valid for single position-offset remaps - NOT for composing rotations.
     */
    public static float[] getRotatedContext(float x, float z, Direction facing) {
        switch (facing) {
            case EAST:
                return new float[]{-z, x};
            case SOUTH:
                return new float[]{-x, -z};
            case WEST:
                return new float[]{z, -x};
            case NORTH:
            default:
                return new float[]{x, z};
        }
    }

    /**
     * Facing angle in degrees matching getRotatedContext's convention:
     * NORTH = 0 (base), EAST = 90, SOUTH = 180, WEST = 270.
     */
    public static float getFacingAngleDegrees(Direction facing) {
        switch (facing) {
            case EAST:
                return -90f;
            case SOUTH:
                return 180f;
            case WEST:
                return -270f; // equivalent to +90f, but keeping the pattern explicit
            case NORTH:
            default:
                return 0f;
        }
    }

    public static void translatePartialModelInPixels(SuperByteBuffer partialModel, float x, float y, float z, Direction facing) {
        float[] rotated = getRotatedContext(x, z, facing);
        partialModel.translate(rotated[0] / 16f, y / 16f, rotated[1] / 16f);
    }

    public static void translatePartialModelInBlocks(SuperByteBuffer partialModel, float x, float y, float z, Direction facing) {
        float[] rotated = getRotatedContext(x, z, facing);
        partialModel.translate(rotated[0], y, rotated[1]);
    }

    /**
     * Applies a local rotation stack (tilt on X, then Y, then Z) that stays
     * consistent regardless of facing, by conjugating the whole stack with
     * the facing's Y rotation instead of remapping each axis independently.
     * This is required because rotations don't commute - remapping axes
     * one at a time silently reorders rotations relative to each other.
     */
    public static void rotateModelByTheBaSeInDegrees(SuperByteBuffer partialModel, float x, float y, float z, Direction facing) {
        float facingAngle = getFacingAngleDegrees(facing);
        partialModel
                .translate(0.5, 0, 0.5)
                .rotateY(AngleHelper.rad(facingAngle))
                .rotateX(AngleHelper.rad(x))
                .rotateY(AngleHelper.rad(y))
                .rotateZ(AngleHelper.rad(z))
                .rotateY(AngleHelper.rad(-facingAngle))
                .translate(-0.5, 0, -0.5);
    }

    public static void rotateModelByTheBaSeInRadians(SuperByteBuffer partialModel, float x, float y, float z, Direction facing) {
        float facingAngle = AngleHelper.rad(getFacingAngleDegrees(facing));
        partialModel
                .translate(0.5, 0, 0.5)
                .rotateY(facingAngle)
                .rotateX(x)
                .rotateY(y)
                .rotateZ(z)
                .rotateY(-facingAngle)
                .translate(-0.5, 0, -0.5);
    }

    public static void renderItemPixels(PoseStack ms, MultiBufferSource buffer, int light, int overlay,
                                        ItemStack itemStack, Vec3 position, Vec3 rotation, Vec3 scale, Direction facing) {

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel bakedModel = itemRenderer.getModel(itemStack, null, null, 0);

        ms.pushPose();

        float facingAngle = getFacingAngleDegrees(facing);

        // rotate the whole frame once, pivoting around block center
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(facingAngle));
        ms.translate(-0.5, -0.5, -0.5);

        // now everything below happens in the already-rotated frame - no getRotatedContext needed
        ms.translate(position.x / 16.0, position.y / 16.0, position.z / 16.0);
        ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees((float) rotation.x));
        ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) rotation.y));
        ms.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) rotation.z));

        ms.scale((float) scale.x, (float) scale.y, (float) scale.z);

        itemRenderer.render(itemStack, ItemDisplayContext.FIXED, false, ms, buffer, light, overlay, bakedModel);
        ms.popPose();
    }


    /**
     * Shared characters used as "filler" while a slot hasn't locked in yet
     * during renderTextTyping. Mirrors the idea of Create's flap cyclingOptions,
     * just without needing per-section config - one fixed pool for everything.
     */
    private static final String TYPING_SCRAMBLE_POOL =
            " ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.,!?-_'\"#@$%&";

    /**
     * Sets up the pose (facing rotation, position offset in pixels, local rotation, scale)
     * exactly like renderItemPixels does, so text lines up with everything else you place
     * using that convention. Returns the resulting pose matrix to draw glyphs against.
     */
    /**
     * If the longest line in text is longer than charactersBeforeAutoResize, shrinks scale
     * proportionally so that longest line keeps roughly the same total width it would have had
     * at exactly charactersBeforeAutoResize characters, instead of overflowing the space you
     * designed for. Shorter lines are NOT stretched to fill extra space - this only ever shrinks.
     * Multi-line text (separated by \n) is measured line-by-line; only the longest one matters
     * for the width calculation, since that's what would actually overflow.
     *
     * Pass charactersBeforeAutoResize <= 0 to disable this entirely (always returns scale as-is).
     */
    private static float computeAutoResizeScale(String[] lines, float scale, int charactersBeforeAutoResize) {
        if (charactersBeforeAutoResize <= 0) {
            return scale;
        }
        int longestLine = 0;
        for (String line : lines) {
            longestLine = Math.max(longestLine, line.length());
        }
        if (longestLine <= charactersBeforeAutoResize) {
            return scale;
        }
        return scale * (charactersBeforeAutoResize / (float) longestLine);
    }

    private static Matrix4f prepareTextPose(PoseStack ms, Vec3 position, Vec3 rotation, float scale, Direction facing) {
        float facingAngle = getFacingAngleDegrees(facing);

        // rotate the whole frame once, pivoting around block center - same convention as renderItemPixels.
        // Text glyphs have no pre-existing rotation baked in (unlike SuperByteBuffer partials from
        // CachedBuffers.partialFacing), so this must NOT use the conjugated correction pattern
        // (translate->rotateY(facing)->[stuff]->rotateY(-facing)->translate back) - that pattern
        // only works for things that already have a facing rotation to correct, and text doesn't.
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(facingAngle));
        ms.translate(-0.5, -0.5, -0.5);

        // position offset happens in the already-rotated local frame - no getRotatedContext needed
        ms.translate(position.x / 16.0, position.y / 16.0, position.z / 16.0);
        ms.mulPose(Axis.XP.rotationDegrees((float) rotation.x));
        ms.mulPose(Axis.YP.rotationDegrees((float) rotation.y));
        ms.mulPose(Axis.ZP.rotationDegrees((float) rotation.z));

        // flip Y because glyphs are authored top-down (screen space), like Create does with ms.scale(1,-1,1)
        ms.scale(scale, -scale, scale);

        return ms.last().pose();
    }

    /**
     * Renders plain, non-distorted text at the given position/rotation, following the same
     * Position/Rotation/Scale/facing convention as renderItemPixels. Supports multi-line text:
     * split lines with \n and each line renders below the previous one, using the font's
     * standard line height (scaled). `scale` of 1/32f roughly matches normal in-world text size
     * (same as Create's flap display).
     *
     * @param charactersBeforeAutoResize if the LONGEST line exceeds this, scale is shrunk
     *                                   proportionally so it still fits in roughly the same
     *                                   width. Pass 0 (or negative) to disable auto-resizing.
     */
    public static void renderTextStatic(PoseStack ms, MultiBufferSource buffer, int light,
                                        String text, int color,
                                        Vec3 position, Vec3 rotation, float scale, Direction facing,
                                        int charactersBeforeAutoResize) {

        Font font = Minecraft.getInstance().font;
        FontSet fontSet = font.getFontSet(Style.DEFAULT_FONT);

        String[] lines = text.split("\n", -1);
        float effectiveScale = charactersBeforeAutoResize >0? computeAutoResizeScale(lines, scale, charactersBeforeAutoResize) : scale;

        ms.pushPose();
        Matrix4f pose = prepareTextPose(ms, position, rotation, effectiveScale, facing);

        float r = (color >> 16 & 255) / 255f;
        float g = (color >> 8 & 255) / 255f;
        float b = (color & 255) / 255f;

        float lineHeight = font.lineHeight;

        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
            float y = lineIndex * lineHeight;
            float x = 0;
            for (int i = 0; i < line.length(); i++) {
                int codePoint = line.charAt(i);
                x += drawGlyph(buffer, fontSet, pose, codePoint, x, y, r, g, b, 1f, light);
            }
        }

        if (buffer instanceof BufferSource bs) {
            bs.endBatch(fontSet.whiteGlyph().renderType(Font.DisplayMode.NORMAL));
        }

        ms.popPose();
    }

    /**
     * Renders text that "types itself out" flap-display style: characters before revealProgress
     * are shown normally, the character currently being revealed and everything after it scrambles
     * through random-looking glyphs and renders dimmer, like Create's FlapDisplayRenderer does for
     * unresolved flaps. Supports multi-line text via \n - reveal progress runs across the whole
     * flattened text in reading order (line breaks don't count as characters, but do reset x/y),
     * so it reads like a typewriter continuing naturally onto the next line.
     *
     * @param revealProgress 0f = nothing revealed yet, 1f = fully revealed/final text. Progress is
     *                       measured against total characters across all lines combined (excluding
     *                       the \n separators themselves).
     * @param animationTime  a continuously increasing value (e.g. AnimationTickHolder.getRenderTime(level))
     *                       used to animate the scrambling; each character is offset so they don't all
     *                       flicker in lockstep.
     * @param seed           lets multiple text renders (e.g. different lines) scramble independently
     *                       instead of showing identical noise at the same tick.
     * @param charactersBeforeAutoResize if the LONGEST line exceeds this, scale is shrunk
     *                                   proportionally so it still fits in roughly the same
     *                                   width. Pass 0 (or negative) to disable auto-resizing.
     */
    public static void renderTextTyping(PoseStack ms, MultiBufferSource buffer, int light,
                                        String text, int color,
                                        Vec3 position, Vec3 rotation, float scale, Direction facing,
                                        float revealProgress, float animationTime, int seed,
                                        int charactersBeforeAutoResize) {

        Font font = Minecraft.getInstance().font;
        FontSet fontSet = font.getFontSet(Style.DEFAULT_FONT);

        String[] lines = text.split("\n", -1);
        float effectiveScale = charactersBeforeAutoResize >0? computeAutoResizeScale(lines, scale, charactersBeforeAutoResize) : scale;

        ms.pushPose();
        Matrix4f pose = prepareTextPose(ms, position, rotation, effectiveScale, facing);

        float r = (color >> 16 & 255) / 255f;
        float g = (color >> 8 & 255) / 255f;
        float b = (color & 255) / 255f;

        int totalChars = 0;
        for (String line : lines) totalChars += line.length();

        float revealedExact = revealProgress * totalChars;
        int lockedCount = (int) Math.floor(revealedExact);

        float lineHeight = font.lineHeight;
        int globalCharIndex = 0;

        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
            float lineY = lineIndex * lineHeight;
            float x = 0;

            for (int i = 0; i < line.length(); i++) {
                boolean locked = globalCharIndex < lockedCount;
                int codePoint;
                float alpha;

                if (locked) {
                    codePoint = line.charAt(i);
                    alpha = 1f;
                } else {
                    // scramble: cycle through the filler pool, offset per-character so it looks
                    // like independent flaps spinning rather than one synced flicker
                    float cycle = (animationTime / 2.5f) + globalCharIndex * 16.83f + seed * 0.75f;
                    int poolIndex = Math.floorMod((int) cycle, TYPING_SCRAMBLE_POOL.length());
                    codePoint = TYPING_SCRAMBLE_POOL.charAt(poolIndex);
                    alpha = 0.75f;
                }

                x += drawGlyph(buffer, fontSet, pose, codePoint, x, lineY, r, g, b, alpha, light);
                globalCharIndex++;
            }
        }

        if (buffer instanceof BufferSource bs) {
            bs.endBatch(fontSet.whiteGlyph().renderType(Font.DisplayMode.NORMAL));
        }

        ms.popPose();
    }

    /**
     * Draws a single glyph at local offset (x, y) and returns its advance width,
     * so callers can accumulate x for the next character. Skips empty glyphs (e.g. space)
     * for draw calls but still returns their advance so spacing stays correct.
     */
    private static float drawGlyph(MultiBufferSource buffer, FontSet fontSet, Matrix4f pose, int codePoint, float x, float y,
                                   float r, float g, float b, float a, int light) {
        GlyphInfo glyphInfo = fontSet.getGlyphInfo(codePoint, false);
        float advance = glyphInfo.getAdvance(false);

        BakedGlyph bakedGlyph = fontSet.getGlyph(codePoint);
        if (!(bakedGlyph instanceof EmptyGlyph)) {
            VertexConsumer vc = buffer.getBuffer(bakedGlyph.renderType(Font.DisplayMode.NORMAL));
            bakedGlyph.render(false, x, y, pose, vc, r, g, b, a, light);
        }

        return advance;
    }
}
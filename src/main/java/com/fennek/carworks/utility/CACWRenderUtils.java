package com.fennek.carworks.utility;

import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.core.Direction;

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
}
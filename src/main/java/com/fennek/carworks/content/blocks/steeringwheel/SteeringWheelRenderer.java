package com.fennek.carworks.content.blocks.steeringwheel;

import com.fennek.carworks.CACWPartialModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import com.fennek.carworks.utility.CACWRenderUtils;

public class SteeringWheelRenderer extends SafeBlockEntityRenderer<SteeringWheelBlockEntity> {

    public SteeringWheelRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(final SteeringWheelBlockEntity be, final float partialTicks, final PoseStack ms, final MultiBufferSource buffer, final int light, final int overlay) {
        final VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());
        final BlockState state = be.getBlockState();

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction oppositeFacing = facing.getOpposite();

        //steering wheel
        SuperByteBuffer steeringWheel = CachedBuffers.partialFacing(CACWPartialModels.STEERING_WHEEL, state, facing);
        CACWRenderUtils.translatePartialModelInPixels(steeringWheel, 0f, 15.5f, -4.1f, oppositeFacing);
        float wheelAngle = be.getSteeringRenderAngle(partialTicks);
        CACWRenderUtils.rotateModelByTheBaSeInDegrees(steeringWheel, 67.5f, wheelAngle, 0f, oppositeFacing);
        steeringWheel
                .light(light)
                .overlay(overlay)
                .renderInto(ms, vb);

        //ignition key
        SuperByteBuffer ignitionKey = CachedBuffers.partialFacing(CACWPartialModels.IGNITION_KEY, state, facing);
        CACWRenderUtils.translatePartialModelInPixels(ignitionKey, -5.1f, 12.1f, -3.4f, oppositeFacing);
        float keyAngle = be.getKeyRenderAngle(partialTicks);
        CACWRenderUtils.rotateModelByTheBaSeInDegrees(ignitionKey, 67.5f, keyAngle, 0f, oppositeFacing);
        ignitionKey
                .light(light)
                .overlay(overlay)
                .renderInto(ms, vb);

        //speed neddle
        SuperByteBuffer speedNeedle = CachedBuffers.partialFacing(CACWPartialModels.SPEED_NEEDLE, state, facing);
        CACWRenderUtils.translatePartialModelInPixels(speedNeedle, -5.05f, 14.825f, -4.15f, oppositeFacing);
        //float wheelAngle = be.getRenderAngle(partialTicks);
        CACWRenderUtils.rotateModelByTheBaSeInDegrees(speedNeedle, 67.5f, 0, 0f, oppositeFacing);
        speedNeedle
                .light(light)
                .overlay(overlay)
                .renderInto(ms, vb);

        //gas neddle
        SuperByteBuffer gasNeedle = CachedBuffers.partialFacing(CACWPartialModels.GAS_NEEDLE, state, facing);
        CACWRenderUtils.translatePartialModelInPixels(gasNeedle, 5.05f, 14.825f, -4.15f, oppositeFacing);
        //float wheelAngle = be.getRenderAngle(partialTicks);
        CACWRenderUtils.rotateModelByTheBaSeInDegrees(gasNeedle, 67.5f, 0, 0f, oppositeFacing);
        gasNeedle
                .light(light)
                .overlay(overlay)
                .renderInto(ms, vb);
    }
}



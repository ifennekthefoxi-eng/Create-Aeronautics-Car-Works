package com.fennek.carworks.content.blocks.engines.FourLineEngine;

import com.fennek.carworks.CACWPartialModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class FourLineEngineRenderer extends KineticBlockEntityRenderer<FourLineEngineBlockEntity> {

    public FourLineEngineRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(FourLineEngineBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = this.getRenderedBlockState(be);
        RenderType type = this.getRenderType(be, state);
        VertexConsumer vc = buffer.getBuffer(type);

        renderRotatingBuffer(be, this.getRotatedModel(be, state), ms, vc, light);

        this.renderFan(be, state, ms, vc, light, partialTicks);

        FilteringRenderer.renderOnBlockEntity(be, partialTicks, ms, buffer, light, overlay);
    }

    private void renderFan(FourLineEngineBlockEntity be, BlockState state, PoseStack ms, VertexConsumer vc, int light, float partialTicks) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction fanFacing = facing.getOpposite();

        SuperByteBuffer fanBuffer = CachedBuffers.partialFacing(CACWPartialModels.FOUR_LINE_ENGINE_RADIATOR_FAN, state, fanFacing);
        Direction.Axis axis = getRotationAxisOf(be);

        float interpolatedAngle = net.minecraft.util.Mth.lerp(partialTicks, be.prevIndependentFanAngle, be.independentFanAngle);

        float angle = AngleHelper.rad(interpolatedAngle);

        fanBuffer.rotateCentered(angle, axis)
                .light(light)
                .renderInto(ms, vc);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(FourLineEngineBlockEntity te, BlockState state) {
        Direction facing = te.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, te.getBlockState(), facing);
    }

    protected SuperByteBuffer rotateToFacing(SuperByteBuffer buffer, Direction facing) {
        buffer.rotateCentered(AngleHelper.rad(AngleHelper.horizontalAngle(facing)), Direction.UP);
        return buffer;
    }
}
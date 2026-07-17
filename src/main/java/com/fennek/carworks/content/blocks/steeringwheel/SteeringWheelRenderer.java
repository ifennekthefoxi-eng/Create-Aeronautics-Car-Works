package com.fennek.carworks.content.blocks.steeringwheel;

import com.fennek.carworks.CACWItems;
import com.fennek.carworks.CACWPartialModels;
import com.fennek.carworks.utility.CACWMinecraftColorCodes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import com.fennek.carworks.utility.CACWRenderUtils;
import com.simibubi.create.content.logistics.depot.DepotRenderer;

import java.util.Random;

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
        float SpeedNeddleAngle = -55+(110+(-110*be.getNormalizedSpeed()));
        CACWRenderUtils.rotateModelByTheBaSeInDegrees(speedNeedle, 67.5f, SpeedNeddleAngle, 0f, oppositeFacing);
        speedNeedle
                .light(light)
                .overlay(overlay)
                .renderInto(ms, vb);

        //gas needle
        SuperByteBuffer gasNeedle = CachedBuffers.partialFacing(CACWPartialModels.GAS_NEEDLE, state, facing);
        CACWRenderUtils.translatePartialModelInPixels(gasNeedle, 5.05f, 14.825f, -4.15f, oppositeFacing);

        // Swap GetGasTankLevel() for getGasRenderLevel(partialTicks)
        float renderGasLevel = be.getGasRenderLevel(partialTicks);
        float GasNeddleAngle = -50 + (100 + (-100 * renderGasLevel));

        CACWRenderUtils.rotateModelByTheBaSeInDegrees(gasNeedle, 67.5f, GasNeddleAngle, 0f, oppositeFacing);
        gasNeedle
                .light(light)
                .overlay(overlay)
                .renderInto(ms, vb);

        //radio
        if(!be.inventory.getStackInSlot(0).isEmpty()){
            String PlayState = "IDLE";
            int color;
            if(be.isDiscPlaying())
            {
                PlayState = "PLAYING";
                color = CACWMinecraftColorCodes.GREEN.getHexValue();
            }
            else
            {
                PlayState = "IDLE";
                color = CACWMinecraftColorCodes.BLUE.getHexValue();
            }
            CACWRenderUtils.renderItemPixels(ms,buffer,light,overlay, be.inventory.getStackInSlot(0),new Vec3(6.5,12,4.5),new Vec3(0,0,be.jukeBoxDiscRotation),new Vec3(0.1f,0.1f,0.1f),oppositeFacing);
            CACWRenderUtils.renderTextStatic(ms,buffer,light,PlayState,color,new Vec3(7.4,12.5f,4.5),new Vec3(0,0,0),0.0027f,oppositeFacing,0);
            CACWRenderUtils.renderTextStatic(ms,buffer,light,be.GetSongName(),CACWMinecraftColorCodes.WHITE.getHexValue(),new Vec3(7.4,12f,4.5),new Vec3(0,0,0),0.0027f,oppositeFacing,8);
        }
        else
        {
            CACWRenderUtils.renderTextStatic(ms,buffer,light,"NO DISC", CACWMinecraftColorCodes.WHITE.getHexValue(),new Vec3(6.7,12f,4.5),new Vec3(0,0,0),0.0027f,oppositeFacing,0);
        }

    }
}



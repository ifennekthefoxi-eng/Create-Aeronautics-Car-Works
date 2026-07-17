package com.fennek.carworks.mixins.compatmixins.offroad;

import com.fennek.carworks.compat.offroad.IWheelMountEntityOverride; // Import your interface
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlock;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WheelMountBlock.class, remap = false)
public abstract class WheelMountBlockMixin {

    @Inject(
            method = "onRemove",
            at = @At("HEAD")
    )
    private void injectIntoOnRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean isMoving, CallbackInfo ci) {

        if (state.hasBlockEntity() && state.getBlock() != newState.getBlock()) {

            // Run only on the server side to handle data changes safely
            if (!level.isClientSide()) {

                // 1. Fetch the block entity from the world coordinates
                BlockEntity blockEntity = level.getBlockEntity(pos);

                // 2. Check if it implements your interface
                if (blockEntity instanceof IWheelMountEntityOverride override) {

                    // 3. Find the nearest player within 10 blocks to attribute the message to
                    Player nearestPlayer = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 10.0, false);

                    // 4. Trigger the unlinking logic, passing this block's position
                    override.unlinkFromSteeringwheel(pos, nearestPlayer);
                }
            }
        }
    }
}
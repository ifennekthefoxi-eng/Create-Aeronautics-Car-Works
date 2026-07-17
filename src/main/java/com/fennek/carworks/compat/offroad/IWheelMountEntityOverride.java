package com.fennek.carworks.compat.offroad;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

/**
 * Implemented by the WheelMountBlockEntityMixin. Lets external code (e.g. SteeringWheelBlockEntity)
 * feed a steering/brake value into a WheelMountBlockEntity as if it were real redstone,
 * without needing a physical redstone signal in the world.
 *
 * Usage:
 *   if (level.getBlockEntity(pos) instanceof WheelMountBlockEntity wheel
 *           && wheel instanceof IWheelMountOverride override) {
 *       override.cacw$setSteerOverride(leftSignal, rightSignal);
 *       override.cacw$setBrakeOverride(brakeSignal);
 *   }
 *
 * Pass null to any setter to fall back to real world redstone again for that input.
 */
public interface IWheelMountEntityOverride {
    //variables
    BlockPos GetWheelPos();

    //steering brake methods
    void cacw$setSteerOverride(Integer left, Integer right);
    void cacw$setBrakeOverride(Integer brake);

    //custom methods
    void linkToSteeringWheel(BlockPos wheelPos,Player player);
    void unlinkFromSteeringwheel(BlockPos wheelPos,Player player);
    //void executeNewMethod(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos);
}
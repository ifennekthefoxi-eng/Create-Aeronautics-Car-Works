package com.fennek.carworks.mixins.compat.offroad;

import com.fennek.carworks.content.blocks.SmartWheelMount.SmartWheelMountBlockEntity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Forces the wheel mount's brake redstone reading to full (15) whenever the
 * attached SmartWheelMountBlockEntity has its own "braking" flag set.
 *
 * IMPORTANT: this uses @ModifyExpressionValue (MixinExtras) instead of
 * @Redirect. Drive By Wire's compat mixin already @Redirects these exact
 * Level.getSignal(...) calls to route through WireRedstoneCompat. Two
 * @Redirects on the same call site conflict and crash on startup.
 * @ModifyExpressionValue reads the result of whatever already ran at that
 * call site (vanilla getSignal OR DBW's redirected wire-aware signal) and
 * lets us override it afterwards, so both mixins can coexist regardless of
 * whether Drive By Wire is installed.
 */
@Mixin(WheelMountBlockEntity.class)
public abstract class MixinWheelMountBlockEntity {

    // Physics-side brake force, computed in sable$physicsTick():
    // final double brakeStrength = this.level.getSignal(blockPos.above(), Direction.DOWN) / 15.0;
    @ModifyExpressionValue(
            method = "sable$physicsTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getSignal(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)I"
            )
    )
    private int carworks$forcePhysicsBrakeSignal(int original) {
        return this.carworks$isSmartBraking() ? 15 : original;
    }

    // Client-side visual wheel-spin slowdown, computed in tick():
    // final double rpt = ... * (15 - this.level.getSignal(this.getBlockPos().above(), Direction.DOWN)) / 15.0;
    @ModifyExpressionValue(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getSignal(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)I"
            )
    )
    private int carworks$forceClientBrakeSignal(int original) {
        return this.carworks$isSmartBraking() ? 15 : original;
    }

    private boolean carworks$isSmartBraking() {
        return (Object) this instanceof SmartWheelMountBlockEntity smart && smart.isBraking();
    }
}
package com.fennek.carworks.mixins.compatmixins.offroad;

import com.fennek.carworks.compat.offroad.IWheelMountEntityOverride;
import com.fennek.carworks.content.blocks.engines.FourLineEngine.FourLineEngineBlockEntity;
import com.fennek.carworks.content.blocks.steeringwheel.SteeringWheelBlockEntity;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WheelMountBlockEntity.class, remap = false)
public abstract class WheelMountBlockEntityMixin implements IWheelMountEntityOverride {

    // Removed the @Shadows entirely to bypass the Mixin apply crash

    @Unique
    private Integer cacw$steerLeftOverride = null;

    @Unique
    private Integer cacw$steerRightOverride = null;

    @Unique
    private Integer cacw$brakeOverride = null;

    @Override
    @Unique
    public void cacw$setSteerOverride(Integer left, Integer right) {
        this.cacw$steerLeftOverride = left;
        this.cacw$steerRightOverride = right;
    }

    @Override
    @Unique
    public void cacw$setBrakeOverride(Integer brake) {
        this.cacw$brakeOverride = brake;
    }

    @Redirect(
            method = "getSteeringSignal",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getSignal(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)I",
                    ordinal = 0
            )
    )
    private int cacw$redirectSteerLeft(Level level, BlockPos pos, Direction dir) {
        return cacw$steerLeftOverride != null ? cacw$steerLeftOverride : level.getSignal(pos, dir);
    }

    @Redirect(
            method = "getSteeringSignal",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getSignal(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)I",
                    ordinal = 1
            )
    )
    private int cacw$redirectSteerRight(Level level, BlockPos pos, Direction dir) {
        return cacw$steerRightOverride != null ? cacw$steerRightOverride : level.getSignal(pos, dir);
    }

    @Redirect(
            method = "sable$physicsTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getSignal(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)I"
            )
    )
    private int cacw$redirectBrake(Level level, BlockPos pos, Direction dir) {
        return cacw$brakeOverride != null ? cacw$brakeOverride : level.getSignal(pos, dir);
    }

    @Unique
    private BlockPos linkedSteeringWheelPos;

    @Override
    public BlockPos GetWheelPos() {
        return this.linkedSteeringWheelPos;
    }

    @Inject(
            method = "read",
            at = @At("HEAD")
    )
    protected void ctmRead(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket, CallbackInfo ci){
        if (tag.contains("LinkedSteeringWheel")) {
            this.linkedSteeringWheelPos = BlockPos.of(tag.getLong("LinkedSteeringWheel"));
        } else {
            this.linkedSteeringWheelPos = null;
        }
    }

    @Inject(
            method = "write",
            at = @At("HEAD")
    )
    protected void ctmWrite(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket, CallbackInfo ci){
        if (this.linkedSteeringWheelPos != null) {
            tag.putLong("LinkedSteeringWheel", this.linkedSteeringWheelPos.asLong());
        }
    }

    @Override
    public void linkToSteeringWheel(BlockPos wheelPos, Player player){
        // Cast 'this' to the target class to access its methods safely
        WheelMountBlockEntity self = (WheelMountBlockEntity) (Object) this;

        this.linkedSteeringWheelPos = wheelPos;
        self.setChanged();
        self.sendData();

        if (player != null) {
            player.displayClientMessage(Component.literal("Steering Wheel Set Up"), false);
        }
    }

    @Override
    public void unlinkFromSteeringwheel(BlockPos wheelPos, Player player){
        // Cast 'this' to the target class to access its methods safely
        WheelMountBlockEntity self = (WheelMountBlockEntity) (Object) this;
        Level level = self.getLevel(); // Get the level securely

        if (this.linkedSteeringWheelPos != null && level != null) {
            if (level.getBlockEntity(this.linkedSteeringWheelPos) instanceof SteeringWheelBlockEntity steeringWheel) {
                steeringWheel.unlinkWheels(wheelPos, player);
            }
        }

        this.linkedSteeringWheelPos = null;
        self.setChanged();
        self.sendData();

        if (player != null) {
            player.displayClientMessage(Component.literal("removed myself from steering wheel"), false);
        }
    }
}
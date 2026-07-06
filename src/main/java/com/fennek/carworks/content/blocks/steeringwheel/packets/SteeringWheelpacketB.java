package com.fennek.carworks.content.blocks.steeringwheel.packets;

import com.fennek.carworks.content.blocks.steeringwheel.SteeringWheelBlockEntity;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public abstract class SteeringWheelpacketB implements ServerboundPacketPayload {
    @Nullable
    private final BlockPos steeringWheelPos;

    public SteeringWheelpacketB(@Nullable BlockPos steeringWheelPos) {
        this.steeringWheelPos = steeringWheelPos;
    }

    @Nullable
    public BlockPos getSteeringWheelPos() {
        return steeringWheelPos;
    }

    @Override
    public void handle(ServerPlayer player) {
        if (this.steeringWheelPos == null)
            return;

        BlockEntity be = player.level().getBlockEntity(steeringWheelPos);
        if (!(be instanceof SteeringWheelBlockEntity steeringWheel))
            return;

        handleSteeringWheel(player, steeringWheel);
    }

    protected abstract void handleSteeringWheel(ServerPlayer player, SteeringWheelBlockEntity steeringWheel);
}
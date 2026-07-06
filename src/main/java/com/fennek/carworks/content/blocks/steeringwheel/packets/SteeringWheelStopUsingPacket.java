package com.fennek.carworks.content.blocks.steeringwheel.packets;

import com.fennek.carworks.CACWPackets;
import com.fennek.carworks.content.blocks.steeringwheel.SteeringWheelBlockEntity;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

public class SteeringWheelStopUsingPacket extends SteeringWheelpacketB {

    public static final StreamCodec<ByteBuf, SteeringWheelStopUsingPacket> STREAM_CODEC = StreamCodec.composite(
            CatnipStreamCodecs.NULLABLE_BLOCK_POS, SteeringWheelpacketB::getSteeringWheelPos,
            SteeringWheelStopUsingPacket::new
    );

    public SteeringWheelStopUsingPacket(BlockPos steeringWheelPos) {
        super(steeringWheelPos);
    }

    @Override
    protected void handleSteeringWheel(ServerPlayer player, SteeringWheelBlockEntity steeringWheel) {
        steeringWheel.tryStopUsing(player);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CACWPackets.STEERING_WHEEL_STOP_USING;
    }
}
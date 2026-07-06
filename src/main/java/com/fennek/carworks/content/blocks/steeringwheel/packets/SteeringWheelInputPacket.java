package com.fennek.carworks.content.blocks.steeringwheel.packets;

import com.fennek.carworks.CACWPackets; // your packet registry - see note at bottom
import com.fennek.carworks.content.blocks.steeringwheel.SteeringWheelBlockEntity;
import com.fennek.carworks.content.blocks.steeringwheel.handlers.SteeringWheelServerHandler;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

public class SteeringWheelInputPacket extends SteeringWheelpacketB {

    public static final StreamCodec<ByteBuf, SteeringWheelInputPacket> STREAM_CODEC = StreamCodec.composite(
            CatnipStreamCodecBuilders.list(ByteBufCodecs.INT), p -> p.activatedButtons,
            ByteBufCodecs.BOOL, p -> p.press,
            CatnipStreamCodecs.NULLABLE_BLOCK_POS, SteeringWheelpacketB::getSteeringWheelPos,
            SteeringWheelInputPacket::new
    );

    private final List<Integer> activatedButtons;
    private final boolean press;

    public SteeringWheelInputPacket(Collection<Integer> activatedButtons, boolean press, BlockPos steeringWheelPos) {
        super(steeringWheelPos);
        this.activatedButtons = List.copyOf(activatedButtons);
        this.press = press;
    }

    @Override
    protected void handleSteeringWheel(ServerPlayer player, SteeringWheelBlockEntity steeringWheel) {
        // Only the player currently seated/using this wheel may drive it
        if (!steeringWheel.isUsedBy(player))
            return;
        if (player.isSpectator() && press)
            return;

        SteeringWheelServerHandler.receivePressed(
                player.level(),
                steeringWheel.getBlockPos(),
                player.getUUID(),
                activatedButtons.stream()
                        .map(steeringWheel::getFrequencyForButton)
                        .filter(java.util.Objects::nonNull)
                        .toList(),
                press
        );
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CACWPackets.STEERING_WHEEL_INPUT;
    }
}
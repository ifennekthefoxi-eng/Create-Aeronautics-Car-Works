package com.fennek.carworks.content.blocks.steeringwheel.packets;

import com.fennek.carworks.CACWPackets; // your packet registry - see note at bottom
import com.fennek.carworks.content.blocks.steeringwheel.SteeringWheelBlock;
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

    boolean steerLeft = false;
    boolean steerRight = false;

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

    /*@Override
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
    }*/

    @Override
    protected void handleSteeringWheel(ServerPlayer player, SteeringWheelBlockEntity steeringWheel) {
        if (!steeringWheel.isUsedBy(player))
            return;
        if (player.isSpectator() && press)
            return;

        // Throttle is a held-state action too, same treatment as steering/brake below.
        if (activatedButtons.contains(SteeringWheelBlockEntity.ThrottleIndex))
            steeringWheel.setThrottleInput(press);

        if (press && activatedButtons.contains(SteeringWheelBlockEntity.GearUpIndex))
            steeringWheel.GearUp();

        if (press && activatedButtons.contains(SteeringWheelBlockEntity.GearDownIndex))
            steeringWheel.GearDown();

        // Steering is a held-state action - update it regardless of press/release,
        // and regardless of whatever else is in activatedButtons this packet.
        if (activatedButtons.contains(SteeringWheelBlockEntity.SteeringLeftIndex))
            steeringWheel.setSteeringButton(SteeringWheelBlockEntity.SteeringLeftIndex, press);

        if (activatedButtons.contains(SteeringWheelBlockEntity.SteeringRightIndex))
            steeringWheel.setSteeringButton(SteeringWheelBlockEntity.SteeringRightIndex, press);

        if(activatedButtons.contains(SteeringWheelBlockEntity.brakeIndex))
            steeringWheel.setBrakeInput(press);

        // Ignition is a one-shot toggle, only fires on press, and has no frequency meaning
        if (press && activatedButtons.contains(SteeringWheelBlockEntity.IGNITION_INDEX))
            steeringWheel.toggleIgnition(player);

        // Everything except ignition still reaches the frequency system -
        // this includes LEFT/RIGHT, so they can still drive redstone links too
        List<Integer> remaining = activatedButtons.stream()
                .filter(i -> i != SteeringWheelBlockEntity.IGNITION_INDEX)
                .toList();

        SteeringWheelServerHandler.receivePressed(
                player.level(),
                steeringWheel.getBlockPos(),
                player.getUUID(),
                remaining.stream()
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
package com.fennek.carworks;

import java.util.Locale;

import com.fennek.carworks.content.blocks.steeringwheel.packets.SteeringWheelInputPacket;
import com.fennek.carworks.content.blocks.steeringwheel.packets.SteeringWheelStopUsingPacket;

import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public enum CACWPackets implements BasePacketPayload.PacketTypeProvider {

    STEERING_WHEEL_INPUT(SteeringWheelInputPacket.class, SteeringWheelInputPacket.STREAM_CODEC),
    STEERING_WHEEL_STOP_USING(SteeringWheelStopUsingPacket.class, SteeringWheelStopUsingPacket.STREAM_CODEC);

    private final CatnipPacketRegistry.PacketType<?> type;

    <T extends BasePacketPayload> CACWPackets(Class<T> clazz, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        String name = this.name().toLowerCase(Locale.ROOT);
        this.type = new CatnipPacketRegistry.PacketType<>(
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CreateAeronauticsCarWorks.ID, name)),
                clazz, codec
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> CustomPacketPayload.Type<T> getType() {
        return (CustomPacketPayload.Type<T>) this.type.type();
    }

    public static void register() {
        CatnipPacketRegistry packetRegistry = new CatnipPacketRegistry(CreateAeronauticsCarWorks.ID, CreateAeronauticsCarWorks.VERSION);
        for (CACWPackets packet : CACWPackets.values()) {
            packetRegistry.registerPacket(packet.type);
        }
        packetRegistry.registerAllPackets();
    }
}
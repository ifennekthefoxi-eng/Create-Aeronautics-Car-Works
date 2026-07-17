package com.fennek.carworks.content.blocks.SmartWheelMount;

import com.fennek.carworks.content.blocks.steeringwheel.SteeringWheelBlockEntity;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlock;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;

public class SmartWheelMountBlockEntity extends WheelMountBlockEntity {

    private BlockPos linkedSteeringWheelPos = null;
    private int steeringDirection = 0;
    private int clientSteeringDirection = 0;
    private boolean braking = false;

    // Thread-safe cache for redstone inputs to satisfy Drive By Wire
    private int cachedRedstoneBrakeSignal = 0;

    public SmartWheelMountBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        // Query redstone safely on the main server thread before physics executes
        if (this.level != null && !this.level.isClientSide) {
            this.cachedRedstoneBrakeSignal = this.level.getSignal(this.getBlockPos().above(), Direction.UP);
        }
        super.tick();
    }

    public void turn(int direction) {
        this.steeringDirection = Mth.clamp(direction, -15, 15);
        this.setChanged();
        this.sendData();
    }

    public void setBraking(boolean braking) {
        if (this.braking != braking) {
            this.braking = braking;
            this.setChanged();
            this.sendData();
        }
    }

    public void performBrake() {
        this.setBraking(!this.braking);
    }

    public boolean isBraking() {
        return this.braking;
    }

    @Override
    protected double computeYaw() {
        final int signal = this.getSteeringSignal();
        if (signal == 0) return 0.0;
        return (-signal / 15.0 * Math.PI / 4.0 * (30.0 / 45.0));
    }

    @Override
    protected int getSteeringSignal() {
        int redstoneSignal = super.getSteeringSignal();
        int smartSignal = this.level.isClientSide ? this.clientSteeringDirection : this.steeringDirection;
        return Mth.clamp(redstoneSignal + smartSignal, -15, 15);
    }

    @Override
    protected void write(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("SmartSteeringDirection", this.steeringDirection);
        tag.putBoolean("SmartBraking", this.braking);
        if (this.linkedSteeringWheelPos != null) {
            tag.putLong("LinkedSteeringWheel", this.linkedSteeringWheelPos.asLong());
        }
    }

    @Override
    protected void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("SmartSteeringDirection")) {
            this.steeringDirection = tag.getInt("SmartSteeringDirection");
            if (clientPacket) {
                this.clientSteeringDirection = this.steeringDirection;
            }
        }
        if (tag.contains("SmartBraking")) {
            this.braking = tag.getBoolean("SmartBraking");
        }
        if (tag.contains("LinkedSteeringWheel")) {
            this.linkedSteeringWheelPos = BlockPos.of(tag.getLong("LinkedSteeringWheel"));
        }
    }

    public void linkToSteeringWheel(BlockPos wheelPos, Player player) {
        this.linkedSteeringWheelPos = wheelPos;
        this.setChanged();
        this.sendData();
        if (player != null) player.displayClientMessage(Component.literal("Steering Wheel Set Up"), false);
    }

    public void unlinkFromSteeringwheel(BlockPos wheelPos, Player player) {
        Level level = this.getLevel();
        if (this.linkedSteeringWheelPos != null && level != null
                && level.getBlockEntity(this.linkedSteeringWheelPos) instanceof SteeringWheelBlockEntity steeringWheel) {
            steeringWheel.unlinkWheels(wheelPos, player);
        }
        this.linkedSteeringWheelPos = null;
        this.setBraking(false);
        this.setChanged();
        this.sendData();
        if (player != null) player.displayClientMessage(Component.literal("removed myself from steering wheel"), false);
    }
}
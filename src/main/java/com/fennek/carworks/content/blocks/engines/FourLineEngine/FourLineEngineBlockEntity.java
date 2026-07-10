package com.fennek.carworks.content.blocks.engines.FourLineEngine;

import com.fennek.carworks.CACWBlockEntityTypes;
import com.fennek.carworks.content.blocks.engines.CACWEngine;
import com.fennek.carworks.content.blocks.steeringwheel.SteeringWheelBlockEntity;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

import java.util.List;

public class FourLineEngineBlockEntity extends GeneratingKineticBlockEntity implements CACWEngine {

    private static final float FUEL_CONSUMED_PER_TICK = 0.11111111F;
    private static final int STARTUP_DURATION_TICKS = 60;
    private static final int CANRUN_DURATION_TICKS = 25;

    private static final TagKey<Fluid> GASOLINE_TAG = TagKey.create(Registries.FLUID, ResourceLocation.parse("c:gasoline"));

    SmartFluidTankBehaviour tank;

    private boolean firstTick = true;
    private boolean hasFuel = false;
    private float fuelDebt = 0.0F;
    private boolean turnedOn = false;
    private boolean CanRun = false;
    private boolean wasRunningServer = false;
    private int serverStartupTicks = 0;

    private BlockPos linkedWheelPos = null;
    private BlockPos linkedTankPos = null;

    @OnlyIn(Dist.CLIENT)
    protected FourLineEngineSound activeSound;
    @OnlyIn(Dist.CLIENT)
    private boolean wasRunning = false;
    @OnlyIn(Dist.CLIENT)
    private int startupTicks = 0;
    @OnlyIn(Dist.CLIENT)
    public float independentFanAngle = 0.0F;
    @OnlyIn(Dist.CLIENT)
    public float prevIndependentFanAngle = 0.0F;


    public FourLineEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("TurnedOn", this.turnedOn);
        tag.putBoolean("HasFuel", this.hasFuel);
        tag.putBoolean("CanRun", this.CanRun);

        if (this.linkedWheelPos != null) {
            tag.putLong("LinkedWheelPos", this.linkedWheelPos.asLong());
        }
        if (this.linkedTankPos != null) {
            tag.putLong("LinkedTankPos", this.linkedTankPos.asLong());
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        this.turnedOn = tag.getBoolean("TurnedOn");
        this.hasFuel = tag.getBoolean("HasFuel");
        this.CanRun = tag.getBoolean("CanRun");
        this.wasRunningServer = this.isEngineRunning() && this.CanRun;

        if (tag.contains("LinkedWheelPos")) {
            this.linkedWheelPos = BlockPos.of(tag.getLong("LinkedWheelPos"));
        } else {
            this.linkedWheelPos = null;
        }

        if (tag.contains("LinkedTankPos")) {
            this.linkedTankPos = BlockPos.of(tag.getLong("LinkedTankPos"));
        } else {
            this.linkedTankPos = null;
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        this.tank = SmartFluidTankBehaviour.single(this, 0);
        behaviours.add(this.tank);
        this.reActivateSource = true;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (this.getGeneratedSpeed() != 0.0F) {
            super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        }

        this.containedFluidTooltip(tooltip, isPlayerSneaking, getFuelSource());
        return true;
    }

    @Override
    public float calculateAddedStressCapacity() {
        return this.hasFuel && this.turnedOn && this.CanRun ? 102.4F : 0.0F;
    }

    @Override
    public float getGeneratedSpeed() {
        return this.hasFuel && this.turnedOn && this.CanRun
                ? convertToDirection(40.0F, this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING))
                : 0.0F;
    }

    private boolean isValidFuel(FluidStack stack) {
        return !stack.isEmpty() && stack.getFluid().builtInRegistryHolder().is(GASOLINE_TAG);
    }

    /**
     * Returns whichever fluid handler currently supplies this engine's fuel:
     * the linked external tank if one is set and still valid, otherwise the
     * engine's own internal tank. Self-heals if the linked tank's capability
     * has disappeared (block removed/changed).
     */
    private IFluidHandler getFuelSource() {
        if (this.linkedTankPos != null && this.level != null) {
            IFluidHandler external = this.level.getCapability(FluidHandler.BLOCK, this.linkedTankPos, null);
            if (external != null)
                return external;
            // Linked tank's capability vanished - clean up the stale link
            clearTankLink();
        }
        return this.tank.getCapability();
    }

    private void updateFuelState() {
        IFluidHandler source = getFuelSource();
        FluidStack current = source.getFluidInTank(0);
        boolean nowHasFuel = this.isValidFuel(current) && current.getAmount() > 0;
        if (nowHasFuel != this.hasFuel) {
            this.hasFuel = nowHasFuel;
            this.reActivateSource = true;
            this.sendData();
        }
    }

    public boolean isEngineRunning() {
        return this.hasFuel && this.turnedOn;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level != null) {
            if (this.level.isClientSide) {
                this.tickClient();
            } else {

                if (this.firstTick) {
                    this.firstTick = false;
                }

                this.updateFuelState();
                boolean isRunning = this.isEngineRunning();

                if (isRunning && !this.wasRunningServer) {
                    this.CanRun = false;
                    this.serverStartupTicks = CANRUN_DURATION_TICKS;
                    this.reActivateSource = true;
                    this.sendData();
                }

                if (isRunning && this.serverStartupTicks > 0) {
                    --this.serverStartupTicks;
                    if (this.serverStartupTicks <= 0) {
                        this.CanRun = true;
                        this.reActivateSource = true;
                        this.sendData();
                    }
                }

                if (!isRunning && this.wasRunningServer) {
                    this.serverStartupTicks = 0;
                    this.CanRun = false;
                    this.reActivateSource = true;
                    this.sendData();
                }

                this.wasRunningServer = isRunning;

                if (this.isEngineRunning() && this.CanRun) {
                    IFluidHandler source = getFuelSource();
                    for (this.fuelDebt += FUEL_CONSUMED_PER_TICK; this.fuelDebt >= 1.0F; --this.fuelDebt) {
                        source.drain(1, FluidAction.EXECUTE);
                    }
                    this.updateFuelState();
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected void tickClient() {

        this.prevIndependentFanAngle = this.independentFanAngle;

        boolean isRunning = this.isEngineRunning();

        if (this.hasFuel && this.turnedOn && this.CanRun) {
            this.independentFanAngle += 25.0F;
        }

        if (isRunning && !this.wasRunning) {
            this.startupTicks = STARTUP_DURATION_TICKS;
            if (this.activeSound != null) {
                this.activeSound.stopSound();
            }

            this.activeSound = new FourLineEngineSound(this, true);
            Minecraft.getInstance().getSoundManager().play(this.activeSound);
        }

        if (isRunning && this.startupTicks > 0) {
            --this.startupTicks;
            if (this.startupTicks <= 0) {
                if (this.activeSound != null) {
                    this.activeSound.stopSound();
                }

                this.activeSound = new FourLineEngineSound(this, false);
                Minecraft.getInstance().getSoundManager().play(this.activeSound);
            }
        }

        if (!isRunning && this.wasRunning) {
            if (this.activeSound != null) {
                this.activeSound.stopSound();
                this.activeSound = null;
            }

            this.startupTicks = 0;
        }

        this.wasRunning = isRunning;
    }

    public boolean hasLinkedWheel() {
        return this.linkedWheelPos != null;
    }

    public void linkWheel(BlockPos wheelPos) {
        this.linkedWheelPos = wheelPos;
        this.setChanged();
        this.sendData();
    }

    public void clearWheelLink() {
        this.linkedWheelPos = null;
        this.stopEngine();
        this.setChanged();
        this.sendData();
    }

    public void unlinkWheel() {
        if (this.linkedWheelPos != null && this.level != null) {
            if (this.level.getBlockEntity(this.linkedWheelPos) instanceof SteeringWheelBlockEntity wheel) {
                wheel.clearEngineLink();
            }
            this.linkedWheelPos = null;
            this.setChanged();
            this.sendData();
        }
    }

    public boolean hasLinkedTank() {
        return this.linkedTankPos != null;
    }

    public void linkTank(BlockPos tankPos) {
        this.linkedTankPos = tankPos;
        this.setChanged();
        this.sendData();
    }

    public void clearTankLink() {
        this.linkedTankPos = null;
        this.setChanged();
        this.sendData();
    }

    public void startEngine() {
        this.turnedOn = true;
        this.reActivateSource = true;
        this.sendData();
    }

    public void stopEngine() {
        this.turnedOn = false;
        this.reActivateSource = true;
        this.sendData();
    }

    public void SetTurnONOff(boolean on) {
        if (on)
            this.startEngine();
        else
            this.stopEngine();
    }

    public void toggleIgnition() {
        if (this.turnedOn)
            this.stopEngine();
        else
            this.startEngine();
    }

    public boolean isTurnedOn() {
        return this.turnedOn;
    }
}
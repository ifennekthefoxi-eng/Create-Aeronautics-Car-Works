package com.fennek.carworks.content.blocks.engines.FourLineEngine;

import com.fennek.carworks.CACWBlockEntityTypes;
import com.fennek.carworks.content.blocks.engines.CACWEngine;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
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
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.List;

public class FourLineEngineBlockEntity extends GeneratingKineticBlockEntity implements CACWEngine {

    private static final float FUEL_CONSUMED_PER_TICK = 0.11111111F;
    private static final int STARTUP_DURATION_TICKS = 60;
    private static final int CANRUN_DURATION_TICKS = 25;

    private static final TagKey<Fluid> GASOLINE_TAG = TagKey.create(Registries.FLUID, ResourceLocation.parse("c:gasoline"));

    SmartFluidTankBehaviour tank;

    private int analogSignal = 0;
    private boolean signalChanged = false;
    private boolean firstTick = true;
    private boolean hasFuel = false;
    private float fuelDebt = 0.0F;
    private boolean turnedOn = false;
    private boolean CanRun = false;
    private boolean wasRunningServer = false;
    private int serverStartupTicks = 0;

    @OnlyIn(Dist.CLIENT)
    protected FourLineEngineSound activeSound;
    @OnlyIn(Dist.CLIENT)
    private boolean wasRunning = false;
    @OnlyIn(Dist.CLIENT)
    private int startupTicks = 0;
    @OnlyIn(Dist.CLIENT)
    public float independentFanAngle = 0.0F;
    @OnlyIn(Dist.CLIENT)
    public float prevIndependentFanAngle = 0.0F; // ADD THIS

    public FourLineEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("AnalogSignal", this.analogSignal);
        tag.putBoolean("TurnedOn", this.turnedOn);
        tag.putBoolean("HasFuel", this.hasFuel);
        tag.putBoolean("CanRun", this.CanRun);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        this.analogSignal = tag.getInt("AnalogSignal");
        this.turnedOn = tag.getBoolean("TurnedOn");
        this.hasFuel = tag.getBoolean("HasFuel");
        this.CanRun = tag.getBoolean("CanRun");
        this.wasRunningServer = this.isEngineRunning() && this.CanRun;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(FluidHandler.BLOCK, CACWBlockEntityTypes.FOUR_LINE_ENGINE.get(), (be, side) -> {
            if (side == null) {
                return be.tank.getCapability();
            } else {
                Direction facing = be.getBlockState().getValue(FourLineEngineBlock.FACING);
                if (facing.getAxis().isVertical()) {
                    Direction.Axis portAxis = facing == Direction.DOWN ? Axis.X : Axis.Z;
                    if (side.getAxis() == portAxis) {
                        return be.tank.getCapability();
                    }
                } else if (side == Direction.DOWN) {
                    return be.tank.getCapability();
                }

                return null;
            }
        });
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        this.tank = SmartFluidTankBehaviour.single(this, 1000);
        behaviours.add(this.tank);
        this.reActivateSource = true;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (this.getGeneratedSpeed() != 0.0F) {
            super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        }

        this.containedFluidTooltip(tooltip, isPlayerSneaking, this.tank.getCapability());
        return true;
    }

    public FluidTank getTank() {
        return this.tank.getPrimaryHandler();
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

    private void updateFuelState() {
        FluidStack current = this.getTank().getFluid();
        boolean nowHasFuel = this.isValidFuel(current) && current.getAmount() > 0;
        if (nowHasFuel != this.hasFuel) {
            this.hasFuel = nowHasFuel;
            this.reActivateSource = true;
            this.sendData();
        }
    }

    public int getAnalogSignal() {
        return this.analogSignal;
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
                    this.setAnalogSignal(this.level.getBestNeighborSignal(this.worldPosition));
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
                    for (this.fuelDebt += FUEL_CONSUMED_PER_TICK; this.fuelDebt >= 1.0F; --this.fuelDebt) {
                        this.getTank().drain(1, FluidAction.EXECUTE);
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

        if (isRunning) {
            this.independentFanAngle += 25.0F; // Adjust your speed here
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

    public void setOriginalSignal(int newSignal) {
        // Implementation left intentionally blank as per original file
    }

    public void setAnalogSignal(int newSignal) {
        boolean wasOn = this.turnedOn;
        this.analogSignal = newSignal;
        this.turnedOn = newSignal >= 1;
        this.setChanged();

        if (wasOn != this.turnedOn) {
            this.reActivateSource = true;
            this.sendData();
        }
    }

    public void setSignalChanged(boolean newSignal) {
        this.signalChanged = newSignal;
    }
}
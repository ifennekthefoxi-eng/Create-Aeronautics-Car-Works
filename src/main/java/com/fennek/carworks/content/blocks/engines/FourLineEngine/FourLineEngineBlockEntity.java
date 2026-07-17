package com.fennek.carworks.content.blocks.engines.FourLineEngine;

import com.fennek.carworks.CACWBlockEntityTypes;
import com.fennek.carworks.CACWSoundEvents;
import com.fennek.carworks.content.blocks.engines.CACWEngine;
import com.fennek.carworks.content.blocks.engines.DynamicEngineSound;
import com.fennek.carworks.content.blocks.engines.EngineStartupSound;
import com.fennek.carworks.content.blocks.engines.structure.Gear;
import com.fennek.carworks.content.blocks.steeringwheel.SteeringWheelBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
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
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.List;

public class FourLineEngineBlockEntity extends GeneratingKineticBlockEntity implements CACWEngine {

    private static final float FUEL_CONSUMED_PER_TICK = 0.11111111F;
    private static final int STARTUP_DURATION_TICKS = 20;
    private static final int CANRUN_DURATION_TICKS = 16;

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
    protected EngineStartupSound<FourLineEngineBlockEntity> startupSound;
    @OnlyIn(Dist.CLIENT)
    protected DynamicEngineSound<FourLineEngineBlockEntity> idleSound; // New: Pure Idle purr
    @OnlyIn(Dist.CLIENT)
    protected DynamicEngineSound<FourLineEngineBlockEntity> lowRpmSound; // Plays FOURLINE_LOW
    @OnlyIn(Dist.CLIENT)
    protected DynamicEngineSound<FourLineEngineBlockEntity> midRpmSound;  // Plays FOURLINE_MID
    @OnlyIn(Dist.CLIENT)
    protected DynamicEngineSound<FourLineEngineBlockEntity> highRpmSound; // Plays FOURLINE_HIGH

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

    //engine simulation

    private float CurrentRPM = 0.0F;
    private float CurrentSpeed = 0.0F;
    private float CurrentTorque = 0.0F;
    private float RPMSnapTreshold = 10F;
    private float MinRPM = 800F;
    private float maxRPM = 7000F;
    private float maxSpeed = 100F;
    private float maxTorque = 3400F;
    private int CurrentGear = 1;

    // 0..1, driven by the steering wheel's throttle button/input. Not persisted -
    // it's a live held-input value, same treatment as the wheel's BrakeInput.
    private float throttle = 0.0F;

    // Gear ratios, lowest gear first. Unlike a real transmission, ratio == torque share:
    // top gear (ratio 1.0) is where the engine makes its FULL torque, because Create's
    // kinetic network has no "high RPM / low torque" concept to model against.

    //new gears record
    private static final Gear REVERSE_GEAR = new Gear("R",-0.4f,0.25f,2000,0,true,true);
    private static final Gear NEUTRAL_GEAR = new Gear("N",0f,0.55f,0,0,false,false);
    private static final Gear FIRST_GEAR = new Gear("1",0.5f,0.55f,3000,2800,true,false);
    private static final Gear SECOND_GEAR = new Gear("2",0.6f,0.35f,5000,3500,true,true);
    private static final Gear THIRD_GEAR = new Gear("3",0.7f,0.25f,6000,4500,true,true);
    private static final Gear FOURTH_GEAR = new Gear("4",0.8f,0.15f,6000,5000,true,true);
    private static final Gear FIFTH_GEAR = new Gear("5",0.9f,0.1f,6000,5000,true,true);
    private static final Gear SIXTH_GEAR = new Gear("6",1f,0.1f,6000,5000,true,true);

    private static final Gear[] GEARS = {REVERSE_GEAR,NEUTRAL_GEAR,FIRST_GEAR,SECOND_GEAR,THIRD_GEAR,FOURTH_GEAR,FIFTH_GEAR,SIXTH_GEAR};

    // REMOVE these arrays:
    // private static final String[] GearsDisplay = {"R","N","1","2","3","4","5"};
    // private static final float[] GEAR_RATIOS = {-0.4f,0f,0.5F, 0.65F, 0.8F, 0.87F, 1F};
    // private static final float[] GEAR_REVING_RATIOS = { 0.25f, 0.55f, 0.55F, 0.35F, 0.25F, 0.15F, 0.1F};

    // UPDATE GEAR_COUNT to use the length of the GEARS array:
    private static final int GEAR_COUNT = GEARS.length;

    // --- Gearbox / RPM simulation tuning ---
    private static final float RPM_ACCEL_PER_TICK = 140F;   // RPM gained per tick while running
    private static final float RPM_DECEL_PER_TICK = 220F;   // RPM lost per tick while not running
    // REMOVE UPSHIFT_RPM_RATIO and DOWNSHIFT_RPM_RATIO
    private static final int SHIFT_PENALTY_DURATION_TICKS = 5; // how long a bad shift hurts torque
    private static final float SHIFT_PENALTY_TORQUE_FACTOR = 0.85F; // torque multiplier while penalized

    private float lastGeneratedSpeed = 0F;
    private float lastSyncedRPM = 0F;

    private int shiftPenaltyTicks = 0;

    public FourLineEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("TurnedOn", this.turnedOn);
        tag.putBoolean("HasFuel", this.hasFuel);
        tag.putBoolean("CanRun", this.CanRun);
        tag.putInt("CurrentGear", this.CurrentGear);
        tag.putFloat("CurrentRPM", this.CurrentRPM);
        tag.putInt("ShiftPenaltyTicks", this.shiftPenaltyTicks);

        // --- ADD THESE THREE LINES ---
        tag.putFloat("CurrentTorque", this.CurrentTorque);
        tag.putFloat("CurrentSpeed", this.CurrentSpeed);
        tag.putFloat("Throttle", this.throttle);
        // -----------------------------

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
        this.CurrentGear = tag.contains("CurrentGear") ? Math.max(1, Math.min(GEAR_COUNT, tag.getInt("CurrentGear"))) : 1;
        this.CurrentRPM = tag.getFloat("CurrentRPM");
        this.shiftPenaltyTicks = tag.getInt("ShiftPenaltyTicks");

        // --- ADD THESE THREE LINES ---
        this.CurrentTorque = tag.getFloat("CurrentTorque");
        this.CurrentSpeed = tag.getFloat("CurrentSpeed");
        this.throttle = tag.getFloat("Throttle");
        // -----------------------------

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
        tooltip.add(Component.literal("    Engine Status: ")
                .withStyle(ChatFormatting.WHITE));

        tooltip.add(Component.literal("    Fuel Type: gasoline")
                .withStyle(ChatFormatting.GOLD));

        /*tooltip.add(Component.literal("    Current Gear: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(GearsDisplay[this.CurrentGear - 1])
                        .withStyle(ChatFormatting.AQUA)));*/

        tooltip.add(Component.literal("    Current Gear: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(GEARS[this.CurrentGear - 1].GEAR_DISPLAY())
                        .withStyle(ChatFormatting.AQUA)));

        tooltip.add(Component.literal("    Current Rpm: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(this.CurrentRPM) + " RPM")
                        .withStyle(ChatFormatting.AQUA)));

        tooltip.add(Component.literal("    Current Torque: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(this.CurrentTorque))
                        .withStyle(this.shiftPenaltyTicks > 0 ? ChatFormatting.RED : ChatFormatting.AQUA)));

        tooltip.add(Component.literal("    Current Speed: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(this.CurrentSpeed))
                        .withStyle(this.shiftPenaltyTicks > 0 ? ChatFormatting.RED : ChatFormatting.AQUA)));


        tooltip.add(Component.literal("    Throttle: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(Math.round(this.throttle * 100F) + "%")
                        .withStyle(ChatFormatting.AQUA)));

        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        // 3. Add the fluid container info at the bottom
        this.containedFluidTooltip(tooltip, isPlayerSneaking, getFuelSource());

        return true;
    }

    /**
     * Create reads this as the stress capacity supplied by the source, and combines it with
     * {@link #getGeneratedSpeed()} (capacity * speed) to get the actual SU/behavior on the
     * kinetic network - so this and getGeneratedSpeed must be derived from the SAME underlying
     * simulation tick (CurrentTorque/CurrentSpeed) or the two will drift out of sync with each other.
     * Already 0 whenever the engine isn't running (see {@link #updateEngineSimulation()}).
     */

    @Override
    public float calculateAddedStressCapacity() {
        // Decouple from the throttle! If it drops to 0 while spinning, Create instantly Overstresses.
        // Instead, give it a stable capacity based on the engine's physical gear state.
        if (!this.isEngineRunning() || !this.CanRun) return 0.0F;

        return this.getCurrentTorque()/this.CurrentSpeed;
    }

    @Override
    public float getGeneratedSpeed() {
        return this.hasFuel && this.turnedOn && this.CanRun ? convertToDirection(this.CurrentSpeed, this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)) : 0.0F;
    }

    /**
     * Sets the throttle position, 0 (off) to 1 (full). Called by the linked steering wheel
     * whenever the throttle button's held-state changes. Clamped defensively since this comes
     * straight off a network packet.
     */
    public void setThrottle(float value) {
        this.throttle = Mth.clamp(value, 0F, 1F);
    }

    public float getThrottle() {
        return this.throttle;
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

    @Override
    public boolean isEngineRunning() {
        return this.hasFuel && this.turnedOn;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level != null) {
            if (this.level.isClientSide) {
                this.tickClient();
            }
            if (!this.level.isClientSide) {

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

                this.updateEngineSimulation();

            }
        }
    }

    /**
     * Internal RPM/torque/speed simulation. This is deliberately decoupled from
     * Create's kinetic RPM/speed (see {@link #getGeneratedSpeed()}) - it exists purely
     * to drive the gearbox logic, sounds, and tooltip readouts.
     */

    /*private void updateEngineSimulation() {
        boolean running = this.isEngineRunning() && this.CanRun;

        // 1. Calculate Target RPM and Lerp (Smooth Acceleration/Deceleration)
        float targetRPM = 0.0F;
        if (running && this.throttle > 0F) {
            // Idle RPM is 800, max is maxRPM. Throttle scales between them.
            // You can adjust 800.0F to whatever your desired idle RPM is!
            targetRPM = this.MinRPM + ((this.maxRPM - this.MinRPM) * this.throttle);
        }

        // The 0.1F is the lerp factor (speed of acceleration/deceleration)
        // Change to a higher value for faster revving, or lower for slower revving.
        //
        // NOTE: Mth.lerp() moves a fixed PERCENTAGE of the remaining distance each tick.
        // That's exponential decay toward the target - it mathematically never actually
        // arrives, it just gets asymptotically closer forever (eventually stalling a few
        // units short once the per-tick correction is too small to matter). That's why RPM
        // used to hover around ~809 instead of settling at idle 800, and ~6995 instead of
        // topping out at maxRPM. Snapping once we're within a small epsilon fixes this.
        float lerpTargetRPM = throttle > 0.1F ? targetRPM : this.MinRPM;
        this.CurrentRPM = net.minecraft.util.Mth.lerp(0.1F, this.CurrentRPM, lerpTargetRPM);
        if (Math.abs(this.CurrentRPM - lerpTargetRPM) < 2.0F) {
            this.CurrentRPM = lerpTargetRPM;
        }

        if(this.CurrentRPM > (this.maxRPM - this.RPMSnapTreshold)) {
            this.CurrentRPM = this.maxRPM;
        }

        if(this.CurrentRPM < (this.MinRPM + this.RPMSnapTreshold)) {
            this.CurrentRPM = this.MinRPM;
        }

        // 2. Handle Shifting Penalty
        if (this.shiftPenaltyTicks > 0) {
            --this.shiftPenaltyTicks;
        }

        // 3. Calculate Gear Ratio and Torque
        float gearRatio = GEAR_RATIOS[this.CurrentGear - 1];
        float penaltyFactor = this.shiftPenaltyTicks > 0 ? SHIFT_PENALTY_TORQUE_FACTOR : 1.0F;

        this.CurrentTorque = running ? (this.CurrentRPM / this.maxRPM) * this.maxTorque * gearRatio * penaltyFactor : 0F;
        if(this.CurrentRPM <= 810f) {
            this.CurrentTorque = 0;
        }

        // 4. Calculate Final Speed for Create
        this.CurrentSpeed = running ? (this.CurrentRPM / this.maxRPM) * this.maxSpeed * gearRatio : 0F;
        if(this.CurrentRPM <= 810f) {
            this.CurrentSpeed = 0;
        }

        // 5. Update Create's Kinetic Network (with anti-lag threshold)
        float currentGenSpeed = this.getGeneratedSpeed();
        if (Math.abs(currentGenSpeed - this.lastGeneratedSpeed) >= 0.5F || (currentGenSpeed == 0 && this.lastGeneratedSpeed != 0)) {
            this.lastGeneratedSpeed = currentGenSpeed;
            this.updateGeneratedRotation();
        }

        // 6. Sync Data to Client (Goggles Tooltip)
        if (Math.abs(this.CurrentRPM - this.lastSyncedRPM) > 10F || (!running && this.lastSyncedRPM != 0)) {
            this.lastSyncedRPM = this.CurrentRPM;
            this.setChanged();
            this.sendData();
        }
    }*/

    private void updateEngineSimulation() {
        boolean running = this.isEngineRunning() && this.CanRun;

        // 1. Calculate Target RPM based on throttle
        float targetRPM = 0.0F;
        if (running) {
            if (this.throttle > 0.05F) {
                targetRPM = this.MinRPM + ((this.maxRPM - this.MinRPM) * this.throttle);
            } else {
                targetRPM = this.MinRPM; // Settle at Idle (800 RPM)
            }
        }

        /*// 2. Linear RPM rate-limiting with mechanical drag/inertia simulation
        float diff = targetRPM - this.CurrentRPM;
        if (diff > 0.0F) {
            // Revving Up: Load factor limits acceleration based on gear ratio.
            // 1st gear (0.1 ratio) revs at ~92% of RPM_ACCEL_PER_TICK.
            // 6th gear (1.0 ratio) revs at ~20% of RPM_ACCEL_PER_TICK (heavy load!).
            float gearRatio = GEAR_RATIOS[this.CurrentGear - 1];
            float loadMultiplier = Mth.lerp(gearRatio, 1.0F, 0.20F);
            float maxAccel = RPM_ACCEL_PER_TICK * loadMultiplier;

            this.CurrentRPM += Math.min(diff, maxAccel);*/
        /*// 2. Custom RPM rate-limiting with mechanical drag/inertia simulation
        float diff = targetRPM - this.CurrentRPM;
        if (diff > 0.0F) {
            // Revving Up: Uses Assetto Corsa tuned acceleration values.
            // Neutral revs freely (1.0x), 1st gear pulls hard (0.9x),
            // 5th gear struggles against load (0.5x).
            float revRatio = GEAR_REVING_RATIOS[this.CurrentGear - 1];
            float maxAccel = RPM_ACCEL_PER_TICK * revRatio;

            this.CurrentRPM += Math.min(diff, maxAccel);
        } else if (diff < 0.0F) {
            // Revving Down (deceleration)
            this.CurrentRPM += Math.max(diff, -RPM_DECEL_PER_TICK);
        }*/

        // 2. Custom RPM rate-limiting with mechanical drag/inertia simulation
        float diff = targetRPM - this.CurrentRPM;
        if (diff > 0.0F) {
            // Revving Up
            float revRatio = GEARS[this.CurrentGear - 1].GEAR_REV_RATIO();
            float maxAccel = RPM_ACCEL_PER_TICK * revRatio;

            this.CurrentRPM += Math.min(diff, maxAccel);
        } else if (diff < 0.0F) {
            // Revving Down (deceleration)
            this.CurrentRPM += Math.max(diff, -RPM_DECEL_PER_TICK);
        }

        // Safety clamps
        if (running) {
            this.CurrentRPM = Mth.clamp(this.CurrentRPM, this.MinRPM, this.maxRPM);
        } else {
            this.CurrentRPM = Math.max(0.0F, this.CurrentRPM);
        }

        // 3. Handle Shifting Penalty
        if (this.shiftPenaltyTicks > 0) {
            --this.shiftPenaltyTicks;
        }

        /*// 4. Calculate Gear Ratio and Torque
        float gearRatio = GEAR_RATIOS[this.CurrentGear - 1];
        float penaltyFactor = this.shiftPenaltyTicks > 0 ? SHIFT_PENALTY_TORQUE_FACTOR : 1.0F;

        this.CurrentTorque = running ? (this.CurrentRPM / this.maxRPM) * this.maxTorque * gearRatio * penaltyFactor : 0F;
        if(this.CurrentRPM <= (this.MinRPM + 10F)) {
            this.CurrentTorque = 0; // Cut torque right at idle to prevent weird creeping behaviors
        }

        // 5. Calculate Final Speed for Create
        this.CurrentSpeed = running ? (this.CurrentRPM / this.maxRPM) * this.maxSpeed * gearRatio : 0F;
        if(this.CurrentRPM <= (this.MinRPM + 10F)) {
            this.CurrentSpeed = 0;
        }*/

        // 4. Calculate Gear Ratio and Torque
        float gearRatio = GEARS[this.CurrentGear - 1].GEAR_RATIO();
        float penaltyFactor = this.shiftPenaltyTicks > 0 ? SHIFT_PENALTY_TORQUE_FACTOR : 1.0F;

        this.CurrentTorque = running ? (this.CurrentRPM / this.maxRPM) * this.maxTorque * gearRatio * penaltyFactor : 0F;
        if(this.CurrentRPM <= (this.MinRPM + 10F)) {
            this.CurrentTorque = 0; // Cut torque right at idle to prevent weird creeping behaviors
        }

        // 5. Calculate Final Speed for Create
        this.CurrentSpeed = running ? (this.CurrentRPM / this.maxRPM) * this.maxSpeed * gearRatio : 0F;
        if(this.CurrentRPM <= (this.MinRPM + 10F)) {
            this.CurrentSpeed = 0;
        }

        // 6. Update Create's Kinetic Network (with anti-lag threshold)
        float currentGenSpeed = this.getGeneratedSpeed();
        if (Math.abs(currentGenSpeed - this.lastGeneratedSpeed) >= 0.5F || (currentGenSpeed == 0 && this.lastGeneratedSpeed != 0)) {
            this.lastGeneratedSpeed = currentGenSpeed;
            this.updateGeneratedRotation();
        }

        // 7. Sync Data to Client (Goggles Tooltip)
        if (Math.abs(this.CurrentRPM - this.lastSyncedRPM) > 10F || (!running && this.lastSyncedRPM != 0)) {
            this.lastSyncedRPM = this.CurrentRPM;
            this.setChanged();
            this.sendData();
        }
    }

    /**
     * Current RPM as a fraction of {@link #maxRPM}, in [0, 1]. Used to judge shift timing.
     */
    public float getRpmRatio() {
        return this.maxRPM <= 0F ? 0F : this.CurrentRPM / this.maxRPM;
    }

    public int getCurrentGear() {
        return this.CurrentGear;
    }

    @Override
    public float getCurrentRPM() {
        return this.CurrentRPM;
    }

    public float getCurrentTorque() {
        return this.CurrentTorque;
    }

    @Override
    public float getCurrentSpeed() {
        return this.CurrentSpeed;
    }

    @Override
    public float getMaxSpeed() {
        return this.maxSpeed;
    }

    public boolean isShiftPenaltyActive() {
        return this.shiftPenaltyTicks > 0;
    }

    /*//**
     * Shifts up one gear if not already in top gear. Shifting above
     * {@link #UPSHIFT_RPM_RATIO} of maxRPM is a clean shift with no penalty; shifting
     * earlier than that (before the engine has built up revs) applies a temporary
     * torque penalty, since the taller gear isn't "earned" yet.
     *
     * @return true if the gear actually changed
     //-/
    public boolean shiftUp() {
        if (this.CurrentGear >= GEAR_COUNT) {
            return false;
        }

        boolean cleanShift = getRpmRatio() >= UPSHIFT_RPM_RATIO;
        performShift(this.CurrentGear + 1, cleanShift);
        return true;
    }

    /**
     * Shifts down one gear if not already in first gear. Shifting below
     * {@link #DOWNSHIFT_RPM_RATIO} of maxRPM is a clean shift; shifting down while RPM
     * is still high (an aggressive/early downshift) applies the same temporary torque
     * penalty as a premature upshift.
     *
     * @return true if the gear actually changed
     -/
    public boolean shiftDown() {
        if (this.CurrentGear <= 1) {
            return false;
        }

        boolean cleanShift = getRpmRatio() <= DOWNSHIFT_RPM_RATIO;
        performShift(this.CurrentGear - 1, true);
        return true;
    }

    private void performShift(int newGear, boolean cleanShift) {
        float newRatio = GEAR_RATIOS[newGear - 1];
        this.CurrentGear = newGear;

        // Keep road speed continuous across the shift (RPM "jumps" to match the new
        // ratio at the same speed) rather than snapping speed itself around.
        if (newRatio > 0F && this.maxSpeed > 0F) {
            this.CurrentRPM = Math.min(this.maxRPM, Math.max(0F,
                    (this.CurrentSpeed / (this.maxSpeed * newRatio)) * this.maxRPM));
        }

        this.shiftPenaltyTicks = cleanShift ? 0 : SHIFT_PENALTY_DURATION_TICKS;

        this.reActivateSource = true;
        this.setChanged();
        this.sendData();
    }*/

    /**
     * Shifts up one gear if not already in top gear.
     * Uses the current gear's penalty boolean and minimum RPM boundary.
     */
    public boolean shiftUp() {
        if (this.CurrentGear >= GEAR_COUNT) {
            return false;
        }

        Gear currentGearData = GEARS[this.CurrentGear - 1];

        // A clean shift happens if the gear doesn't penalize upshifts OR if the RPM is high enough
        boolean cleanShift = !currentGearData.SHOULD_PENALIZE_ON_SHIFT_UP() ||
                this.CurrentRPM >= currentGearData.MIN_RPM_NEEDED_FOR_SHIFT_UP_WITHOUT_PENALTY();

        performShift(this.CurrentGear + 1, cleanShift);
        return true;
    }

    /**
     * Shifts down one gear if not already in first gear (Reverse).
     * Uses the current gear's penalty boolean and maximum RPM boundary.
     */
    public boolean shiftDown() {
        if (this.CurrentGear <= 1) {
            return false;
        }

        Gear currentGearData = GEARS[this.CurrentGear - 1];

        // A clean shift happens if the gear doesn't penalize downshifts OR if the RPM is low enough
        boolean cleanShift = !currentGearData.SHOULD_PENALIZE_ON_SHIFT_DOWN() ||
                this.CurrentRPM <= currentGearData.MAX_RPM_NEEDED_FOR_SHIFT_DOWN_WITHOUT_PENALTY();

        performShift(this.CurrentGear - 1, cleanShift);
        return true;
    }

    private void performShift(int newGear, boolean cleanShift) {
        float newRatio = GEARS[newGear - 1].GEAR_RATIO();
        this.CurrentGear = newGear;

        // Keep road speed continuous across the shift (RPM "jumps" to match the new
        // ratio at the same speed) rather than snapping speed itself around.
        if (newRatio > 0F && this.maxSpeed > 0F) {
            this.CurrentRPM = Math.min(this.maxRPM, Math.max(0F,
                    (this.CurrentSpeed / (this.maxSpeed * newRatio)) * this.maxRPM));
        }

        this.shiftPenaltyTicks = cleanShift ? 0 : SHIFT_PENALTY_DURATION_TICKS;

        this.reActivateSource = true;
        this.setChanged();
        this.sendData();
    }

    /*@OnlyIn(Dist.CLIENT)
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

            this.activeSound = new FourLineEngineSound(this, true,0.2f);
            Minecraft.getInstance().getSoundManager().play(this.activeSound);
        }

        if (isRunning && this.startupTicks > 0) {
            --this.startupTicks;
            if (this.startupTicks <= 0) {
                if (this.activeSound != null) {
                    this.activeSound.stopSound();
                }

                this.activeSound = new FourLineEngineSound(this, false,0.2f);
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
    }*/

    @OnlyIn(Dist.CLIENT)
    protected void tickClient() {
        this.prevIndependentFanAngle = this.independentFanAngle;
        boolean isRunning = this.isEngineRunning();

        if (this.hasFuel && this.turnedOn && this.CanRun) {
            this.independentFanAngle += 25.0F;
        }

        // --- STARTUP LOGIC ---
        if (isRunning && !this.wasRunning) {
            this.startupTicks = STARTUP_DURATION_TICKS;
            stopAllSounds();

            this.startupSound = new EngineStartupSound<>(this, CACWSoundEvents.FOURLINE_START.get(), 0.2f);
            Minecraft.getInstance().getSoundManager().play(this.startupSound);
        }

        // --- TRANSITION TO LOOPING LOGIC ---
        if (isRunning && this.startupTicks > 0) {
            --this.startupTicks;
            if (this.startupTicks <= 0) {
                startLoopingSounds();
            }
        }

        // --- SHUTDOWN LOGIC ---
        if (!isRunning && this.wasRunning) {
            stopAllSounds();
            this.startupTicks = 0;
        }

        this.wasRunning = isRunning;
    }

    /*@OnlyIn(Dist.CLIENT)
    private void startLoopingSounds() {
        // Idle gets 0.0F speed mod (speed doesn't affect idle sound)
        this.lowRpmSound = new DynamicEngineSound<>(this, CACWSoundEvents.FOURLINE_IDLE.get(), 0.2f,
                0F, 800F, 3000F, 0.8F, 1.2F, 0.0F);
        Minecraft.getInstance().getSoundManager().play(this.lowRpmSound);

        if (CACWSoundEvents.FOURLINE_MID != null && CACWSoundEvents.FOURLINE_MID.get() != null) {
            // Mid gets a small 0.1F pitch boost at max speed
            this.midRpmSound = new DynamicEngineSound<>(this, CACWSoundEvents.FOURLINE_MID.get(), 0.2f,
                    1500F, 4000F, 6000F, 0.9F, 1.3F, 0.1F);
            Minecraft.getInstance().getSoundManager().play(this.midRpmSound);
        }

        // High gets a noticeable 0.25F pitch boost at max speed
        this.highRpmSound = new DynamicEngineSound<>(this, CACWSoundEvents.FOURLINE_HIGH.get(), 0.2f,
                4500F, 7000F, 10000F, 0.9F, 1.4F, 0.25F);
        Minecraft.getInstance().getSoundManager().play(this.highRpmSound);
    }

    @OnlyIn(Dist.CLIENT)
    private void stopAllSounds() {
        if (this.startupSound != null) { this.startupSound.stopSound(); this.startupSound = null; }
        if (this.lowRpmSound != null) { this.lowRpmSound.stopSound(); this.lowRpmSound = null; }
        if (this.midRpmSound != null) { this.midRpmSound.stopSound(); this.midRpmSound = null; }
        if (this.highRpmSound != null) { this.highRpmSound.stopSound(); this.highRpmSound = null; }
    }
    */

    @OnlyIn(Dist.CLIENT)
    private void startLoopingSounds() {
        // 1. PURE IDLE: Loudest at 800 RPM, completely faded out by 2000 RPM. (0.0F speed pitch mod)
        this.idleSound = new DynamicEngineSound<>(this, CACWSoundEvents.FOURLINE_IDLE.get(), 0.1f,
                0F, 800F, 2000F, 0.8F, 1.1F, 0.0F);
        Minecraft.getInstance().getSoundManager().play(this.idleSound);

        // 2. LOW RPM: Fades in at 1000, peaks at 2200, fades out by 4000. (Slight 0.05F pitch speed mod)
        if (CACWSoundEvents.FOURLINE_LOW != null && CACWSoundEvents.FOURLINE_LOW.get() != null) {
            this.lowRpmSound = new DynamicEngineSound<>(this, CACWSoundEvents.FOURLINE_LOW.get(), 0.1f,
                    900F, 2200F, 4000F, 0.85F, 1.2F, 0.05F);
            Minecraft.getInstance().getSoundManager().play(this.lowRpmSound);
        }

        // 3. MID RPM: Fades in at 3000, peaks at 4500, fades out by 6500. (Moderate 0.15F pitch speed mod)
        if (CACWSoundEvents.FOURLINE_MID != null && CACWSoundEvents.FOURLINE_MID.get() != null) {
            this.midRpmSound = new DynamicEngineSound<>(this, CACWSoundEvents.FOURLINE_MID.get(), 0.1f,
                    2000F, 4000F, 6500F, 0.9F, 1.3F, 0.15F);
            Minecraft.getInstance().getSoundManager().play(this.midRpmSound);
        }

        // 4. HIGH RPM: Fades in at 5000, peaks at 7000 (Redline). (Intense 0.30F pitch speed mod at top speed)
        if (CACWSoundEvents.FOURLINE_HIGH != null && CACWSoundEvents.FOURLINE_HIGH.get() != null) {
            this.highRpmSound = new DynamicEngineSound<>(this, CACWSoundEvents.FOURLINE_HIGH.get(), 0.1f,
                    4500F, 7000F, 10000F, 0.9F, 1.4F, 0.30F);
            Minecraft.getInstance().getSoundManager().play(this.highRpmSound);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void stopAllSounds() {
        if (this.startupSound != null) { this.startupSound.stopSound(); this.startupSound = null; }
        if (this.idleSound != null) { this.idleSound.stopSound(); this.idleSound = null; }
        if (this.lowRpmSound != null) { this.lowRpmSound.stopSound(); this.lowRpmSound = null; }
        if (this.midRpmSound != null) { this.midRpmSound.stopSound(); this.midRpmSound = null; }
        if (this.highRpmSound != null) { this.highRpmSound.stopSound(); this.highRpmSound = null; }
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

        SubLevelAccess tankSublevel = SableCompanion.INSTANCE.getContaining(level, tankPos);
        SubLevelAccess thisSublevel = SableCompanion.INSTANCE.getContaining(level, this.worldPosition);

        if(tankSublevel == null || thisSublevel == null)
        {
            return;
        }

        if(tankSublevel != thisSublevel){
            return;
        }

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
        this.throttle = 0.0F;
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


    @Override
    public IFluidHandler getTank() {
        return getFuelSource();
    }
}
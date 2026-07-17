package com.fennek.carworks.content.blocks.steeringwheel;

import com.fennek.carworks.content.blocks.SmartWheelMount.SmartWheelMountBlockEntity;
import com.fennek.carworks.content.blocks.engines.FourLineEngine.FourLineEngineBlockEntity;
import com.fennek.carworks.content.blocks.steeringwheel.handlers.SteeringWheeClientHandler;
import com.fennek.carworks.content.blocks.steeringwheel.menu.SteeringWheelMenu;
import com.fennek.carworks.utility.CACWMathHelpers;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.data.SimLang;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.createmod.catnip.data.Couple;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.util.Mth;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.lang.ref.WeakReference;
import java.util.*;

public class SteeringWheelBlockEntity extends SmartBlockEntity implements MenuProvider,IHaveGoggleInformation {

    private List<BlockPos> brakeWheels = new ArrayList<>();
    private List<BlockPos> steerWheels = new ArrayList<>();

    //jukebox
    public int jukeBoxDiscRotation = 0;
    private int jukeBoxDiscRotationSpeed = 4;
    private boolean discPlaying = false; // synced to client
    public SteeringWheelInventory inventory;
    private final JukeboxSongPlayer jukeboxSongPlayer = new JukeboxSongPlayer(this::onJukeboxSongChanged, this.worldPosition);
    private int discPlaybackTicks = 0; // our own timer, since we don't call jukeboxSongPlayer.tick() (avoids note particles)

    //private FourLineEngineBlockEntity FLEBE;
    // Remove: private FourLineEngineBlockEntity FLEBE;
    private BlockPos linkedEnginePos = null; // Add this instead

    public float SteeringDirection = 0.0f;
    private boolean leftHeld = false;
    private boolean rightHeld = false;

    public int BrakeInput = 0;

    // live held-state for the throttle button, forwarded straight to the linked engine
    private boolean throttleHeld = false;

    // client-only smoothing state - never synced directly, derived from SteeringDirection
    private float clientSteeringAngle = 0f;
    private float prevclientSteeringAngle = 0f;
    private static final float STEER_LERP_SPEED = 30f; // degrees per tick - tune to taste

    boolean ignition = false;
    private float clientKeyAngle = 0f;
    private float prevclientKeyAngle = 0f;
    private static final float KEY_LERP_SPEED = 30f; // degrees per tick - tune to taste

    private static final float MAX_SPEED_NEEDLE_MS_READ = 50;

    // client-only smoothing state for the gas gauge
    private float clientGasLevel = 0f;
    private float prevClientGasLevel = 0f;
    private static final float GAS_LERP_SPEED = 0.06f; // Amount it moves per tick. 0.02 = takes 2.5 seconds to go from Full to Empty.

    //speed meter
    private float adjustedVelocity;
    private Vector3dc currentNormal;
    private WeakReference<SubLevel> subLevelReference;


    // write saveData to this instead of 'encodedRequest'
    public final ItemStackHandler frequencySlots = new ItemStackHandler(6);//right now just 3 keybinds, 2 slots each

    private UUID user;
    private UUID prevUser;    // used only on client
    private boolean deactivatedThisTick;    // used only on server

    public class SteeringWheelInventory extends ItemStackHandler {
        public SteeringWheelInventory() {
            super(1);
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            setChanged();

            // Not auto-play: this only stops playback if the disc is pulled out from under a playing song.
            if (level != null && !level.isClientSide && getStackInSlot(0).isEmpty()) {
                stopDisc();
            }
        }
    }

    public SteeringWheelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        inventory = new SteeringWheelInventory();
        this.subLevelReference = new WeakReference<>(null);
        this.adjustedVelocity = 0;
        this.currentNormal = new Vector3d();
    }

    @Override
    public void initialize() {
        super.initialize();

        this.subLevelReference = new WeakReference<>(Sable.HELPER.getContaining(this.getLevel(), this.worldPosition));
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return SteeringWheelMenu.create(id, inv, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.createaeronauticscarworks.steering_wheel");
    }

    @Override
    public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
        if (this.subLevelReference.get() != null) {
            SimLang.number(Math.abs(this.getAdjustedVelocity()))
                    .text(" Blocks/s").forGoggles(tooltip);
        }

        return IHaveGoggleInformation.super.addToGoggleTooltip(tooltip, isPlayerSneaking);
    }

    // Add to write() method
    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.put("Inventory", inventory.serializeNBT(registries));
        compound.put("Frequencies", frequencySlots.serializeNBT(registries));
        if (user != null)
            compound.putUUID("User", user);

        if (this.linkedEnginePos != null) {
            compound.putLong("LinkedEnginePos", this.linkedEnginePos.asLong());
        }
        compound.putFloat("SteeringDirection", SteeringDirection);
        compound.putBoolean("ignition", ignition);
        compound.putFloat("AdjustedVelocity", this.getAdjustedVelocity());
        compound.putBoolean("discPlaying", discPlaying);

        // Save Brake and Steer Wheels as Long Arrays
        long[] brakeArr = this.brakeWheels.stream().mapToLong(BlockPos::asLong).toArray();
        compound.putLongArray("BrakeWheels", brakeArr);

        long[] steerArr = this.steerWheels.stream().mapToLong(BlockPos::asLong).toArray();
        compound.putLongArray("SteerWheels", steerArr);
    }

    @Override
    public void writeSafe(CompoundTag compound, HolderLookup.Provider registries) {
        super.writeSafe(compound, registries);
        //compound.put("ControllerData", CatnipCodecUtils.encode(ItemContainerContents.CODEC, registries, controllerData).orElseThrow());
        compound.put("Frequencies", frequencySlots.serializeNBT(registries));
    }


    // Add to read() method
    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        inventory.deserializeNBT(registries, compound.getCompound("Inventory"));
        if (compound.contains("Frequencies"))
            frequencySlots.deserializeNBT(registries, compound.getCompound("Frequencies"));

        user = compound.hasUUID("User") ? compound.getUUID("User") : null;

        if (compound.contains("LinkedEnginePos")) {
            this.linkedEnginePos = BlockPos.of(compound.getLong("LinkedEnginePos"));
        } else {
            this.linkedEnginePos = null;
        }

        if (compound.contains("SteeringDirection")) {
            this.SteeringDirection = compound.getFloat("SteeringDirection");
            if (!clientPacket) {
                // initial load (e.g. chunk load) - snap instantly, don't animate from 0
                float target = -45f * SteeringDirection;
                this.clientSteeringAngle = target;
                this.prevclientSteeringAngle = target;
            }
        }
        if (compound.contains("ignition")) {
            this.ignition = compound.getBoolean("ignition");
            if (!clientPacket) {
                float target = this.ignition ? 90 : 0;
                this.clientKeyAngle = target;
                this.prevclientKeyAngle = target;
                this.turnengineOnOff(this.ignition);
            }

        }
        if (compound.contains("AdjustedVelocity")) {
            this.adjustedVelocity = compound.getFloat("AdjustedVelocity");
        }
        if (compound.contains("discPlaying")) {
            this.discPlaying = compound.getBoolean("discPlaying");
        }

        // Load Brake and Steer Wheels
        if (compound.contains("BrakeWheels")) {
            this.brakeWheels.clear();
            for (long posLong : compound.getLongArray("BrakeWheels")) {
                this.brakeWheels.add(BlockPos.of(posLong));
            }
        }
        if (compound.contains("SteerWheels")) {
            this.steerWheels.clear();
            for (long posLong : compound.getLongArray("SteerWheels")) {
                this.steerWheels.add(BlockPos.of(posLong));
            }
        }
    }

    public static boolean playerInRange(Player player, Level world, BlockPos pos) {
        double reach = 0.5 * player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);

        // CORRECT: Calculates distance in global space, natively accounting for Sable sub-levels
        double distanceSq = SableCompanion.INSTANCE.distanceSquaredWithSubLevels(
                world,
                player.getEyePosition(),
                Vec3.atCenterOf(pos)
        );

        return distanceSq < reach * reach;
    }

    public void tryStartUsing(Player player,Level world) {
        if (!deactivatedThisTick && !hasUser() && !playerIsUsingSteeringWheel(player) && playerInRange(player, level, worldPosition))
        {
            startUsing(player, world);
        }
        else {
            if(playerIsUsingSteeringWheel(player)){
                stopUsing(player);
            }
            else {
                player.displayClientMessage(Component.literal("its in use cant steal"), false);
            }
        }
    }

    public void tryStopUsing(Player player) {
        if (isUsedBy(player))
            stopUsing(player);
    }

    private void startUsing(Player player,Level world) {
        user = player.getUUID();
        player.getPersistentData().putBoolean("IsUsingSteeringWheel", true);
        sendData();
        player.displayClientMessage(Component.literal("using"), false);
        if (world.isClientSide)
            CatnipServices.PLATFORM.executeOnClientOnly(() -> this::tryToggleActive);
    }

    private void stopUsing(Player player) {
        user = null;
        if (player != null) {
            // Move the message inside the null check so it doesn't crash when a player disconnects
            player.getPersistentData().remove("IsUsingSteeringWheel");
            player.displayClientMessage(Component.literal("use stoped"), false);
        }
        deactivatedThisTick = true;
        SteeringDirection = 0;
        setThrottleInput(false);
        setChanged();
        sendData();
    }

    public void stopUsingOnDestroy(){
        Entity entity = ((ServerLevel) level).getEntity(user);
        if (entity instanceof Player player)
        {
            stopUsing(player);
        }
    }

    private void CloseSetUpMenu(Player player) {
        user = null;
        if (player != null)
            player.getPersistentData().remove("IsConfiguringSteeringWheel");
        deactivatedThisTick = true;
        sendData();
        player.displayClientMessage(Component.literal("configuring finished"), false);
    }

    public boolean hasUser() {
        return user != null;
    }
    public boolean isUsedBy(Player player) {
        return hasUser() && user.equals(player.getUUID());
    }

    public static boolean playerIsUsingSteeringWheel(Player player) {
        return player.getPersistentData().contains("IsUsingSteeringWheel");
    }

    @Override
    public void tick() {
        super.tick();

        if (level.isClientSide) {
            CatnipServices.PLATFORM.executeOnClientOnly(() -> this::tryToggleActive);
            prevUser = user;

            // smoothly ease the wheel toward its target angle
            prevclientSteeringAngle = clientSteeringAngle;
            float steerTarget = -45f * SteeringDirection;
            float steerDiff = steerTarget - clientSteeringAngle;
            if (Math.abs(steerDiff) <= STEER_LERP_SPEED)
                clientSteeringAngle = steerTarget;
            else
                clientSteeringAngle += Math.signum(steerDiff) * STEER_LERP_SPEED;

            // smoothly ease the key toward its target angle
            prevclientKeyAngle = clientKeyAngle;
            float KeyTarget = this.ignition ? 90 : 0;
            float KeyDiff = KeyTarget - clientKeyAngle;
            if (Math.abs(KeyDiff) <= KEY_LERP_SPEED)
                clientKeyAngle = KeyTarget;
            else
                clientKeyAngle += Math.signum(KeyDiff) * KEY_LERP_SPEED;

            // --- ADD THIS: smoothly ease the gas needle ---
            prevClientGasLevel = clientGasLevel;
            float gasTarget = this.GetGasTankLevel();
            float gasDiff = gasTarget - clientGasLevel;
            if (Math.abs(gasDiff) <= GAS_LERP_SPEED)
                clientGasLevel = gasTarget;
            else
                clientGasLevel += Math.signum(gasDiff) * GAS_LERP_SPEED;

            if(isDiscPlaying()){
                jukeBoxDiscRotation += jukeBoxDiscRotationSpeed;
            }
            else
            {
                jukeBoxDiscRotation = 0;
            }
        }

        if (!level.isClientSide) {

            // jukebox: manually track playback length instead of calling jukeboxSongPlayer.tick(),
            // since vanilla's tick() is what periodically spawns the note particles - there's no
            // way to opt out of just that part while still calling it.

            if (this.discPlaying != jukeboxSongPlayer.isPlaying()) {
                setDiscPlaying(jukeboxSongPlayer.isPlaying());
            }

            if (jukeboxSongPlayer.isPlaying()) {
                discPlaybackTicks++;
                JukeboxSong song = jukeboxSongPlayer.getSong();
                if (song != null) {
                    long lengthTicks = Math.round(song.lengthInSeconds() * 20f);
                    if (discPlaybackTicks >= lengthTicks)
                        stopDisc();
                }
            } else {
                discPlaybackTicks = 0;
            }

            Direction facing = this.getBlockState().getValue(SteeringWheelBlock.FACING);
            this.currentNormal = JOMLConversion.toJOML(Vec3.atLowerCornerOf(facing.getNormal()));
            final SubLevel subLevel = this.subLevelReference.get();

            if (subLevel != null) {
                final float dot = (float) this.getGlobalVelocity().dot(subLevel.logicalPose().transformNormal(this.currentNormal, new Vector3d()));
                if (Math.abs(dot) > 0.05) {
                    this.adjustedVelocity = dot;
                } else {
                    this.adjustedVelocity = 0;
                }

            } else { // sublevel is null, aka on stationary ground
                this.adjustedVelocity = 0;
            }
            this.sendData();

            //after this comment only ticks if the user is using the wheel
            deactivatedThisTick = false;

            if (!(level instanceof ServerLevel))
                return;
            if (user == null)
                return;

            Entity entity = ((ServerLevel) level).getEntity(user);
            if (!(entity instanceof Player player)) {
                stopUsing(null);
                return;
            }

            if (!playerInRange(player, level, worldPosition) || !playerIsUsingSteeringWheel(player))
                stopUsing(player);

            pushSteeringAndBrakeOverrides();
        }
    }

    public void linkSteerWheels(BlockPos wheelPos,Player player) {
        if(!this.steerWheels.contains(wheelPos)) {
            this.steerWheels.add(wheelPos);
            if (this.level.getBlockEntity(wheelPos) instanceof SmartWheelMountBlockEntity wheel){
                wheel.linkToSteeringWheel(this.worldPosition,player);
            }
            if(player!= null) {player.sendSystemMessage(Component.literal("Successfully linked wheel mount to steer wheel!"));}
        }
        else
        {
            if(player!= null) {player.displayClientMessage(Component.literal("already linked as steer wheel"), false);}
        }
        this.setChanged();
        this.sendData();
    }

    public void linkBrakeWheels(BlockPos wheelPos,Player player) {
        if(!this.brakeWheels.contains(wheelPos)){
            this.brakeWheels.add(wheelPos);
            if (this.level.getBlockEntity(wheelPos) instanceof SmartWheelMountBlockEntity wheel){
                wheel.linkToSteeringWheel(this.worldPosition,player);
            }
            if(player!= null) {player.sendSystemMessage(Component.literal("Successfully linked wheel mount to brake wheel!"));}
        }
        else
        {
            if(player!= null) {player.displayClientMessage(Component.literal("already linked as brake wheel"), false);}
        }
        this.setChanged();
        this.sendData();
    }

    public void unlinkWheels(BlockPos wheelPos,Player player){
        player.displayClientMessage(Component.literal("unlink executed"), false);
        if(this.brakeWheels.contains(wheelPos)){
            this.brakeWheels.remove(wheelPos);
            player.sendSystemMessage(Component.literal("Successfully unlinked wheel mount to brake wheel!"));
        }
        if(this.steerWheels.contains(wheelPos)) {
            this.steerWheels.remove(wheelPos);
            player.sendSystemMessage(Component.literal("Successfully unlinked wheel mount to steer wheel!"));
        }
        this.setChanged();
        this.sendData();
    }

    // Example - drop this into your server-side tick() branch in SteeringWheelBlockEntity.
// Assumes steerWheels/brakeWheels are switched to List<BlockPos> (see note below) instead
// of List<WheelMountBlockEntity>, so links survive chunk unload/reload like linkedEnginePos does.

    private void pushSteeringAndBrakeOverrides() {
        if (this.level == null) return;

        int steerSignal = (int) (SteeringDirection * 15);

        for (BlockPos pos : steerWheels) {
            if (this.level.getBlockEntity(pos) instanceof SmartWheelMountBlockEntity wheel) {
                wheel.turn(steerSignal);
            }
        }

        // FIX 2: Evaluate the true/false state and pass it straight to the wheels
        boolean brakeSignal = BrakeInput > 0;

        for (BlockPos pos : brakeWheels) {
            if (this.level.getBlockEntity(pos) instanceof SmartWheelMountBlockEntity wheel) {
                // By calling setBraking, we actively apply OR release brakes every tick
                wheel.setBraking(brakeSignal);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void tryToggleActive() {
        if (user == null && Minecraft.getInstance().player.getUUID().equals(prevUser)) {
            SteeringWheeClientHandler.deActivate();
        } else if (prevUser == null && Minecraft.getInstance().player.getUUID().equals(user)) {
            SteeringWheeClientHandler.activate(Minecraft.getInstance().player,worldPosition);
        }
    }

    public Couple<Frequency> getFrequencyForButton(int button) {
        int column = button - 1;
        if (column < 0)
            return null;
        int first = column * 2;
        int second = column * 2 + 1;
        if (second >= frequencySlots.getSlots())
            return null;
        ItemStack a = frequencySlots.getStackInSlot(first);
        ItemStack b = frequencySlots.getStackInSlot(second);
        if (a.isEmpty() && b.isEmpty())
            return null;
        return Couple.create(Frequency.of(a), Frequency.of(b));
    }

    // Replace SetFLEBE with safe link generation:
    public void linkEngine(BlockPos enginePos, Player player) {
        this.linkedEnginePos = enginePos;
        this.setChanged();
        this.sendData();

        if (this.level != null && this.level.getBlockEntity(enginePos) instanceof FourLineEngineBlockEntity engine) {
            engine.linkWheel(this.worldPosition);
        }
        if (player != null) {
            player.displayClientMessage(Component.literal("Engine Connected"), false);
        }
    }

    public void clearEngineLink() {
        this.linkedEnginePos = null;
        this.ignition = false;
        this.setChanged();
        this.sendData();
    }

    // Replace unlinkEngine:
    public void unlinkEngine() {
        if (this.linkedEnginePos != null && this.level != null) {
            if (this.level.getBlockEntity(this.linkedEnginePos) instanceof FourLineEngineBlockEntity engine) {
                engine.clearWheelLink();
            }
            this.linkedEnginePos = null;
            this.setChanged();
            this.sendData();
        }
    }

    public static final int IGNITION_INDEX = 4; // matches CACWControls.getControls() ordering: JUMP = ignition

    private void turnengineOnOff(boolean on) {
        if (this.linkedEnginePos == null || this.level == null) {
            return;
        }

        if (this.level.getBlockEntity(this.linkedEnginePos) instanceof FourLineEngineBlockEntity engine) {
            engine.SetTurnONOff(this.ignition);
            setIgnition(engine.isTurnedOn());
        } else {
            // The engine is missing! Clean up the broken link.
            clearEngineLink();
        }
    }

    // Replace toggleIgnition to fetch the engine safely:
    public void toggleIgnition(Player player) {
        if (this.linkedEnginePos == null || this.level == null) {
            player.displayClientMessage(Component.literal("no engine connected"), false);
            return;
        }

        if (this.level.getBlockEntity(this.linkedEnginePos) instanceof FourLineEngineBlockEntity engine) {
            engine.toggleIgnition();
            player.displayClientMessage(
                    Component.literal(engine.isTurnedOn() ? "ignition on" : "ignition off"), false);
            setIgnition(engine.isTurnedOn());
            player.displayClientMessage(
                    Component.literal("ignition state is " + this.ignition ), false);
        } else {
            // The engine is missing! Clean up the broken link.
            clearEngineLink();
            player.displayClientMessage(Component.literal("Linked engine was missing! Link broken."), false);
        }
    }

    public void setIgnition(boolean on) {
        if (this.ignition == on) return; // avoid needless packets
        this.ignition = on;
        setChanged();
        sendData();
    }

    public float getKeyRenderAngle(float partialTicks) {
        return Mth.lerp(partialTicks, prevclientKeyAngle, clientKeyAngle);
    }

    public static final int SteeringLeftIndex = 2;
    public static final int SteeringRightIndex = 3;

    public void setSteeringButton(int index, boolean pressed) {
        if (index == SteeringLeftIndex) leftHeld = pressed;
        else if (index == SteeringRightIndex) rightHeld = pressed;
        else return;

        float newDirection = (leftHeld ? -1f : 0f) + (rightHeld ? 1f : 0f);
        if (newDirection != SteeringDirection) {
            SteeringDirection = newDirection;
            setChanged();
            sendData(); // <-- this is what actually syncs it to the client
        }
    }

    public float getSteeringRenderAngle(float partialTicks) {
        return Mth.lerp(partialTicks, prevclientSteeringAngle, clientSteeringAngle);
    }

    public static final int brakeIndex = 1;

    public void setBrakeInput(boolean input) {

        BrakeInput = input? 15 : 0;
        setChanged();
        sendData();
    }

    // matches CACWControls.getControls() ordering: Throttle is index 0
    public static final int ThrottleIndex = 0;

    /**
     * Held-state throttle input, mirrors {@link #setBrakeInput(boolean)}. Forwards straight
     * to the linked engine as 0/1 so the engine simulation can ramp RPM/torque off it.
     */
    public void setThrottleInput(boolean pressed) {
        if (this.throttleHeld == pressed) return; // avoid needless packets
        this.throttleHeld = pressed;
        setChanged();
        sendData();

        if (this.linkedEnginePos != null && this.level != null
                && this.level.getBlockEntity(this.linkedEnginePos) instanceof FourLineEngineBlockEntity engine) {
            engine.setThrottle(pressed ? 1.0F : 0.0F);
        }
    }

    public static final int GearUpIndex = 6;
    public void GearUp(){
        if (this.linkedEnginePos != null && this.level != null
                && this.level.getBlockEntity(this.linkedEnginePos) instanceof FourLineEngineBlockEntity engine) {
            engine.shiftUp();
        }
    }

    public static final int GearDownIndex = 7;
    public void GearDown(){
        if (this.linkedEnginePos != null && this.level != null
                && this.level.getBlockEntity(this.linkedEnginePos) instanceof FourLineEngineBlockEntity engine) {
            engine.shiftDown();
        }
    }

    public boolean isThrottleHeld() {
        return this.throttleHeld;
    }

    private Vector3d getGlobalVelocity() {
        final SubLevel subLevel = this.subLevelReference.get();
        if (subLevel == null) {
            return new Vector3d();
        }

        final Vector3d jomlPos = JOMLConversion.toJOML(this.worldPosition.getCenter());
        return subLevel.logicalPose().transformPosition(jomlPos, new Vector3d()).sub(subLevel.lastPose().transformPosition(jomlPos, new Vector3d()), jomlPos).mul(20.0F);
    }

    public float getAdjustedVelocity() {
        return this.adjustedVelocity;
    }

    public float getNormalizedSpeed(){
        float calc = CACWMathHelpers.NormalizeToOne(Math.abs(this.getAdjustedVelocity()),0,MAX_SPEED_NEEDLE_MS_READ);
        return calc >= 1 ? 1 : calc;
    }

    public float getGasRenderLevel(float partialTicks) {
        return Mth.lerp(partialTicks, prevClientGasLevel, clientGasLevel);
    }

    public float GetGasTankLevel() {
        if (this.linkedEnginePos == null || this.level == null) {
            return 0f;
        }

        if (this.level.getBlockEntity(this.linkedEnginePos) instanceof FourLineEngineBlockEntity engine) {
            // 1. Get the active fluid handler (internal or linked)
            var tank = engine.getTank();

            if (tank != null) {
                // 2. Query NeoForge's fluid handler for capacity and current amount
                float maxCapacity = tank.getTankCapacity(0);
                float currentAmount = tank.getFluidInTank(0).getAmount();

                // 3. Avoid division by zero just in case
                if (maxCapacity > 0) {
                    float calc = CACWMathHelpers.NormalizeToOne(currentAmount, 0, maxCapacity);

                    // Clamp between 0 and 1 so the needle never goes out of bounds
                    return Math.max(0f, Math.min(1f, calc));
                }
            }
        }
        return 0f; // Default to Empty if something goes wrong
    }

    //jukebox

    // Called by JukeboxSongPlayer whenever play/stop happens internally (e.g. song naturally ends).
    // Keep this cheap - it can fire off-thread relative to your own logic.
    private void onJukeboxSongChanged() {
        setChanged();
        sendData();
        setDiscPlaying(jukeboxSongPlayer.isPlaying()); // <-- catches natural song-end too
    }

    /**
     * Attempts to start playing whatever disc is currently in {@link #inventory}.
     * @return true if playback started, false if there's no disc or no valid song on it.
     */
    public boolean playDisc() {
        if (this.level == null || this.level.isClientSide)
            return false;
        if (jukeboxSongPlayer.isPlaying())
            return false;

        ItemStack disc = inventory.getStackInSlot(0);
        if (disc.isEmpty())
            return false;

        Optional<Holder<JukeboxSong>> song = JukeboxSong.fromStack(this.level.registryAccess(), disc);
        if (song.isEmpty())
            return false;

        jukeboxSongPlayer.play(this.level, song.get());
        discPlaybackTicks = 0;
        setDiscPlaying(true); // <-- sync to client
        return true;
    }

    /**
     * Stops playback if a song is currently playing. Safe to call even if nothing is playing.
     */
    public void stopDisc() {
        if (this.level == null || this.level.isClientSide)
            return;

        jukeboxSongPlayer.stop(this.level, this.getBlockState());
        discPlaybackTicks = 0;
        setDiscPlaying(false); // <-- sync to client
    }

    public boolean isDiscPlaying() {
        return discPlaying;
    }

    public String GetSongName() {
        String songName = "";
        ItemStack disc = inventory.getStackInSlot(0);
        if (disc.isEmpty())
            return "";
        Optional<Holder<JukeboxSong>> song = JukeboxSong.fromStack(this.level.registryAccess(), disc);
        if (song.isEmpty())
            return "";

        String input = song.get().getRegisteredName();
        String[] parts = input.split(":");

        // Check if the array actually has two parts to prevent errors
        if (parts.length > 1) {
            songName = parts[1];
        }
        return songName;
    }

    /**
     * Convenience wrapper for wiring up a single button/keybind: stop if playing, otherwise try to play.
     */
    public void toggleDisc(Player player) {
        if (this.level == null || this.level.isClientSide)
            return;

        if (jukeboxSongPlayer.isPlaying()) {
            stopDisc();
            if (player != null)
                player.displayClientMessage(Component.literal("Music stopped"), false);
        } else {
            boolean started = playDisc();
            if (player != null)
                player.displayClientMessage(
                        Component.literal(started ? "Music started" : "No disc inserted"), false);
        }
    }

    public void setDiscPlaying(boolean playing) {
        if (this.discPlaying == playing) return;
        this.discPlaying = playing;
        setChanged();
        sendData();
    }
}
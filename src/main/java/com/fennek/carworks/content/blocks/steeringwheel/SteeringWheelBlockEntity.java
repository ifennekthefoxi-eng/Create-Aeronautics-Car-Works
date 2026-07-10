package com.fennek.carworks.content.blocks.steeringwheel;

import com.fennek.carworks.content.blocks.engines.FourLineEngine.FourLineEngineBlockEntity;
import com.fennek.carworks.content.blocks.steeringwheel.handlers.SteeringWheeClientHandler;
import com.fennek.carworks.content.blocks.steeringwheel.menu.SteeringWheelMenu;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
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

import java.util.*;

public class SteeringWheelBlockEntity extends SmartBlockEntity implements MenuProvider {

    //private FourLineEngineBlockEntity FLEBE;
    // Remove: private FourLineEngineBlockEntity FLEBE;
    private BlockPos linkedEnginePos = null; // Add this instead

    public float SteeringDirection = 0.0f;
    private boolean leftHeld = false;
    private boolean rightHeld = false;

    // client-only smoothing state - never synced directly, derived from SteeringDirection
    private float clientSteeringAngle = 0f;
    private float prevclientSteeringAngle = 0f;
    private static final float STEER_LERP_SPEED = 30f; // degrees per tick - tune to taste

    boolean ignition = false;
    private float clientKeyAngle = 0f;
    private float prevclientKeyAngle = 0f;
    private static final float KEY_LERP_SPEED = 30f; // degrees per tick - tune to taste


    // write saveData to this instead of 'encodedRequest'
    public final ItemStackHandler frequencySlots = new ItemStackHandler(6);//right now just 3 keybinds, 2 slots each

    private UUID user;
    private UUID prevUser;    // used only on client
    private boolean deactivatedThisTick;    // used only on server

    public SteeringWheelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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

    // Add to write() method
    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.put("Frequencies", frequencySlots.serializeNBT(registries));
        if (user != null)
            compound.putUUID("User", user);

        if (this.linkedEnginePos != null) {
            compound.putLong("LinkedEnginePos", this.linkedEnginePos.asLong());
        }
        compound.putFloat("SteeringDirection", SteeringDirection);
        compound.putBoolean("ignition", ignition);
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
            startUsing(player, world);
        else player.displayClientMessage(Component.literal("its in use cant steal"), false);
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
        if (player != null)
            player.getPersistentData().remove("IsUsingSteeringWheel");
        deactivatedThisTick = true;
        SteeringDirection = 0;
        setChanged();
        sendData();
        player.displayClientMessage(Component.literal("use stoped"), false);
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
        }

        if (!level.isClientSide) {
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
}

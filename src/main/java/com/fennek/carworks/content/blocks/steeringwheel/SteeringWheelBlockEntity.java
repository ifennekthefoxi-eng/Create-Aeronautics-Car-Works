package com.fennek.carworks.content.blocks.steeringwheel;

import com.fennek.carworks.content.blocks.steeringwheel.handlers.SteeringWheeClientHandler;
import com.fennek.carworks.content.blocks.steeringwheel.menu.SteeringWheelMenu;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.createmod.catnip.data.Couple;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.*;

public class SteeringWheelBlockEntity extends SmartBlockEntity implements MenuProvider {
    public PackageOrderWithCrafts encodedRequest = PackageOrderWithCrafts.empty();


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

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        //compound.put("ControllerData", CatnipCodecUtils.encode(ItemContainerContents.CODEC, registries, controllerData).orElseThrow());
        if (user != null)
            compound.putUUID("User", user);
    }

    @Override
    public void writeSafe(CompoundTag compound, HolderLookup.Provider registries) {
        super.writeSafe(compound, registries);
        //compound.put("ControllerData", CatnipCodecUtils.encode(ItemContainerContents.CODEC, registries, controllerData).orElseThrow());
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);

        //controllerData = CatnipCodecUtils.decode(ItemContainerContents.CODEC, registries, compound.get("ControllerData"))
        //        .orElse(ItemContainerContents.EMPTY);
        user = compound.hasUUID("User") ? compound.getUUID("User") : null;
    }

    public static boolean playerInRange(Player player, Level world, BlockPos pos) {
        //double modifier = world.isRemote ? 0 : 1.0;
        double reach = 0.5 * player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);// + modifier;
        return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) < reach * reach;
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

    public final ItemStackHandler frequencySlots = new ItemStackHandler(12); // 6 buttons x 2 slots

    public Couple<Frequency> getFrequencyForButton(int button) {
        var a = frequencySlots.getStackInSlot(button * 2);
        var b = frequencySlots.getStackInSlot(button * 2 + 1);
        if (a.isEmpty() && b.isEmpty())
            return null;
        return Couple.create(Frequency.of(a), Frequency.of(b));
    }
}
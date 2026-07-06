package com.fennek.carworks.content.blocks.steeringwheel.menu;

import com.fennek.carworks.CACWBocks;
import com.fennek.carworks.CACWMenuTypes;
import com.fennek.carworks.content.blocks.steeringwheel.SteeringWheelBlockEntity;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.gui.menu.GhostItemMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;

public class SteeringWheelMenu extends GhostItemMenu<SteeringWheelBlockEntity> {

    public SteeringWheelMenu(MenuType<?> type, int id, Inventory inv, SteeringWheelBlockEntity contentHolder) {
        super(type, id, inv, contentHolder);
    }

    public SteeringWheelMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public static SteeringWheelMenu create(int id, Inventory inv, SteeringWheelBlockEntity be) {
        return new SteeringWheelMenu(CACWMenuTypes.STEERING_WHEEL.get(), id, inv, be);
    }

    @Override
    protected ItemStackHandler createGhostInventory() {
        ItemStackHandler inventory = new ItemStackHandler(9);
        List<BigItemStack> stacks = contentHolder.encodedRequest.stacks();
        for (int i = 0; i < stacks.size(); i++)
            inventory.setStackInSlot(i, stacks.get(i).stack.copyWithCount(1));
        return inventory;
    }

    @Override
    protected boolean allowRepeats() {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected SteeringWheelBlockEntity createOnClient(RegistryFriendlyByteBuf extraData) {
        BlockPos blockPos = extraData.readBlockPos();
        return CACWBocks.STEERING_WHEEL.get()
                .getBlockEntity(Minecraft.getInstance().level, blockPos);
    }

    @Override
    protected void addSlots() {
        int playerX = 8;
        int playerY = 142;
        int slotX = 11;
        int slotY = 34;

        addPlayerSlots(playerX, playerY);
        addSlot(new SteeringWheelMenu.SorterProofSlot(ghostInventory, 0, slotX, slotY));
        addSlot(new SteeringWheelMenu.SorterProofSlot(ghostInventory, 1, slotX, slotY + 18));
        addSlot(new SteeringWheelMenu.SorterProofSlot(ghostInventory, 2, slotX + 24, slotY));
        addSlot(new SteeringWheelMenu.SorterProofSlot(ghostInventory, 3, slotX + 24, slotY + 18));
        addSlot(new SteeringWheelMenu.SorterProofSlot(ghostInventory, 4, slotX + 48, slotY));
        addSlot(new SteeringWheelMenu.SorterProofSlot(ghostInventory, 5, slotX + 48, slotY + 18));

    }

    @Override
    protected void saveData(SteeringWheelBlockEntity contentHolder) {
        List<BigItemStack> stacks = contentHolder.encodedRequest.stacks();
        ArrayList<BigItemStack> list = new ArrayList<>();
        for (int i = 0; i < ghostInventory.getSlots(); i++) {
            ItemStack stackInSlot = ghostInventory.getStackInSlot(i);
            if (stackInSlot.isEmpty())
                continue;
            list.add(new BigItemStack(stackInSlot.copyWithCount(1), i < stacks.size() ? stacks.get(i).count : 1));
        }

        PackageOrderWithCrafts newRequest = new PackageOrderWithCrafts(new PackageOrder(list), contentHolder.encodedRequest.orderedCrafts());
        if (!newRequest.orderedStacksMatchOrderedRecipes())
            newRequest = PackageOrderWithCrafts.simple(newRequest.stacks());
        contentHolder.encodedRequest = newRequest;
        contentHolder.sendData();
    }

    // this is used to prevent InventorySorter from interfering with scrolling on the slots.
    // we just need a class to use as a marker, see InventorySorterCompat
    public static class SorterProofSlot extends SlotItemHandler {
        public SorterProofSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }
    }
}
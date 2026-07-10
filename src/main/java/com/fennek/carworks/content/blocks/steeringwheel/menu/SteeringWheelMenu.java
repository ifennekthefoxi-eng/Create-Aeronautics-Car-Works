package com.fennek.carworks.content.blocks.steeringwheel.menu;

import com.fennek.carworks.CACWBocks;
import com.fennek.carworks.CACWMenuTypes;
import com.fennek.carworks.content.blocks.steeringwheel.SteeringWheelBlockEntity;
import com.simibubi.create.foundation.gui.menu.GhostItemMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class SteeringWheelMenu extends GhostItemMenu<SteeringWheelBlockEntity> {

    // 3 keybind, 2 slots each, has to equal SteeringWheelBlockEntity.frequencySlots.getSlots()
    private static final int SLOT_COUNT = 6;

    public SteeringWheelMenu(MenuType<?> type, int id, Inventory inv, SteeringWheelBlockEntity contentHolder) {
        super(type, id, inv, contentHolder);
    }

    public SteeringWheelMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public static SteeringWheelMenu create(int id, Inventory inv, SteeringWheelBlockEntity be) {
        return new SteeringWheelMenu(CACWMenuTypes.STEERING_WHEEL.get(), id, inv, be);
    }

    // this was using PackageOrder/encoded request before, which is a seperate store the input packet isnt reading
    @Override
    protected ItemStackHandler createGhostInventory() {
        ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT);
        ItemStackHandler source = contentHolder.frequencySlots;
        for (int i = 0; i < SLOT_COUNT && i < source.getSlots(); i++)
            inventory.setStackInSlot(i, source.getStackInSlot(i).copy());
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
        addSlot(new SorterProofSlot(ghostInventory, 0, slotX, slotY));           // S, freq 1
        addSlot(new SorterProofSlot(ghostInventory, 1, slotX, slotY + 18));      // S, freq 2
        addSlot(new SorterProofSlot(ghostInventory, 2, slotX + 24, slotY));      // A, freq 1
        addSlot(new SorterProofSlot(ghostInventory, 3, slotX + 24, slotY + 18)); // A, freq 2
        addSlot(new SorterProofSlot(ghostInventory, 4, slotX + 48, slotY));      // D, freq 1
        addSlot(new SorterProofSlot(ghostInventory, 5, slotX + 48, slotY + 18)); // D, freq 2
    }

    @Override
    protected void saveData(SteeringWheelBlockEntity contentHolder) {
        // commit the slots server-side
        if (contentHolder.getLevel() == null || contentHolder.getLevel().isClientSide)
            return;
        ItemStackHandler target = contentHolder.frequencySlots;
        for (int i = 0; i < SLOT_COUNT && i < target.getSlots(); i++)
            target.setStackInSlot(i, ghostInventory.getStackInSlot(i).copy());
        contentHolder.setChanged();
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

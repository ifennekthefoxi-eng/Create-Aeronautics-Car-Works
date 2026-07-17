package com.fennek.carworks.content.blocks.steeringwheel.menu;

import com.fennek.carworks.CACWBocks;
import com.fennek.carworks.CACWMenuTypes;
import com.fennek.carworks.content.blocks.steeringwheel.SteeringWheelBlockEntity;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.gui.menu.GhostItemMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.minecraft.core.component.DataComponents;


public class SteeringWheelMenu extends GhostItemMenu<SteeringWheelBlockEntity> {

    public Slot DiscSlot;

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

        // FIXED: Changed the index from 6 to 0.
        // Since contentHolder.inventory only has a size of 1, its only valid index is 0.
        DiscSlot = new SlotItemHandler(contentHolder.inventory, 0, 99, 19) {

            @Override
            public boolean mayPlace(ItemStack stack) {
                // Keep this method PURE. Only return true/false. No sounds or messages here!
                return stack.has(DataComponents.JUKEBOX_PLAYABLE);
            }

            @Override
            public void set(ItemStack stack) {
                super.set(stack);
                if (!stack.isEmpty() && contentHolder.getLevel() != null) {
                    Level level = contentHolder.getLevel();
                    AllSoundEvents.CONTROLLER_CLICK.playAt(level, contentHolder.getBlockPos(), 1f, 0.5f, true);
                }
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                //AllSoundEvents.CONTROLLER_CLICK.playAt(player.level(), contentHolder.getBlockPos(), 1f, 0.4f, true);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        };

        addPlayerSlots(playerX, playerY);
        addSlot(new SorterProofSlot(ghostInventory, 0, slotX, slotY));           // S, freq 1
        addSlot(new SorterProofSlot(ghostInventory, 1, slotX, slotY + 18));      // S, freq 2
        addSlot(new SorterProofSlot(ghostInventory, 2, slotX + 24, slotY));      // A, freq 1
        addSlot(new SorterProofSlot(ghostInventory, 3, slotX + 24, slotY + 18)); // A, freq 2
        addSlot(new SorterProofSlot(ghostInventory, 4, slotX + 48, slotY));      // D, freq 1
        addSlot(new SorterProofSlot(ghostInventory, 5, slotX + 48, slotY + 18)); // D, freq 2

        addSlot(DiscSlot);
    }

    /*@Override
    protected void addSlots() {
        int playerX = 8;
        int playerY = 142;
        int slotX = 11;
        int slotY = 34;

        DiscSlot = new SlotItemHandler(contentHolder.inventory, 6, slotX + 72, slotY) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AllItems.EMPTY_SCHEMATIC.isIn(stack) || AllItems.SCHEMATIC_AND_QUILL.isIn(stack)
                        || AllItems.SCHEMATIC.isIn(stack);
            }
        };

        addPlayerSlots(playerX, playerY);
        addSlot(new SorterProofSlot(ghostInventory, 0, slotX, slotY));           // S, freq 1
        addSlot(new SorterProofSlot(ghostInventory, 1, slotX, slotY + 18));      // S, freq 2
        addSlot(new SorterProofSlot(ghostInventory, 2, slotX + 24, slotY));      // A, freq 1
        addSlot(new SorterProofSlot(ghostInventory, 3, slotX + 24, slotY + 18)); // A, freq 2
        addSlot(new SorterProofSlot(ghostInventory, 4, slotX + 48, slotY));      // D, freq 1
        addSlot(new SorterProofSlot(ghostInventory, 5, slotX + 48, slotY + 18)); // D, freq 2
        addSlot(DiscSlot);

    }*/

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

    public static final int BUTTON_PLAY_DISC = 0;
    public static final int BUTTON_STOP_DISC = 1;

    // Screen buttons only run client-side code; this is the vanilla RPC (same one Beacon/Loom use)
    // that routes a button click to the server via ServerboundContainerButtonClickPacket, so we can
    // actually call play/stop on the real, server-side contentHolder.
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (contentHolder.getLevel() == null || contentHolder.getLevel().isClientSide)
            return false;

        if (id == BUTTON_PLAY_DISC) {
            contentHolder.playDisc();
            return true;
        } else if (id == BUTTON_STOP_DISC) {
            contentHolder.stopDisc();
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    // this is used to prevent InventorySorter from interfering with scrolling on the slots.
    // we just need a class to use as a marker, see InventorySorterCompat
    public static class SorterProofSlot extends SlotItemHandler {
        public SorterProofSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickTypeIn, Player player) {
        // If the player clicks the physical Disc Slot, bypass the Ghost item logic completely
        if (DiscSlot != null && slotId == DiscSlot.index) {
            Slot slot = this.slots.get(slotId);
            ItemStack holding = this.getCarried();

            if (clickTypeIn == ClickType.PICKUP) {
                if (holding.isEmpty()) {
                    // Take item out
                    ItemStack taken = slot.getItem().copy(); // Save a copy of the item being removed
                    this.setCarried(slot.getItem());
                    slot.set(ItemStack.EMPTY);
                    slot.onTake(player, taken); // MANUALLY TRIGGER THE SOUND AND MESSAGE
                } else if (slot.mayPlace(holding)) {
                    // Put item in (or swap items)
                    ItemStack temp = slot.getItem().copy();
                    slot.set(holding);
                    this.setCarried(temp);
                    if (!temp.isEmpty()) {
                        slot.onTake(player, temp); // TRIGGER IF SWAPPING AN EXISTING DISC
                    }
                }
            }
            return; // Stop the GhostItemMenu from crashing
        }

        // Otherwise, proceed with normal ghost logic for the frequency slots
        super.clicked(slotId, dragType, clickTypeIn, player);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        // Handle shift-clicking an item OUT of the Disc Slot
        if (DiscSlot != null && index == DiscSlot.index) {
            Slot slot = this.slots.get(index);
            if (slot != null && slot.hasItem()) {
                ItemStack stackInSlot = slot.getItem().copy();
                ItemStack taken = stackInSlot.copy(); // Save a copy of what we are taking

                // Move back to player inventory (slots 0 to 35)
                if (!this.moveItemStackTo(stackInSlot, 0, 36, false)) {
                    return ItemStack.EMPTY;
                }

                slot.set(ItemStack.EMPTY);
                slot.onTake(playerIn, taken); // MANUALLY TRIGGER THE SOUND AND MESSAGE
            }
            return ItemStack.EMPTY;
        }

        // Handle shift-clicking an item FROM the player's inventory INTO the Disc Slot
        if (index < 36) {
            Slot clickedPlayerSlot = this.slots.get(index);
            ItemStack stackToInsert = clickedPlayerSlot.getItem();

            if (DiscSlot != null && DiscSlot.mayPlace(stackToInsert) && !stackToInsert.isEmpty()) {
                if (!DiscSlot.hasItem()) {
                    DiscSlot.set(stackToInsert.split(1)); // Insert exactly 1 item
                    clickedPlayerSlot.setChanged();
                    return ItemStack.EMPTY; // Stop ghost logic from copying it to frequencies
                }
            }
        }

        return super.quickMoveStack(playerIn, index);
    }
}
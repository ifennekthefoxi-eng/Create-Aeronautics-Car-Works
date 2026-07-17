package com.fennek.carworks.content.blocks.steeringwheel.menu;

import com.fennek.carworks.CACWBocks;
import com.fennek.carworks.CACWGuiTextures;
import com.fennek.carworks.utility.CACWControls;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.world.inventory.Slot;

public class SteeringWheelScreen extends AbstractSimiContainerScreen<SteeringWheelMenu>{

    private IconButton confirmButton;
    private IconButton playButton;
    private IconButton stopButton;

    public SteeringWheelScreen(SteeringWheelMenu container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Override
    protected void init() {
        int bgHeight = CACWGuiTextures.STEERING_WHEEL_FREQUENCIES.getHeight();
        int bgWidth = CACWGuiTextures.STEERING_WHEEL_FREQUENCIES.getWidth();
        setWindowSize(bgWidth, bgHeight + AllGuiTextures.PLAYER_INVENTORY.getHeight());
        super.init();
        clearWidgets();
        int x = getGuiLeft();
        int y = getGuiTop();

        confirmButton = new IconButton(x + 61, y + bgHeight - 23, AllIcons.I_CONFIRM);
        confirmButton.withCallback(() -> minecraft.player.closeContainer());
        addRenderableWidget(confirmButton);

        playButton = new IconButton(x + 88, y + 37, AllIcons.I_PLAY);
        playButton.withCallback(() ->
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SteeringWheelMenu.BUTTON_PLAY_DISC));
        addRenderableWidget(playButton);

        stopButton = new IconButton(x + 108, y + 37, AllIcons.I_STOP);
        stopButton.withCallback(() ->
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SteeringWheelMenu.BUTTON_STOP_DISC));
        addRenderableWidget(stopButton);
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int x = getGuiLeft();
        int y = getGuiTop();
        CACWGuiTextures.STEERING_WHEEL_FREQUENCIES.render(pGuiGraphics, x, y);
        renderPlayerInventory(pGuiGraphics, x, y + 124);

        ItemStack stack = CACWBocks.STEERING_WHEEL.asStack();
        Component title = CreateLang.text(stack.getHoverName()
                        .getString())
                .component();
        pGuiGraphics.drawString(font, title, x + 4, y + 4, 0x3D3C48, false);
        pGuiGraphics.drawString(font, "RADIO", x + 88, y + 4, 0x3D3C48, false);

        GuiGameElement.of(stack)
                .scale(3)
                .render(pGuiGraphics, x + 100, y + 70);
    }

    /*@Override
    protected void renderTooltip(GuiGraphics graphics, int x, int y) {
        if (!menu.getCarried()
                .isEmpty() || this.hoveredSlot == null || hoveredSlot.container == menu.playerInventory) {
            super.renderTooltip(graphics, x, y);
            return;
        }

        List<Component> list = new LinkedList<>();
        if (hoveredSlot.hasItem())
            list = getTooltipFromContainerItem(hoveredSlot.getItem());

        graphics.renderComponentTooltip(font, addToTooltip(list, hoveredSlot.getSlotIndex()), x, y);
    }*/

    @Override
    protected void renderTooltip(GuiGraphics graphics, int x, int y) {
        if (!menu.getCarried()
                .isEmpty() || this.hoveredSlot == null || hoveredSlot.container == menu.playerInventory) {
            super.renderTooltip(graphics, x, y);
            return;
        }

        List<Component> list = new LinkedList<>();
        if (hoveredSlot.hasItem())
            list = getTooltipFromContainerItem(hoveredSlot.getItem());

        graphics.renderComponentTooltip(font, addToTooltip(list, hoveredSlot), x, y);
    }

    /*private List<Component> addToTooltip(List<Component> list, int slot) {
        if (slot < 0 || slot >= 12)
            return list;
        int keybind = (slot / 2) + 1;
        if (keybind >= CACWControls.getControls().size())
            return list;
        list.add(CreateLang.translateDirect("linked_controller.frequency_slot_" + ((slot % 2) + 1), CACWControls.getControls()
                        .get(keybind)
                        .getTranslatedKeyMessage()
                        .getString())
                .withStyle(ChatFormatting.GOLD));
        return list;
    }*/

    private List<Component> addToTooltip(List<Component> list, Slot slot) {
        // Identify the DiscSlot by reference, not index — its local index (0) can
        // collide with the frequency ghost slots' local indices (also 0-5).
        if (slot == menu.DiscSlot) {
            list.add(Component.literal("Slot for a Music Disc").withStyle(ChatFormatting.GREEN));
            return list;
        }

        int localIndex = slot.getSlotIndex();
        if (localIndex < 0 || localIndex >= 6) {
            return list;
        }

        int keybind = (localIndex / 2) + 1;
        if (keybind >= CACWControls.getControls().size())
            return list;

        list.add(CreateLang.translateDirect("linked_controller.frequency_slot_" + ((localIndex % 2) + 1), CACWControls.getControls()
                        .get(keybind)
                        .getTranslatedKeyMessage()
                        .getString())
                .withStyle(ChatFormatting.GOLD));
        return list;
    }
}
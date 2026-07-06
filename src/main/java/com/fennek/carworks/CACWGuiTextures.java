package com.fennek.carworks;

import net.createmod.catnip.gui.TextureSheetSegment;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public enum CACWGuiTextures implements ScreenElement, TextureSheetSegment {
    STEERING_WHEEL_FREQUENCIES("steering_wheel_link_menu", 176, 108);

    public static final int FONT_COLOR = 5726074;
    public final ResourceLocation location;
    private final int width;
    private final int height;
    private final int startX;
    private final int startY;

    private CACWGuiTextures(String location, int width, int height) {
        this(location, 0, 0, width, height);
    }

    private CACWGuiTextures(String location, int startX, int startY, int width, int height) {
        this("createaeronauticscarworks", location, startX, startY, width, height);
    }

    private CACWGuiTextures(String namespace, String location, int startX, int startY, int width, int height) {
        this.location = ResourceLocation.fromNamespaceAndPath(namespace, "textures/gui/" + location + ".png");
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
    }

    public ResourceLocation getLocation() {
        return this.location;
    }

    @OnlyIn(Dist.CLIENT)
    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(this.location, x, y, this.startX, this.startY, this.width, this.height);
    }

    @OnlyIn(Dist.CLIENT)
    public void render(GuiGraphics graphics, int x, int y, Color c) {
        this.bind();
        UIRenderHelper.drawColoredTexture(graphics, c, x, y, this.startX, this.startY, this.width, this.height);
    }

    public int getStartX() {
        return this.startX;
    }

    public int getStartY() {
        return this.startY;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }
}

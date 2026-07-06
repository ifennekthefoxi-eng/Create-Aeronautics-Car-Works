package com.fennek.carworks;

import com.fennek.carworks.content.blocks.steeringwheel.menu.SteeringWheelMenu;
import com.fennek.carworks.content.blocks.steeringwheel.menu.SteeringWheelScreen;
import com.tterrag.registrate.builders.MenuBuilder;
import com.tterrag.registrate.util.entry.MenuEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class CACWMenuTypes {

    public static final MenuEntry<SteeringWheelMenu> STEERING_WHEEL =
            register("steering_wheel", SteeringWheelMenu::new, () -> SteeringWheelScreen::new);

    private static <C extends AbstractContainerMenu, S extends Screen & MenuAccess<C>> MenuEntry<C> register(
            String name, MenuBuilder.ForgeMenuFactory<C> factory, NonNullSupplier<MenuBuilder.ScreenFactory<C, S>> screenFactory) {
        return CreateAeronauticsCarWorks.REGISTRATE
                .menu(name, factory, screenFactory)
                .register();
    }

    public static void register() {
    }

}
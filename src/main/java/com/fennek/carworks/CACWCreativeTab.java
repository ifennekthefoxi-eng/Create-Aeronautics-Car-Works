package com.fennek.carworks;

import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import java.util.function.Supplier;

public class CACWCreativeTab {
    private static final ResourceLocation CARWORS_TAB = ResourceLocation.fromNamespaceAndPath(CreateAeronauticsCarWorks.ID, "carworks_tab");
    private static boolean sectionsInitialized = false;

    public static synchronized void registerAeronauticsSections() {
        if (sectionsInitialized) {
            return;
        }

        registerSectionItem(CARWORS_TAB, "four_line_engine", () -> CACWBocks.FOUR_LINE_ENGINE.get().asItem());
        registerSectionItem(CARWORS_TAB, "steering_wheel", () -> CACWBocks.STEERING_WHEEL.get().asItem());
        registerSectionItem(CARWORS_TAB, "linker_tool", () -> CACWItems.LINKER_TOOL.get().asItem());

        sectionsInitialized = true;
    }

    private static void registerSectionItem(ResourceLocation sectionId, String itemPath, Supplier<Item> itemSupplier) {
        SimulatedRegistrate.TAB_ITEMS.add(itemSupplier);
        SimulatedRegistrate.ITEM_TO_SECTION.put(ResourceLocation.fromNamespaceAndPath(CreateAeronauticsCarWorks.ID, itemPath), sectionId);
    }
}
package com.fennek.carworks;

import com.fennek.carworks.content.items.linkertool.LinkerTool;
import com.simibubi.create.*;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.tags.ItemTags;

import static com.fennek.carworks.CreateAeronauticsCarWorks.REGISTRATE;

public class CACWItems {

    static {
        REGISTRATE.setCreativeTab(AllCreativeModeTabs.BASE_CREATIVE_TAB);
    }


    public static final ItemEntry<LinkerTool> LINKER_TOOL = REGISTRATE.item("linker_tool", LinkerTool::new)
            .properties(p -> p.stacksTo(1)
                    .durability(99))
            .tag(ItemTags.DURABILITY_ENCHANTABLE)
            .register();

    public static void register() {
    }

}
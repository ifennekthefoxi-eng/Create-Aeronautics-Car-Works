package com.fennek.carworks;

import com.fennek.carworks.utility.CACWLangRegister;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import com.jesz.createdieselgenerators.compat.EveryCompatCompat;
import com.jesz.createdieselgenerators.compat.computercraft.CCProxy;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;

import static com.fennek.carworks.CreateAeronauticsCarWorks.ID;

@Mod(ID)
public class CreateAeronauticsCarWorks {

    public static final String ID = "createaeronauticscarworks";
    public static final String VERSION = "0.1.0-BETA";

    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID)
            .setTooltipModifierFactory(item ->
                    new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                            .andThen(TooltipModifier.mapNull(KineticStats.create(item)))
            );
    public CreateAeronauticsCarWorks(IEventBus modEventBus, ModContainer container) {
        REGISTRATE.defaultCreativeTab((ResourceKey<CreativeModeTab>) null);
        REGISTRATE.registerEventListeners(modEventBus);


        CACWLangRegister.registerLang();
        CACWBocks.register();
        CACWBlockEntityTypes.register();
        CACWItems.register();
        CACWSoundEvents.register(modEventBus);
        CACWMenuTypes.register();
        CACWPackets.register();
        CACWCreativeTab.registerAeronauticsSections();

        if (ModList.get().isLoaded("moonlight"))
            EveryCompatCompat.init();
        Mods.COMPUTERCRAFT.executeIfInstalled(() -> CCProxy::register);

        CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> onClient(modEventBus, container));
    }

    public static void onClient(IEventBus modEventBus, ModContainer container) {
        CACWPartialModels.init();
    }

    public static ResourceLocation rl(String path){
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

    public static Component lang(String path, Object... args) {
        return Component.translatable(ID+"."+path, args);
    }
}

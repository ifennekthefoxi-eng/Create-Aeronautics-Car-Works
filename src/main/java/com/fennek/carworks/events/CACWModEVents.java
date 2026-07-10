package com.fennek.carworks.events;

import com.fennek.carworks.CreateAeronauticsCarWorks;
import com.fennek.carworks.content.blocks.engines.FourLineEngine.FourLineEngineBlockEntity;
import com.fennek.carworks.content.blocks.steeringwheel.handlers.SteeringWheelServerHandler;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = CreateAeronauticsCarWorks.ID)
public class CACWModEVents {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        //FourLineEngineBlockEntity.registerCapabilities(event);
    }

    @SubscribeEvent
    public static void onServerWorldTick(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
        Level world = event.getLevel();
        if (world.isClientSide())
            return;
        SteeringWheelServerHandler.tick(world);
    }

}
package com.fennek.carworks.events;

import com.fennek.carworks.CreateAeronauticsCarWorks;
import com.fennek.carworks.content.blocks.steeringwheel.handlers.SteeringWheeClientHandler;
import com.fennek.carworks.utility.CACWControls; // Import your controls utility class

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

// Clean annotation without the deprecated 'bus' parameter
@EventBusSubscriber(modid = CreateAeronauticsCarWorks.ID, value = Dist.CLIENT)
public class CACWclientEvents {

    // --- GAME BUS EVENTS (Routed automatically) ---

    @SubscribeEvent
    public static void onTickPre(ClientTickEvent.Pre event) {
        onTick(true);
    }

    @SubscribeEvent
    public static void onTickPost(ClientTickEvent.Post event) {
        onTick(false);
    }

    public static void onTick(boolean isPreEvent) {
        if (!isGameActive())
            return;

        if (isPreEvent) {
            SteeringWheeClientHandler.tick();
        }
    }

    protected static boolean isGameActive() {
        return !(Minecraft.getInstance().level == null || Minecraft.getInstance().player == null);
    }

    // --- MOD BUS EVENTS (Routed automatically via IModBusEvent) ---

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CACWControls.Throttle);
        event.register(CACWControls.Brakes);
        event.register(CACWControls.Steer_Left);
        event.register(CACWControls.Steer_Right);
        event.register(CACWControls.Ignition);
        event.register(CACWControls.CUSTOM_SHIFT);
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiLayersEvent event) {
        // event.registerAbove(VanillaGuiLayers.HOTBAR, Create.asResource("steering_wheel"), SteeringWheelnteractionHandler.OVERLAY);
    }
}
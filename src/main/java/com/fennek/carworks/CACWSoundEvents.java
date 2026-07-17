package com.fennek.carworks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CACWSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, "createaeronauticscarworks");

    public static DeferredHolder<SoundEvent, SoundEvent> FOURLINE_START = registerSoundEvent("four_line_engine_start");
    public static DeferredHolder<SoundEvent, SoundEvent> FOURLINE_IDLE = registerSoundEvent("four_line_engine_idle");
    public static DeferredHolder<SoundEvent, SoundEvent> FOURLINE_LOW = registerSoundEvent("four_line_engine_low");
    public static DeferredHolder<SoundEvent, SoundEvent> FOURLINE_MID = registerSoundEvent("four_line_engine_mid"); // New
    public static DeferredHolder<SoundEvent, SoundEvent> FOURLINE_HIGH = registerSoundEvent("four_line_engine_high"); // New

    private static DeferredHolder<SoundEvent, SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(CreateAeronauticsCarWorks.rl(name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
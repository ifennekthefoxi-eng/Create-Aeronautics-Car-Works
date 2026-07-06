package com.fennek.carworks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CACWSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS;
    public static DeferredHolder<SoundEvent, SoundEvent> FOURLINE_START;
    public static DeferredHolder<SoundEvent, SoundEvent> FOURLINE_IDLE;

    public CACWSoundEvents() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(CreateAeronauticsCarWorks.rl(name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }

    static {
        SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, "createaeronauticscarworks");
        FOURLINE_START = registerSoundEvent("four_line_engine_start");
        FOURLINE_IDLE = registerSoundEvent("four_line_engine_idle");
    }
}

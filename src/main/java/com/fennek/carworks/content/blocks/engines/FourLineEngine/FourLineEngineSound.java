package com.fennek.carworks.content.blocks.engines.FourLineEngine;

import com.fennek.carworks.CACWSoundEvents;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class FourLineEngineSound extends AbstractTickableSoundInstance {

    private final FourLineEngineBlockEntity engine;

    public FourLineEngineSound(FourLineEngineBlockEntity engine, boolean isStartup, float volume) {
        super(
                isStartup ? CACWSoundEvents.FOURLINE_START.get() : CACWSoundEvents.FOURLINE_IDLE.get(),
                SoundSource.BLOCKS,
                RandomSource.create()
        );
        this.engine = engine;
        this.looping = !isStartup;
        this.delay = 0;
        this.volume = volume;//1.0F;
        this.pitch = 1.0F;
        this.attenuation = Attenuation.LINEAR;
        this.relative = false;

        // Center the sound directly in the middle of the block
        this.x = engine.getBlockPos().getX() + 0.5D;
        this.y = engine.getBlockPos().getY() + 0.5D;
        this.z = engine.getBlockPos().getZ() + 0.5D;
    }

    @Override
    public void tick() {
        if (!this.engine.isRemoved() && this.engine.isEngineRunning()) {
            this.x = this.engine.getBlockPos().getX() + 0.5D;
            this.y = this.engine.getBlockPos().getY() + 0.5D;
            this.z = this.engine.getBlockPos().getZ() + 0.5D;
        } else {
            this.stop();
        }
    }

    public void stopSound() {
        this.stop();
    }
}
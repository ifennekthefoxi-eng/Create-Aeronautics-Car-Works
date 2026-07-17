package com.fennek.carworks.content.blocks.engines;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;

public class EngineStartupSound<T extends BlockEntity & CACWEngine> extends AbstractTickableSoundInstance {
    private final T engine;

    public EngineStartupSound(T engine, SoundEvent sound, float volume) {
        super(sound, SoundSource.BLOCKS, RandomSource.create());
        this.engine = engine;
        this.looping = false;
        this.delay = 0;
        this.volume = volume;
        this.pitch = 1.0F;
        this.attenuation = Attenuation.LINEAR;
        this.updatePosition();
    }

    private void updatePosition() {
        this.x = engine.getBlockPos().getX() + 0.5D;
        this.y = engine.getBlockPos().getY() + 0.5D;
        this.z = engine.getBlockPos().getZ() + 0.5D;
    }

    @Override
    public void tick() {
        if (this.engine.isRemoved() || !this.engine.isEngineRunning()) {
            this.stop();
            return;
        }
        this.updatePosition();
    }

    public void stopSound() {
        this.stop();
    }
}
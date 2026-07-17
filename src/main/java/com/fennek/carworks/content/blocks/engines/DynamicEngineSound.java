package com.fennek.carworks.content.blocks.engines;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;

public class DynamicEngineSound<T extends BlockEntity & CACWEngine> extends AbstractTickableSoundInstance {
    private final T engine;
    private final float baseVolume;
    private final float minFadeRpm;
    private final float peakFadeRpm;
    private final float maxFadeRpm;
    private final float minPitch;
    private final float maxPitch;
    private final float speedPitchMod;

    public DynamicEngineSound(T engine, SoundEvent sound, float baseVolume,
                              float minFadeRpm, float peakFadeRpm, float maxFadeRpm,
                              float minPitch, float maxPitch, float speedPitchMod) {
        super(sound, SoundSource.BLOCKS, RandomSource.create());
        this.engine = engine;
        this.baseVolume = baseVolume;
        this.minFadeRpm = minFadeRpm;
        this.peakFadeRpm = peakFadeRpm;
        this.maxFadeRpm = maxFadeRpm;
        this.minPitch = minPitch;
        this.maxPitch = maxPitch;
        this.speedPitchMod = speedPitchMod;
        this.looping = true;
        this.delay = 0;
        this.attenuation = Attenuation.LINEAR;

        this.updatePosition();

        // CRITICAL FIX: Evaluate state immediately so the SoundManager doesn't
        // receive a 0.0F volume and instantly cull the sound instance.
        this.updateState(engine.getCurrentRPM());
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
        this.updateState(this.engine.getCurrentRPM());
    }

    private void updateState(float rpm) {
        float targetVolume = 0.0F;

        // 1. Crossfade Volume Calculation
        if (rpm <= minFadeRpm || rpm >= maxFadeRpm) {
            targetVolume = 0.0F;
        } else if (rpm < peakFadeRpm) {
            targetVolume = baseVolume * Mth.clamp((rpm - minFadeRpm) / (peakFadeRpm - minFadeRpm), 0.0F, 1.0F);
        } else {
            targetVolume = baseVolume * Mth.clamp(1.0F - ((rpm - peakFadeRpm) / (maxFadeRpm - peakFadeRpm)), 0.0F, 1.0F);
        }

        // CRITICAL FIX: Clamp volume to a minimum of 0.001F so it is completely silent to the player,
        // but Minecraft keeps the layer alive in the background during crossfades.
        this.volume = Math.max(0.001F, targetVolume);

        // 2. Dynamic Pitch Scaling based on RPM
        float rpmRatio = Mth.clamp((rpm - minFadeRpm) / (maxFadeRpm - minFadeRpm), 0.0F, 1.0F);
        float baseCalculatedPitch = Mth.lerp(rpmRatio, minPitch, maxPitch);

        // 3. Add Speed-Based Pitch Modulation
        float speedRatio = 0.0F;
        if (this.engine.getMaxSpeed() > 0.0F) {
            speedRatio = Mth.clamp(Math.abs(this.engine.getCurrentSpeed()) / this.engine.getMaxSpeed(), 0.0F, 1.0F);
        }

        this.pitch = baseCalculatedPitch + (speedRatio * this.speedPitchMod);
    }

    public void stopSound() {
        this.stop();
    }
}
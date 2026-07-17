package com.fennek.carworks.content.blocks.engines;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public interface CACWEngine {
    IFluidHandler getTank();

    boolean isEngineRunning();

    float getCurrentRPM();

    // Added for speed-based audio pitch modulation
    float getCurrentSpeed();

    float getMaxSpeed();
}
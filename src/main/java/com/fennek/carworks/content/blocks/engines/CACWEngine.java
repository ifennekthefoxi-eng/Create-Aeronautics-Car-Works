package com.fennek.carworks.content.blocks.engines;

import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public interface CACWEngine {
    FluidTank getTank();

    int getAnalogSignal();
}

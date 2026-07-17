package com.fennek.carworks.content.blocks.engines.structure;

public record Gear(
        String GEAR_DISPLAY,
        float GEAR_RATIO,
        float GEAR_REV_RATIO,
        float MIN_RPM_NEEDED_FOR_SHIFT_UP_WITHOUT_PENALTY,
        float MAX_RPM_NEEDED_FOR_SHIFT_DOWN_WITHOUT_PENALTY,
        boolean SHOULD_PENALIZE_ON_SHIFT_UP,
        boolean SHOULD_PENALIZE_ON_SHIFT_DOWN
) {
}

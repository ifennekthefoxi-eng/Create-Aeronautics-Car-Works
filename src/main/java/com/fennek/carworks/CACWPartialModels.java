package com.fennek.carworks;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

public class CACWPartialModels {
    public static final PartialModel FOUR_LINE_ENGINE_RADIATOR_FAN = model("block/four_line_engine/partial_models/fan");

    public CACWPartialModels() {
    }

    public static PartialModel model(String id) {
        return PartialModel.of(CreateAeronauticsCarWorks.rl(id));
    }

    public static void init() {
    }
}

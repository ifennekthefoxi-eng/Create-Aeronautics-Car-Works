package com.fennek.carworks;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

public class CACWPartialModels {
    //engines
    public static final PartialModel FOUR_LINE_ENGINE_RADIATOR_FAN = model("block/four_line_engine/partial_models/fan");

    //steering wheel
    public static final PartialModel STEERING_WHEEL = model("block/steering_wheel/partial_models/wheel");
    public static final PartialModel IGNITION_KEY = model("block/steering_wheel/partial_models/ignition_key");
    public static final PartialModel SPEED_NEEDLE = model("block/steering_wheel/partial_models/speed_needle");
    public static final PartialModel GAS_NEEDLE = model("block/steering_wheel/partial_models/gas_needle");

    public CACWPartialModels() {
    }

    public static PartialModel model(String id) {
        return PartialModel.of(CreateAeronauticsCarWorks.rl(id));
    }

    public static void init() {
    }
}

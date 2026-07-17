package com.fennek.carworks.content.items.linkertool;

import com.fennek.carworks.content.items.linkertool.linkingbehaviours.LinkBrakeWheels;
import com.fennek.carworks.content.items.linkertool.linkingbehaviours.LinkEngineToSteeringWheel;
import com.fennek.carworks.content.items.linkertool.linkingbehaviours.LinkSteeringWheels;
import com.fennek.carworks.content.items.linkertool.linkingbehaviours.LinkTankToEngine;

import java.util.function.Supplier;

public enum LinkingBehaviours {

    NONE(null,"stand by"),

    LINK_ENGINE_TO_STEERING_WHEEL(LinkEngineToSteeringWheel::new, "engine -> steering wheel"),
    LINK_GAS_TANK_TO_ENGINE(LinkTankToEngine::new, "gas tank -> engine"),
    LINK_STEER_WHEEL_TO_STEERING_WHEEL(LinkSteeringWheels::new, "steering wheel -> steering wheel"),
    LINK_BRAKE_WHEEL_TO_STEERING_WHEEL(LinkBrakeWheels::new, "brake wheel -> steering wheel"),;

    private final Supplier<LinkingBehvioursInerface> factory;
    private final String Name;

    LinkingBehaviours(Supplier<LinkingBehvioursInerface> factory,String name) {
        this.factory = factory;
        this.Name = name;
    }

    public String getDisplayName() {
        return this.Name;
    }

    public LinkingBehvioursInerface create() {
        return factory == null ? null : factory.get();
    }
}
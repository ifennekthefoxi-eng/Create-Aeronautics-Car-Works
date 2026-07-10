package com.fennek.carworks.utility;

import static com.fennek.carworks.CreateAeronauticsCarWorks.REGISTRATE;

public class CACWLangRegister {
    public static void registerLang() {
        //key Binds
        REGISTRATE.addRawLang("category.carworks.controls", "Carworks KeyBindings");
        REGISTRATE.addRawLang("key.carworks.throttle", "Throttle");
        REGISTRATE.addRawLang("key.carworks.brake", "Brake");
        REGISTRATE.addRawLang("key.carworks.left_turn", "Steer Left");
        REGISTRATE.addRawLang("key.carworks.right_turn", "Steer Right");
        REGISTRATE.addRawLang("key.carworks.ignition", "Ignition");
        REGISTRATE.addRawLang("key.carworks.shift", "Handbrake");
    }
}

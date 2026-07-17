package com.fennek.carworks.utility;

import static com.fennek.carworks.CreateAeronauticsCarWorks.REGISTRATE;

import com.fennek.carworks.CreateAeronauticsCarWorks;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;

public class CACWLangRegister {
    public static void registerLang() {
        //key Binds
        REGISTRATE.addRawLang("category.carworks.controls", "Carworks KeyBindings");
        REGISTRATE.addRawLang("key.carworks.throttle", "Throttle");
        REGISTRATE.addRawLang("key.carworks.brake", "Brake");
        REGISTRATE.addRawLang("key.carworks.left_turn", "Steer Left");
        REGISTRATE.addRawLang("key.carworks.right_turn", "Steer Right");
        REGISTRATE.addRawLang("key.carworks.ignition", "Ignition");
        REGISTRATE.addRawLang("key.carworks.handbrake","Hand Brake");
        REGISTRATE.addRawLang("key.carworks.gear_up","Gear Up");
        REGISTRATE.addRawLang("key.carworks.gear_down","Gar Down");
        REGISTRATE.addRawLang("key.carworks.horn", "Horn");

        //simulated section
        REGISTRATE.addRawLang("simulated.simulated_section.carworks_tab", "carworks");

        //tooptips
        //items
        REGISTRATE.addRawLang("tooltip.carworks.four_line_engine", "CACW IS ON BETA Four Line Engine may change");
        REGISTRATE.addRawLang("tooltip.carworks.steering_wheel", "CACW IS ON BETA Steering Wheel may change");
        REGISTRATE.addRawLang("tooltip.carworks.linker_tool", "CACW IS ON BETA Linker Tool may change");
    }
}

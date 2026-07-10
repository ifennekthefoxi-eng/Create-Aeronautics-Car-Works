package com.fennek.carworks.utility;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.simibubi.create.AllKeys;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class CACWControls {
    public static final KeyMapping Throttle;
    public static final KeyMapping Brakes;
    public static final KeyMapping Steer_Left;
    public static final KeyMapping Steer_Right;
    public static final KeyMapping Ignition;
    public static final KeyMapping CUSTOM_SHIFT;
    private static List<KeyMapping> standardControls;

    public CACWControls() {
    }

    public static List<KeyMapping> getControls() {
        if (standardControls == null) {
            standardControls = new ArrayList(6);
            standardControls.add(Throttle);
            standardControls.add(Brakes);
            standardControls.add(Steer_Left);
            standardControls.add(Steer_Right);
            standardControls.add(Ignition);
            standardControls.add(CUSTOM_SHIFT);
        }

        return standardControls;
    }

    public static boolean isActuallyPressed(KeyMapping kb) {
        InputConstants.Key key = kb.getKey();
        if (key.getValue() == -1) {
            return false;
        } else {
            return key.getType() == Type.MOUSE ? AllKeys.isMouseButtonDown(key.getValue()) : AllKeys.isKeyDown(key.getValue());
        }
    }

    static {
        Throttle = new KeyMapping("key.carworks.throttle", Type.KEYSYM, GLFW.GLFW_KEY_W, "category.carworks.controls");
        Brakes = new KeyMapping("key.carworks.brake", Type.KEYSYM, GLFW.GLFW_KEY_S, "category.carworks.controls");
        Steer_Left = new KeyMapping("key.carworks.left_turn", Type.KEYSYM, GLFW.GLFW_KEY_A, "category.carworks.controls");
        Steer_Right = new KeyMapping("key.carworks.right_turn", Type.KEYSYM, GLFW.GLFW_KEY_D, "category.carworks.controls");
        Ignition = new KeyMapping("key.carworks.ignition", Type.KEYSYM, GLFW.GLFW_KEY_R, "category.carworks.controls");
        CUSTOM_SHIFT = new KeyMapping("key.carworks.horn", Type.KEYSYM, GLFW.GLFW_KEY_H, "category.carworks.controls");
    }
}

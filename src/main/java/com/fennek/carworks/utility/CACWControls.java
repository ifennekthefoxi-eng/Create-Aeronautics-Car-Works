package com.fennek.carworks.utility;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.simibubi.create.AllKeys;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;

public class CACWControls {
    public static final KeyMapping CUSTOM_UP;
    public static final KeyMapping CUSTOM_DOWN;
    public static final KeyMapping CUSTOM_LEFT;
    public static final KeyMapping CUSTOM_RIGHT;
    public static final KeyMapping CUSTOM_JUMP;
    public static final KeyMapping CUSTOM_SHIFT;
    private static List<KeyMapping> standardControls;

    public CACWControls() {
    }

    public static List<KeyMapping> getControls() {
        if (standardControls == null) {
            standardControls = new ArrayList(6);
            standardControls.add(CUSTOM_UP);
            standardControls.add(CUSTOM_DOWN);
            standardControls.add(CUSTOM_LEFT);
            standardControls.add(CUSTOM_RIGHT);
            standardControls.add(CUSTOM_JUMP);
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
        CUSTOM_UP = new KeyMapping("key.carworks.throttle", Type.KEYSYM, 87, "category.carworks.controls");
        CUSTOM_DOWN = new KeyMapping("key.carworks.brake", Type.KEYSYM, 83, "category.carworks.controls");
        CUSTOM_LEFT = new KeyMapping("key.carworks.left_turn", Type.KEYSYM, 65, "category.carworks.controls");
        CUSTOM_RIGHT = new KeyMapping("key.carworks.right_turn", Type.KEYSYM, 68, "category.carworks.controls");
        CUSTOM_JUMP = new KeyMapping("key.carworks.ignition", Type.KEYSYM, 82, "category.carworks.controls");
        CUSTOM_SHIFT = new KeyMapping("key.carworks.shift", Type.KEYSYM, 344, "category.carworks.controls");
    }
}

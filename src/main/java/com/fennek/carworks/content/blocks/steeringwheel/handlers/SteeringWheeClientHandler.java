package com.fennek.carworks.content.blocks.steeringwheel.handlers;

import com.fennek.carworks.CACWBocks;
import com.fennek.carworks.content.blocks.steeringwheel.packets.SteeringWheelInputPacket;
import com.fennek.carworks.content.blocks.steeringwheel.packets.SteeringWheelStopUsingPacket;
import com.fennek.carworks.utility.CACWControls;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllSoundEvents;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class SteeringWheeClientHandler {

    public static Mode MODE = Mode.IDLE;
    public static int PACKET_RATE = 5;
    public static Collection<Integer> currentlyPressed = new HashSet<>();
    private static BlockPos steeringWheelPos;
    private static int packetCooldown;
    private static Player chat;

    public static Mode getMode() {
        return MODE;
    }

    public static void activate(Player player, BlockPos steeringWheelAt) {
        if (getMode() == Mode.IDLE) {
            MODE = Mode.ACTIVE;
            steeringWheelPos = steeringWheelAt;
            chat = player;
            chat.displayClientMessage(Component.literal("block activated"), false);
        }
    }

    public static void deActivate() {
        if (getMode() == Mode.ACTIVE) {
            MODE = Mode.IDLE;
            onReset();
            chat.displayClientMessage(Component.literal("block de-activated"), false);
        }
    }

    protected static void onReset() {
        CACWControls.getControls()
                .forEach(kb -> kb.setDown(CACWControls.isActuallyPressed(kb)));

        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            List<KeyMapping> vanillaMovementKeys = List.of(
                    mc.options.keyUp, mc.options.keyDown,
                    mc.options.keyLeft, mc.options.keyRight,
                    mc.options.keyJump, mc.options.keyShift
            );
            vanillaMovementKeys.forEach(kb -> {
                InputConstants.Key key = kb.getKey();
                if (key.getValue() != -1) {
                    boolean isPressed = key.getType() == InputConstants.Type.MOUSE
                            ? AllKeys.isMouseButtonDown(key.getValue())
                            : AllKeys.isKeyDown(key.getValue());
                    kb.setDown(isPressed);
                }
            });
        }

        packetCooldown = 0;

        if (inSteeringWheel())
            CatnipServices.NETWORK.sendToServer(new SteeringWheelStopUsingPacket(steeringWheelPos));
        steeringWheelPos = null;

        if (!currentlyPressed.isEmpty())
            CatnipServices.NETWORK.sendToServer(new SteeringWheelInputPacket(currentlyPressed, false, null));
        currentlyPressed.clear();

        chat.displayClientMessage(Component.literal("reseted properly"), false);
    }

    public static boolean inSteeringWheel() {
        return steeringWheelPos != null;
    }

    public static void tick() {
        if (MODE == Mode.IDLE)
            return;
        if (packetCooldown > 0)
            packetCooldown--;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player.isSpectator()) {
            MODE = Mode.IDLE;
            onReset();
            return;
        }

        if (inSteeringWheel() && CACWBocks.STEERING_WHEEL.get()
                .getBlockEntityOptional(mc.level, steeringWheelPos)
                .map(be -> !be.isUsedBy(mc.player))
                .orElse(true)) {
            deActivate();
            return;
        }

        if (mc.screen != null) {
            MODE = Mode.IDLE;
            onReset();
            return;
        }

        if (InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_ESCAPE) || InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            MODE = Mode.IDLE;
            onReset();
            return;
        }

        List<KeyMapping> controls = CACWControls.getControls();
        Collection<Integer> pressedKeys = new HashSet<>();
        for (int i = 0; i < controls.size(); i++)
            if (CACWControls.isActuallyPressed(controls.get(i)))
                pressedKeys.add(i);

        Collection<Integer> newKeys = new HashSet<>(pressedKeys);
        Collection<Integer> releasedKeys = currentlyPressed;
        newKeys.removeAll(releasedKeys);
        releasedKeys.removeAll(pressedKeys);

        if (MODE == Mode.ACTIVE) {
            if (!releasedKeys.isEmpty()) {
                CatnipServices.NETWORK.sendToServer(new SteeringWheelInputPacket(releasedKeys, false, steeringWheelPos));
                AllSoundEvents.CONTROLLER_CLICK.playAt(player.level(), player.blockPosition(), 1f, .5f, true);
            }

            if (!newKeys.isEmpty()) {
                CatnipServices.NETWORK.sendToServer(new SteeringWheelInputPacket(newKeys, true, steeringWheelPos));
                packetCooldown = PACKET_RATE;
                AllSoundEvents.CONTROLLER_CLICK.playAt(player.level(), player.blockPosition(), 1f, .75f, true);
            }

            // was: if (packetCooldown == 0 && !pressedKeys.isEmpty()) { ... }
            if (packetCooldown == 0 && !pressedKeys.isEmpty()) {
                Collection<Integer> resendKeys = new HashSet<>(pressedKeys);
                resendKeys.remove(CACWControls.getControls().indexOf(CACWControls.Ignition));
                if (!resendKeys.isEmpty()) {
                    CatnipServices.NETWORK.sendToServer(new SteeringWheelInputPacket(resendKeys, true, steeringWheelPos));
                    packetCooldown = PACKET_RATE;
                }
            }
        }

        currentlyPressed = pressedKeys;
        controls.forEach(kb -> kb.setDown(false));

        net.minecraft.client.Options vanillaOptions = mc.options;
        vanillaOptions.keyUp.setDown(false);
        vanillaOptions.keyDown.setDown(false);
        vanillaOptions.keyLeft.setDown(false);
        vanillaOptions.keyRight.setDown(false);
        vanillaOptions.keyJump.setDown(false);
        vanillaOptions.keyShift.setDown(false);
    }

    public enum Mode {
        IDLE, ACTIVE
    }
}
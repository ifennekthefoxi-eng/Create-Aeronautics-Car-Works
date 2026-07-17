package com.fennek.carworks.content.items.linkertool.linkingbehaviours;

import com.fennek.carworks.content.blocks.SmartWheelMount.SmartWheelMountBlockEntity;
import com.fennek.carworks.content.blocks.steeringwheel.SteeringWheelBlockEntity;
import com.fennek.carworks.content.items.linkertool.LinkingBehvioursInerface;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class LinkSteeringWheels  implements LinkingBehvioursInerface {
    private BlockPos SteerWheelPos;
    private BlockPos steeringWheelPos;

    public LinkSteeringWheels() {
        SteerWheelPos = null;
        steeringWheelPos = null;
    }

    @Override
    public boolean checkFirst() {
        return SteerWheelPos != null;
    }

    @Override
    public void SelectFirst(BlockPos Pos, Level level) {
        if (level.getBlockEntity(Pos) instanceof SmartWheelMountBlockEntity) {
            SteerWheelPos = Pos;
        }
    }

    @Override
    public boolean checkSecond() {
        return steeringWheelPos != null;
    }

    @Override
    public void SelectSecond(BlockPos Pos, Level level) {
        if (level.getBlockEntity(Pos) instanceof SteeringWheelBlockEntity) {
            steeringWheelPos = Pos;
        }
    }

    @Override
    public void link(Level level) {
        if (level.getBlockEntity(this.steeringWheelPos) instanceof SteeringWheelBlockEntity steeringWheel) {
            steeringWheel.linkSteerWheels(this.SteerWheelPos, null);
        }
    }
}

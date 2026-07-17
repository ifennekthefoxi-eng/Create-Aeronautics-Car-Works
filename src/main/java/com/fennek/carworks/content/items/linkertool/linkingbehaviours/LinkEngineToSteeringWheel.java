package com.fennek.carworks.content.items.linkertool.linkingbehaviours;

import com.fennek.carworks.content.blocks.engines.FourLineEngine.FourLineEngineBlockEntity;
import com.fennek.carworks.content.blocks.steeringwheel.SteeringWheelBlockEntity;
import com.fennek.carworks.content.items.linkertool.LinkingBehvioursInerface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class LinkEngineToSteeringWheel implements LinkingBehvioursInerface {

    private BlockPos enginePos;
    private BlockPos steeringWheelPos;

    public LinkEngineToSteeringWheel() {
        enginePos = null;
        steeringWheelPos = null;
    }

    @Override
    public boolean checkFirst(){
        return enginePos != null;
    }

    @Override
    public void SelectFirst(BlockPos Pos,Level level){
        if (level.getBlockEntity(Pos) instanceof FourLineEngineBlockEntity) {
            enginePos = Pos;
        }
    }

    @Override
    public boolean checkSecond(){
        return steeringWheelPos != null;
    }

    @Override
    public void SelectSecond(BlockPos Pos,Level level){
        if (level.getBlockEntity(Pos) instanceof SteeringWheelBlockEntity) {
            steeringWheelPos = Pos;
        }
    }

    @Override
    public void link(Level level){
        if(checkFirst() && checkSecond()){
            if (level.getBlockEntity(this.steeringWheelPos) instanceof SteeringWheelBlockEntity wheel) {
                wheel.linkEngine(this.enginePos, null);
            }
        }
    }
}
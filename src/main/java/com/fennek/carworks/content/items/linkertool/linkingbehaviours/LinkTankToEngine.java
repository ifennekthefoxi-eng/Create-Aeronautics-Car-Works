package com.fennek.carworks.content.items.linkertool.linkingbehaviours;

import com.fennek.carworks.content.blocks.engines.FourLineEngine.FourLineEngineBlockEntity;
import com.fennek.carworks.content.items.linkertool.LinkingBehvioursInerface;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class LinkTankToEngine implements LinkingBehvioursInerface {

    private BlockPos tankPos;
    private BlockPos enginePos;

    public LinkTankToEngine() {
        tankPos = null;
        enginePos = null;
    }

    @Override
    public boolean checkFirst() {
        return tankPos != null;
    }

    @Override
    public void SelectFirst(BlockPos Pos, Level level) {
        if (level.getBlockEntity(Pos) instanceof FluidTankBlockEntity) {
            tankPos = Pos;
        }
    }

    @Override
    public boolean checkSecond() {
        return enginePos != null;
    }

    @Override
    public void SelectSecond(BlockPos Pos, Level level) {
        if (level.getBlockEntity(Pos) instanceof FourLineEngineBlockEntity) {
            enginePos = Pos;
        }
    }

    @Override
    public void link(Level level) {
        if(checkFirst() && checkSecond()){
            if (level.getBlockEntity(this.enginePos) instanceof FourLineEngineBlockEntity engine) {
                engine.linkTank(this.tankPos);
            }
        }
    }
}

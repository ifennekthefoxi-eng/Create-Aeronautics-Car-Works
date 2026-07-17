package com.fennek.carworks.content.items.linkertool;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface LinkingBehvioursInerface {
    boolean checkFirst();
    void SelectFirst(BlockPos Pos,Level level);
    boolean checkSecond();
    void SelectSecond(BlockPos Pos,Level level);
    void link(Level level);
}

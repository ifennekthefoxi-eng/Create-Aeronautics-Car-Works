package com.fennek.carworks.content.blocks.SmartWheelMount;

import com.fennek.carworks.CACWBlockEntityTypes;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlock;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity;
import dev.ryanhcode.offroad.index.OffroadBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SmartWheelMountBlock extends WheelMountBlock{
    public SmartWheelMountBlock(final Properties properties) {
        super(properties);
    }

    @SuppressWarnings("unchecked") // Suppress the warning because you know it's safe
    @Override
    public Class<WheelMountBlockEntity> getBlockEntityClass() {
        return (Class<WheelMountBlockEntity>) (Class<?>) SmartWheelMountBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends WheelMountBlockEntity> getBlockEntityType() {
        return CACWBlockEntityTypes.SMART_WHEEL_MOUNT.get();
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
        if (state.hasBlockEntity() && state.getBlock() != newState.getBlock()) {
            // Cast 'be' to your specific class here
            withBlockEntityDo(level, pos, be -> {
                if (be instanceof SmartWheelMountBlockEntity smartBe) {
                    smartBe.unlinkFromSteeringwheel(pos, null);
                }
            });
        }
    }
}

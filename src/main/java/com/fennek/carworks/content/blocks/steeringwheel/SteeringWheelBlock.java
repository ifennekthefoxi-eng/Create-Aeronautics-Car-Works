package com.fennek.carworks.content.blocks.steeringwheel;

import com.fennek.carworks.CACWBlockEntityTypes;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
public class SteeringWheelBlock extends HorizontalDirectionalBlock implements IBE<SteeringWheelBlockEntity> {


    public static final MapCodec<SteeringWheelBlock> CODEC = simpleCodec(SteeringWheelBlock::new);

    public SteeringWheelBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<SteeringWheelBlockEntity> getBlockEntityClass() {
        return SteeringWheelBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SteeringWheelBlockEntity> getBlockEntityType() {
        return CACWBlockEntityTypes.STEERING_WHEEL.get();
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!player.isShiftKeyDown() && SteeringWheelBlockEntity.playerInRange(player, level, pos)) {
            if (!level.isClientSide)
                withBlockEntityDo(level, pos, be -> be.tryStartUsing(player,level));
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown() && SteeringWheelBlockEntity.playerInRange(player, level, pos)) {
            if (level.isClientSide)
                return InteractionResult.SUCCESS;
            withBlockEntityDo(level, pos,
                    be -> player.openMenu(be, be::sendToMenu));
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!world.isClientSide)
                withBlockEntityDo(world, pos, be -> be.stopUsingOnDestroy());

            super.onRemove(state, world, pos, newState, isMoving);
        }
    }
}


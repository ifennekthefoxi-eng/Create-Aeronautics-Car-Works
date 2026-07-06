package com.fennek.carworks.content.blocks.engines.FourLineEngine;

import com.fennek.carworks.CACWBlockEntityTypes;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class FourLineEngineBlock extends HorizontalKineticBlock implements IBE<FourLineEngineBlockEntity>, ProperWaterloggedBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final VoxelShape SHAPE_NS = Shapes.or(
            Block.box(3.0D, 3.0D, 0.0D, 13.0D, 13.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D)
    );
    private static final VoxelShape SHAPE_EW = Shapes.or(
            Block.box(0.0D, 3.0D, 3.0D, 16.0D, 13.0D, 13.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D)
    );

    public FourLineEngineBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(WATERLOGGED, false)
                .setValue(POWERED, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.withWater(super.getStateForPlacement(context), context);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, POWERED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public Class<FourLineEngineBlockEntity> getBlockEntityClass() {
        return FourLineEngineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FourLineEngineBlockEntity> getBlockEntityType() {
        return CACWBlockEntityTypes.FOUR_LINE_ENGINE.get();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(HORIZONTAL_FACING);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos otherPos, boolean moving) {
        boolean powered = level.getBestNeighborSignal(pos) > 0;
        if (state.getValue(POWERED) != powered) {
            level.setBlock(pos, state.setValue(POWERED, powered), 2);
        }

        this.withBlockEntityDo(level, pos, (be) -> be.setAnalogSignal(level.getBestNeighborSignal(pos)));
        super.neighborChanged(state, level, pos, block, otherPos, moving);
    }

    @Override
    public void onPlace(BlockState state, Level worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
        boolean powered = worldIn.getBestNeighborSignal(pos) > 0;
        if (state.getValue(POWERED) != powered) {
            worldIn.setBlock(pos, state.setValue(POWERED, powered), 2);
        }

        if (state.hasBlockEntity()) {
            this.withBlockEntityDo(worldIn, pos, (be) -> be.setAnalogSignal(worldIn.getBestNeighborSignal(pos)));
        }

        super.onPlace(state, worldIn, pos, oldState, isMoving);
    }
}
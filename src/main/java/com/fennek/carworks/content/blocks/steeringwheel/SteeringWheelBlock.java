package com.fennek.carworks.content.blocks.steeringwheel;

import com.fennek.carworks.CACWBlockEntityTypes;
import com.fennek.carworks.CACWItems;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SteeringWheelBlock extends HorizontalDirectionalBlock implements IBE<SteeringWheelBlockEntity> {

    private static final VoxelShape SHAPE_N = Shapes.or(
            Block.box(0, 0, 9, 16, 18.5, 16)
    );
    private static final VoxelShape SHAPE_S = Shapes.or(
            Block.box(0, 0, 0, 16, 18.5, 7)
    );
    private static final VoxelShape SHAPE_W = Shapes.or(
            Block.box(9, 0, 0, 16, 18.5, 16)
    );
    private static final VoxelShape SHAPE_E = Shapes.or(
            Block.box(0, 0, 0, 7, 18.5, 16)
    );

    public static final MapCodec<SteeringWheelBlock> CODEC = simpleCodec(SteeringWheelBlock::new);

    public SteeringWheelBlock(Properties properties) {
        super(properties);
        // Set the default facing to North (or whichever direction you prefer)
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        VoxelShape shape = Shapes.empty();

        if (facing == Direction.NORTH) {
            shape = SHAPE_N;
        }
        else if (facing == Direction.SOUTH)
        {
            shape = SHAPE_S;
        }
        else if (facing == Direction.WEST)
        {
            shape = SHAPE_W;
        }
        else if (facing == Direction.EAST)
        {
            shape = SHAPE_E;
        }
        return shape;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // context.getHorizontalDirection().getOpposite() makes the front of the block face the player
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        ItemStack mainHandItem = player.getMainHandItem();

        // Correct way to check if the item is NOT the Linker Tool
        if (!mainHandItem.is(CACWItems.LINKER_TOOL.get())) {

            if (!level.isClientSide && !SteeringWheelBlockEntity.playerInRange(player, level, pos)) {
                player.displayClientMessage(Component.literal("not in reach"), false);
                return InteractionResult.SUCCESS; // Good practice to return here so the rest of the code doesn't fire
            }

            if (!player.isShiftKeyDown() && SteeringWheelBlockEntity.playerInRange(player, level, pos)) {
                if (!level.isClientSide)
                    withBlockEntityDo(level, pos, be -> be.tryStartUsing(player, level));
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

        // If the item IS the Linker Tool, pass so the Linker Tool can do its own thing
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!world.isClientSide)
            {
                withBlockEntityDo(world, pos, be -> be.unlinkEngine());
                withBlockEntityDo(world, pos, be -> be.stopUsingOnDestroy());
            }

            super.onRemove(state, world, pos, newState, isMoving);
        }
    }
}


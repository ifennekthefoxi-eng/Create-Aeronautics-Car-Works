package com.fennek.carworks.content.blocks.steeringwheel;

import com.fennek.carworks.CACWBlockEntityTypes;
import com.fennek.carworks.CACWItems;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SteeringWheelBlock extends HorizontalDirectionalBlock implements IBE<SteeringWheelBlockEntity> {

    private static final VoxelShape SHAPE_N = Shapes.or(Block.box(0, 0, 9, 16, 18.5, 16));
    private static final VoxelShape SHAPE_S = Shapes.or(Block.box(0, 0, 0, 16, 18.5, 7));
    private static final VoxelShape SHAPE_W = Shapes.or(Block.box(9, 0, 0, 16, 18.5, 16));
    private static final VoxelShape SHAPE_E = Shapes.or(Block.box(0, 0, 0, 7, 18.5, 16));

    public static final MapCodec<SteeringWheelBlock> CODEC = simpleCodec(SteeringWheelBlock::new);

    public SteeringWheelBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        if (facing == Direction.NORTH) return SHAPE_N;
        if (facing == Direction.SOUTH) return SHAPE_S;
        if (facing == Direction.WEST) return SHAPE_W;
        if (facing == Direction.EAST) return SHAPE_E;
        return Shapes.empty();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        // 1. If holding the linker tool, immediately pass so the tool can do its job.
        if (stack.is(CACWItems.LINKER_TOOL.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // 3. For all other items, trigger the steering wheel driving logic
        InteractionResult result = this.handleWheelInteraction(level, pos, player);
        return result.consumesAction() ? ItemInteractionResult.sidedSuccess(level.isClientSide) : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // --- NEW: Handle clicks WITHOUT an item ---
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        // Hard failsafe: If the linker tool is in EITHER hand, do not activate the wheel.
        if (player.getMainHandItem().is(CACWItems.LINKER_TOOL.get()) || player.getOffhandItem().is(CACWItems.LINKER_TOOL.get())) {
            return InteractionResult.PASS;
        }

        return this.handleWheelInteraction(level, pos, player);
    }

    // --- Centralized logic ---
    private InteractionResult handleWheelInteraction(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && !SteeringWheelBlockEntity.playerInRange(player, level, pos)) {
            player.displayClientMessage(Component.literal("not in reach"), false);
            return InteractionResult.SUCCESS;
        }

        if (!player.isShiftKeyDown() && SteeringWheelBlockEntity.playerInRange(player, level, pos)) {
            if (!level.isClientSide)
                withBlockEntityDo(level, pos, be -> be.tryStartUsing(player, level));
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown() && SteeringWheelBlockEntity.playerInRange(player, level, pos)) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            withBlockEntityDo(level, pos, be -> player.openMenu(be, be::sendToMenu));
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!world.isClientSide) {
                withBlockEntityDo(world, pos, be -> {
                    be.stopDisc();
                    be.unlinkEngine();
                    be.stopUsingOnDestroy();
                    ItemHelper.dropContents(world, pos, be.inventory);
                });
                world.removeBlockEntity(pos);
            }
            super.onRemove(state, world, pos, newState, isMoving);
        }
    }
}
/*package com.fennek.carworks.content.blocks.steeringwheel;

import com.fennek.carworks.CACWBlockEntityTypes;
import com.fennek.carworks.CACWItems;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.item.ItemHelper;
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

        if(mainHandItem.isEmpty()) {
            if(mainHandItem.is(CACWItems.LINKER_TOOL.get())){
                return InteractionResult.PASS;
            }
        }

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

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!world.isClientSide)
            {
                withBlockEntityDo(world, pos, be -> be.stopDisc());
                withBlockEntityDo(world, pos, be -> be.unlinkEngine());
                withBlockEntityDo(world, pos, be -> be.stopUsingOnDestroy());
                withBlockEntityDo(world, pos, be -> ItemHelper.dropContents(world, pos, be.inventory));
                world.removeBlockEntity(pos);
            }

            super.onRemove(state, world, pos, newState, isMoving);
        }
    }
}

*/
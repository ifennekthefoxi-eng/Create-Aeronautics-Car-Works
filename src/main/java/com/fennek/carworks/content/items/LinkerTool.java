package com.fennek.carworks.content.items;

import com.fennek.carworks.content.blocks.engines.FourLineEngine.FourLineEngineBlockEntity;
import com.fennek.carworks.content.blocks.steeringwheel.SteeringWheelBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

//@EventBusSubscriber
public class LinkerTool extends Item {

    public LinkerTool(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canAttackBlock(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
        return false;
    }

    // 1. Triggered when clicking a BLOCK
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();

        if (level.isClientSide() || player == null) {
            return InteractionResult.SUCCESS;
        }

        // Shift-Right-Click on a block to reset
        if (player.isShiftKeyDown()) {
            resetTool(stack, player);
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);


        // 2. Select Steering Wheel
        if (blockEntity instanceof SteeringWheelBlockEntity) {
            // NEW 1.21+ WAY: Get custom data, copy it, modify it, and set it back
            CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = customData.copyTag();

            tag.putInt("Wheel_X", pos.getX());
            tag.putInt("Wheel_Y", pos.getY());
            tag.putInt("Wheel_Z", pos.getZ());

            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            player.sendSystemMessage(Component.literal("Selected a steering wheel"));
            return InteractionResult.SUCCESS;
        }
        else if (blockEntity instanceof FluidTankBlockEntity) {
            CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = customData.copyTag();

            tag.putInt("Tank_X", pos.getX());
            tag.putInt("Tank_Y", pos.getY());
            tag.putInt("Tank_Z", pos.getZ());

            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            player.sendSystemMessage(Component.literal("Selected a Fluid Tank"));
            return InteractionResult.SUCCESS;
        }
        else if (blockEntity != null) {
            player.sendSystemMessage(Component.literal("this block is not linkable"));
        }

        // 3. Select Engine & Link
        if (blockEntity instanceof FourLineEngineBlockEntity engineBlockEntity) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);

            if (customData != null && customData.contains("Wheel_X")) {
                CompoundTag tag = customData.copyTag();
                BlockPos wheelPos = new BlockPos(tag.getInt("Wheel_X"), tag.getInt("Wheel_Y"), tag.getInt("Wheel_Z"));
                BlockEntity savedEntity = level.getBlockEntity(wheelPos);

                if (savedEntity instanceof SteeringWheelBlockEntity steeringWheelBlockEntity) {
                    player.sendSystemMessage(Component.literal("Selected an engine"));
                    LinkEngine(steeringWheelBlockEntity, engineBlockEntity, player, stack);
                } else {
                    player.sendSystemMessage(Component.literal("Saved steering wheel is missing!"));
                    resetTool(stack, player);
                }
            }
            else if (customData != null && customData.contains("Tank_X"))
            {
                CompoundTag tag = customData.copyTag();
                BlockPos tankPos = new BlockPos(tag.getInt("Tank_X"), tag.getInt("Tank_Y"), tag.getInt("Tank_Z"));
                BlockEntity savedEntity = level.getBlockEntity(tankPos);

                if (savedEntity instanceof FluidTankBlockEntity FluidTankBlockEntity) {
                    player.sendSystemMessage(Component.literal("Selected an engine"));
                    LinkGasTank(FluidTankBlockEntity, engineBlockEntity, player, stack);
                } else {
                    player.sendSystemMessage(Component.literal("Saved gas tank is missing!"));
                    resetTool(stack, player);
                }
            }
            else {
                player.sendSystemMessage(Component.literal("no component to link with this engine"));
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    // Triggered when clicking the AIR
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Shift-Right-Click in the air to reset
        if (!level.isClientSide() && player.isShiftKeyDown()) {
            resetTool(stack, player);
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    // 4. Link Logic
    // Inside LinkerTool.java, replace LinkEngine with this:
    private void LinkEngine(SteeringWheelBlockEntity wheel, FourLineEngineBlockEntity engine, Player player, ItemStack stack) {
        if (engine.hasLinkedWheel()) {
            player.sendSystemMessage(Component.literal("engine already linked!"));
            return;
        } else {
            wheel.linkEngine(engine.getBlockPos(), player);
            player.sendSystemMessage(Component.literal("Successfully linked engine to steering wheel!"));
            // Automatically reset after linking
            resetTool(stack, null);
        }
    }

    private void LinkGasTank(FluidTankBlockEntity gastank, FourLineEngineBlockEntity engine, Player player, ItemStack stack)
    {
        if (engine.hasLinkedTank()) {
            player.sendSystemMessage(Component.literal("engine already have a linked fluid tank!"));
            return;
        } else {
            engine.linkTank(gastank.getBlockPos());
            player.sendSystemMessage(Component.literal("Successfully linked fluid tank to engine!"));
            // Automatically reset after linking
            resetTool(stack, null);
        }
    }

    // 5. Reset Logic (Clears the Components)
    private void resetTool(ItemStack stack, Player player) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);

        if (customData != null && customData.contains("Wheel_X")) {
            CompoundTag tag = customData.copyTag();
            tag.remove("Wheel_X");
            tag.remove("Wheel_Y");
            tag.remove("Wheel_Z");

            // If the tag is empty after we remove our stuff, just delete the component entirely
            if (tag.isEmpty()) {
                stack.remove(DataComponents.CUSTOM_DATA);
            } else {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }

            if (player != null) {
                player.sendSystemMessage(Component.literal("Linker Tool reset."));
            }
        } else if (player != null) {
            player.sendSystemMessage(Component.literal("Linker Tool is already empty."));
        }
    }
}
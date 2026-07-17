package com.fennek.carworks.content.items.linkertool;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LinkerTool extends Item {

    private LinkingBehaviours linkingBehaviour;

    // Per-player in-progress linking sessions. The Item instance is a singleton
    // shared by everyone holding it, so this state can't live on the Item's
    // own fields - it has to be keyed per player.
    private final Map<UUID, LinkingBehvioursInerface> sessions = new HashMap<>();

    public LinkerTool(Properties properties) {
        super(properties);
        linkingBehaviour = LinkingBehaviours.NONE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.carworks.linker_tool").withStyle(ChatFormatting.RED));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    public boolean canAttackBlock(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();

        if (level.isClientSide() || player == null) {
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            resetTool(stack, player);
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity == null) {
            // Not a linkable block - treat this as the "cycle behaviour" gesture
            changeBehaviour();
            player.displayClientMessage(Component.literal("Linking behaviour: " + linkingBehaviour.getDisplayName()),true);
            return InteractionResult.SUCCESS;
        }

        LinkingBehvioursInerface behaviour = getSession(player);

        if (behaviour == null) {
            player.displayClientMessage(Component.literal("no linking behaviour selected"),true);
            return InteractionResult.SUCCESS;
        }

        if (!behaviour.checkFirst()) {
            behaviour.SelectFirst(pos, level);
            if (behaviour.checkFirst()) {
                player.displayClientMessage(Component.literal("First block selected"),true);
            } else {
                player.displayClientMessage(Component.literal("This block can't be used as the first link target"),true);
            }
            return InteractionResult.SUCCESS;
        }

        if (!behaviour.checkSecond()) {
            behaviour.SelectSecond(pos, level);
            if (behaviour.checkSecond()) {
                behaviour.link(level);
                player.displayClientMessage(Component.literal("Linked!"),true);
                sessions.remove(player.getUUID());
            } else {
                player.displayClientMessage(Component.literal("This block can't be used as the second link target"),true);
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            if (player.isShiftKeyDown()) {
                resetTool(stack, player);
            } else {
                // Right-clicking air - treat this as the "cycle behaviour" gesture too
                changeBehaviour();
                player.displayClientMessage(Component.literal("Linking behaviour: " + linkingBehaviour.getDisplayName()),true);
            }
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    /**
     * Cycles the tool to the next linking behaviour and clears everyone's
     * in-progress sessions, since a half-finished selection made under the
     * old behaviour doesn't make sense under the new one.
     */
    void changeBehaviour() {
        LinkingBehaviours[] values = LinkingBehaviours.values();
        int next = (linkingBehaviour.ordinal() + 1) % values.length;
        linkingBehaviour = values[next];
        sessions.clear();
    }

    private void resetTool(ItemStack stack, Player player) {
        sessions.remove(player.getUUID());
        player.displayClientMessage(Component.literal("Linker tool reset"),true);
    }

    /**
     * Gets this player's in-progress behaviour instance, creating a fresh one
     * from the currently selected LinkingBehaviours enum entry if they don't
     * have one yet.
     */
    private LinkingBehvioursInerface getSession(Player player) {
        return sessions.computeIfAbsent(player.getUUID(), id -> linkingBehaviour.create());
    }
}
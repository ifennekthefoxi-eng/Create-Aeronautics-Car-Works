package com.fennek.carworks;

import com.fennek.carworks.content.blocks.engines.FourLineEngine.FourLineEngineBlock;
import com.fennek.carworks.content.blocks.steeringwheel.SteeringWheelBlock;
import com.jesz.createdieselgenerators.content.pumpjack.*;
import com.jesz.createdieselgenerators.contraption.DieselEngineMovementBehaviour;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;

import java.util.List;

import static com.fennek.carworks.CreateAeronauticsCarWorks.REGISTRATE;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

public class CACWBocks {

    public static final BlockEntry<FourLineEngineBlock> FOUR_LINE_ENGINE = REGISTRATE.block("four_line_engine", FourLineEngineBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.mapColor(MapColor.COLOR_YELLOW).noOcclusion()) // Keeping noOcclusion here!
            .transform(pickaxeOnly())
            .blockstate((c, p) ->
                    p.getVariantBuilder(c.getEntry())
                            .forAllStates(bs ->
                                    ConfiguredModel.builder()
                                            .modelFile(AssetLookup.partialBaseModel(c, p)) // Standard base model
                                            .rotationY((int) bs.getValue(FourLineEngineBlock.FACING).toYRot()) // Pure horizontal rotation
                                            .build()
                            )
            )
            .onRegister(movementBehaviour(new DieselEngineMovementBehaviour()))
            .item((block, properties) -> new BlockItem(block, properties) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
                    tooltipComponents.add(Component.translatable("tooltip.carworks.four_line_engine").withStyle(ChatFormatting.RED));
                    super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
                }
            })
            .tag(AllTags.AllItemTags.CONTRAPTION_CONTROLLED.tag)
            .model((c, p) -> p.blockItem(c, "/item"))
            .build()
            .register();

    public static final BlockEntry<SteeringWheelBlock> STEERING_WHEEL = REGISTRATE.block("steering_wheel", SteeringWheelBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.mapColor(MapColor.COLOR_YELLOW).noOcclusion()) // Keeping noOcclusion here!
            .transform(pickaxeOnly())
            .blockstate((c, p) ->
                    p.getVariantBuilder(c.getEntry())
                            .forAllStates(bs ->
                                    ConfiguredModel.builder()
                                            .modelFile(AssetLookup.partialBaseModel(c, p))
                                            .rotationY((int) bs.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot())
                                            .build()
                            )
            )
            /*.item()
            .tag(AllTags.AllItemTags.CONTRAPTION_CONTROLLED.tag)
            // FIX: Explicitly point the item model to the nested block model path
            .model((c, p) -> p.withExistingParent(c.getName(), p.modLoc("block/steering_wheel/block")))
            .build()
            .register();*/
            .item((block, properties) -> new BlockItem(block, properties) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
                    tooltipComponents.add(Component.translatable("tooltip.carworks.steering_wheel").withStyle(ChatFormatting.RED));
                    super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
                }
            })
            .tag(AllTags.AllItemTags.CONTRAPTION_CONTROLLED.tag)
            .model((c, p) -> p.blockItem(c, "/item"))
            .build()
            .register();



    public static void register() {
    }

    private static NonNullConsumer<? super Block> movementBehaviour(MovementBehaviour movementBehaviour) {
        return b -> MovementBehaviour.REGISTRY.register(b, movementBehaviour);
    }
}
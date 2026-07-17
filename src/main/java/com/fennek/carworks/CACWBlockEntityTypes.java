package com.fennek.carworks;

import com.fennek.carworks.content.blocks.engines.FourLineEngine.FourLineEngineBlockEntity;
import com.fennek.carworks.content.blocks.engines.FourLineEngine.FourLineEngineRenderer;
import com.fennek.carworks.content.blocks.steeringwheel.SteeringWheelBlockEntity;
import com.fennek.carworks.content.blocks.steeringwheel.SteeringWheelRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import dev.ryanhcode.offroad.Offroad;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;

import static com.fennek.carworks.CreateAeronauticsCarWorks.REGISTRATE;

public class CACWBlockEntityTypes{

    public static final BlockEntityEntry<FourLineEngineBlockEntity> FOUR_LINE_ENGINE = REGISTRATE
            .blockEntity("four_line_tile_entity", FourLineEngineBlockEntity::new)
            //.visual(() -> OrientedRotatingVisual.of(AllPartialModels.SHAFT_HALF), false)
            .validBlocks(CACWBocks.FOUR_LINE_ENGINE)
            .renderer(() -> FourLineEngineRenderer::new)
            .register();

    public static final BlockEntityEntry<SteeringWheelBlockEntity> STEERING_WHEEL = REGISTRATE
            .blockEntity("steering_wheel_tile_entity", SteeringWheelBlockEntity::new)
            //.visual(() -> OrientedRotatingVisual.of(AllPartialModels.SHAFT_HALF), false)
            .validBlocks(CACWBocks.STEERING_WHEEL)
            .renderer(() -> SteeringWheelRenderer::new)
            .register();

    public static void register() {
    }
}

/*{
    public static final BlockEntityEntry<FourLineEngineBlockEntity> FOUR_LINE_ENGINE;
    public static final BlockEntityEntry<SteeringWheelBlockEntity> STEERING_WHEEL;

    public CACWBlockEntityTypes() {
    }

    public static void register() {
    }

    static {
        FOUR_LINE_ENGINE = CreateAeronauticsCarWorks.REGISTRATE.blockEntity("four_line_tile_entity", FourLineEngineBlockEntity::new).validBlocks(new NonNullSupplier[]{CACWBocks.FOUR_LINE_ENGINE}).renderer(() -> FourLineEngineRenderer::new).register();
        STEERING_WHEEL = CreateAeronauticsCarWorks.REGISTRATE.blockEntity("steering_wheel_tile_entity", SteeringWheelBlockEntity::new).validBlocks(new NonNullSupplier[]{CACWBocks.STEERING_WHEEL}).register();
    }
}*/

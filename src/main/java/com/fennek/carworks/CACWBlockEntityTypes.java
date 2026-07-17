package com.fennek.carworks;

import com.fennek.carworks.content.blocks.SmartWheelMount.SmartWheelMountBlockEntity;
import com.fennek.carworks.content.blocks.engines.FourLineEngine.FourLineEngineBlockEntity;
import com.fennek.carworks.content.blocks.engines.FourLineEngine.FourLineEngineRenderer;
import com.fennek.carworks.content.blocks.steeringwheel.SteeringWheelBlockEntity;
import com.fennek.carworks.content.blocks.steeringwheel.SteeringWheelRenderer;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import dev.ryanhcode.offroad.Offroad;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;

import static com.fennek.carworks.CreateAeronauticsCarWorks.REGISTRATE;

public class CACWBlockEntityTypes{

    public static final BlockEntityEntry<FourLineEngineBlockEntity> FOUR_LINE_ENGINE = REGISTRATE
            .blockEntity("four_line_tile_entity", FourLineEngineBlockEntity::new)
            .validBlocks(CACWBocks.FOUR_LINE_ENGINE)
            .renderer(() -> FourLineEngineRenderer::new)
            .register();

    public static final BlockEntityEntry<SteeringWheelBlockEntity> STEERING_WHEEL = REGISTRATE
            .blockEntity("steering_wheel_tile_entity", SteeringWheelBlockEntity::new)
            .validBlocks(CACWBocks.STEERING_WHEEL)
            .renderer(() -> SteeringWheelRenderer::new)
            .register();

    public static final BlockEntityEntry<SmartWheelMountBlockEntity> SMART_WHEEL_MOUNT = REGISTRATE
            .blockEntity("smart_wheel_mount_tile_entity", SmartWheelMountBlockEntity::new)
            .validBlocks(CACWBocks.SMART_WHEEL_MOUNT)
            .renderer(() -> WheelMountRenderer::new)
            .register();

    public static void register() {
    }
}

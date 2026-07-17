package com.fennek.carworks.mixins.client;

import com.fennek.carworks.CACWBocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public abstract class SteeringWheelClientMixin {

    //1 = 16
    private static final float STEERING_WHEEL_RADIO_VOLUME = 0.5F;
    @Redirect(
            method = "playJukeboxSong(Lnet/minecraft/core/Holder;Lnet/minecraft/core/BlockPos;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui;setNowPlaying(Lnet/minecraft/network/chat/Component;)V"
            )
    )
    private void carworks$suppressNowPlayingForSteeringWheel(Gui gui, Component component,
                                                             Holder<JukeboxSong> song, BlockPos pos) {
        Level level = Minecraft.getInstance().level;
        if (level != null && level.getBlockState(pos).is(CACWBocks.STEERING_WHEEL.get())) {
            return; // our block: skip the action-bar text entirely
        }
        gui.setNowPlaying(component);
    }

    @Redirect(
            method = "playJukeboxSong(Lnet/minecraft/core/Holder;Lnet/minecraft/core/BlockPos;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;forJukeboxSong(Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;"
            )
    )
    private SimpleSoundInstance carworks$customRangeForSteeringWheel(SoundEvent soundEvent, Vec3 pos,
                                                                     Holder<JukeboxSong> song, BlockPos blockPos) {
        Level level = Minecraft.getInstance().level;
        if (level != null && level.getBlockState(blockPos).is(CACWBocks.STEERING_WHEEL.get())) {
            return new SimpleSoundInstance(soundEvent, SoundSource.RECORDS, STEERING_WHEEL_RADIO_VOLUME, 1.0F,
                    RandomSource.create(), pos.x, pos.y, pos.z);
        }
        return SimpleSoundInstance.forJukeboxSong(soundEvent, pos);
    }
}
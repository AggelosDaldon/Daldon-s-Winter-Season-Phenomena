package com.daldon.winterphenomena;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DaldonWeather.MOD_ID, value = Dist.CLIENT,
    bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientTint {
    private ClientTint() {}

    @SubscribeEvent
    public static void register(RegisterColorHandlersEvent.Block event) {
        event.register((state, world, pos, tintIndex) -> {
            if (tintIndex != 0 || world == null || pos == null) return -1;
            int original = BiomeColors.getAverageGrassColor(world, pos);
            float strength = switch (ClientWeatherState.season) {
                case PREWINTER -> 0.0f;
                case FREEZING -> 0.37f;
                case WINTER -> 0.78f;
            };
            return mix(original, 0x99A49A, strength);
        }, Blocks.GRASS_BLOCK, Blocks.GRASS, Blocks.TALL_GRASS, Blocks.FERN,
            Blocks.LARGE_FERN);
    }

    private static int mix(int original, int cold, float strength) {
        int r = Math.round(((original >> 16) & 255) * (1 - strength) + ((cold >> 16) & 255) * strength);
        int g = Math.round(((original >> 8) & 255) * (1 - strength) + ((cold >> 8) & 255) * strength);
        int b = Math.round((original & 255) * (1 - strength) + (cold & 255) * strength);
        return (r << 16) | (g << 8) | b;
    }
}

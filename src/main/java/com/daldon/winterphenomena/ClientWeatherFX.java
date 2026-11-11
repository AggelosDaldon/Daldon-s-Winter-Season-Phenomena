package com.daldon.winterphenomena;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DaldonWeather.MOD_ID, value = Dist.CLIENT,
    bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientWeatherFX {
    private ClientWeatherFX() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft game = Minecraft.getInstance();
        if (game.level == null || game.player == null) {
            ClientWeatherState.reset();
            return;
        }
        ClientLevel level = game.level;
        if (level.dimension() != Level.OVERWORLD || game.isPaused()) return;

        WeatherKind weather = ClientWeatherState.weather;
        int particles = switch (weather) {
            case SLEET -> 28;
            case SNOW -> 24;
            case BLIZZARD -> 48;
            default -> 0;
        };

        RandomSource random = level.random;
        for (int i = 0; i < particles; i++) {
            double x = game.player.getX() + random.nextInt(21) - 10 + random.nextDouble();
            double y = game.player.getY() + 3 + random.nextInt(11);
            double z = game.player.getZ() + random.nextInt(21) - 10 + random.nextDouble();
            if (!level.canSeeSky(BlockPos.containing(x, y, z)) || !level.getBlockState(BlockPos.containing(x, y, z)).isAir()) continue;
            boolean snow = weather == WeatherKind.SNOW || weather == WeatherKind.BLIZZARD
                || (weather == WeatherKind.SLEET && i % 2 == 0);
            level.addParticle(snow ? ParticleTypes.SNOWFLAKE : ParticleTypes.FALLING_WATER,
                x, y, z,
                weather == WeatherKind.BLIZZARD ? 0.065 : 0.005,
                snow ? -0.045 : -0.20, 0);
        }

        // Tiny floating snow flecks all winter, even under an otherwise clear sky.
        if (ClientWeatherState.season == Season.WINTER && random.nextInt(3) == 0) {
            double x = game.player.getX() + random.nextInt(23) - 11;
            double z = game.player.getZ() + random.nextInt(23) - 11;
            double y = game.player.getY() + random.nextInt(9) - 2;
            if (level.canSeeSky(BlockPos.containing(x, y, z)))
                level.addParticle(ParticleTypes.WHITE_ASH, x, y, z, 0.005, 0.01, 0.005);
        }
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.ComputeFogColor event) {
        Minecraft game = Minecraft.getInstance();
        if (game.level == null || game.level.dimension() != Level.OVERWORLD) return;
        if (event.getCamera().getFluidInCamera() != FogType.NONE) return;

        float dim = switch (ClientWeatherState.weather) {
            case CLOUDY -> 0.42f;
            case THUNDERCLOUDS -> 0.78f;
            case DRIZZLE -> 0.075f;
            case RAIN -> 0.20f;
            case SLEET -> 0.43f;
            case SNOW -> 0.20f;
            case BLIZZARD -> 0.44f;
            default -> 0.0f;
        };
        // Original cool, desaturated winter atmosphere; not an imported biome shader.
        float redTarget = ClientWeatherState.season == Season.WINTER ? 0.60f : 0.62f;
        float greenTarget = ClientWeatherState.season == Season.WINTER ? 0.67f : 0.68f;
        float blueTarget = ClientWeatherState.season == Season.WINTER ? 0.75f : 0.73f;
        if (ClientWeatherState.season == Season.WINTER) dim = Math.min(0.66f, dim + 0.08f);
        event.setRed(lerp(event.getRed(), redTarget, dim));
        event.setGreen(lerp(event.getGreen(), greenTarget, dim));
        event.setBlue(lerp(event.getBlue(), blueTarget, dim));
    }

    private static float lerp(float from, float to, float alpha) {
        return from * (1.0f - alpha) + to * alpha;
    }
}

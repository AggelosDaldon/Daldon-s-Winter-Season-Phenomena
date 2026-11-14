package com.daldon.winterphenomena;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
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
            ClientWeatherAudio.stopAll(game);
            ClientWeatherState.reset();
            AtmosphereLayerA.reset();
            return;
        }
        ClientLevel level = game.level;
        if (level.dimension() != Level.OVERWORLD || game.isPaused()) return;

        AtmosphereLayerA.tick();
        WeatherKind weather = ClientWeatherState.weather;
        int particles = switch (weather) {
            case SLEET -> 38;
            case SNOW -> 45;
            case BLIZZARD -> 120;
            case HAIL -> 85;
            default -> 0;
        };

        RandomSource random = level.random;
        for (int i = 0; i < particles; i++) {
            double x = game.player.getX() + random.nextInt(41) - 20 + random.nextDouble();
            double y = game.player.getY() + 3 + random.nextInt(11);
            double z = game.player.getZ() + random.nextInt(41) - 20 + random.nextDouble();
            if (!level.canSeeSky(BlockPos.containing(x, y, z)) || !level.getBlockState(BlockPos.containing(x, y, z)).isAir()) continue;
            boolean snow = weather == WeatherKind.SNOW || weather == WeatherKind.BLIZZARD
                || (weather == WeatherKind.SLEET && i % 2 == 0);
            boolean hail = weather == WeatherKind.HAIL;
            if (hail) {
                // Small cuboid-ish ice sprite, with ground collisions and two bounces.
                game.particleEngine.add(new HailStoneParticle(level, x, y, z));
            } else {
                level.addParticle(snow ? ParticleTypes.SNOWFLAKE : ParticleTypes.FALLING_WATER,
                    x, y, z,
                    weather == WeatherKind.BLIZZARD ? 0.26 : 0.005,
                    weather == WeatherKind.BLIZZARD ? -0.34 : snow ? -0.085 : -0.20, 0.0);
            }
        }

        ClientWeatherAudio.tick(game, weather);
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

        // Layer A: only the shared event palette; density/distance stay in Layer B.
        Vec3 color = AtmosphereLayerA.fog(new Vec3(event.getRed(), event.getGreen(), event.getBlue()));
        event.setRed((float)color.x);
        event.setGreen((float)color.y);
        event.setBlue((float)color.z);
    }

    private static float lerp(float from, float to, float alpha) {
        return from * (1.0f - alpha) + to * alpha;
    }
}

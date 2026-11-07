package com.daldon.winterphenomena;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Test phenomenon 01: light drizzle. All rain particles are client-local:
 * no entity ticking, weather gamerule changes, biome swaps or chunk tickets.
 * Preset density is saved server-side and synced to clients.
 */
@Mod.EventBusSubscriber(modid = DaldonWeather.MOD_ID, value = Dist.CLIENT,
    bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientDrizzleFX {
    private ClientDrizzleFX() {}

    @SubscribeEvent
    public static void renderDrizzle(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft game = Minecraft.getInstance();
        if (game.level == null || game.player == null || game.isPaused()) return;
        ClientLevel level = game.level;
        if (level.dimension() != Level.OVERWORLD ||
            ClientWeatherState.weather != WeatherKind.DRIZZLE) return;

        // Approximate number of airborne drops spawned per second:
        // LOW 12, MEDIUM 32, HIGH 64 at normal 20TPS.
        RandomSource rng = level.random;
        int density = Math.max(1, Math.min(3, ClientWeatherState.drizzleDensity));
        int drops = switch (density) {
            case 1 -> rng.nextInt(5) == 0 ? 0 : 1;
            case 2 -> 1 + rng.nextInt(2);
            default -> 2 + rng.nextInt(3);
        };

        double eyeY = game.player.getEyeY();
        for (int i = 0; i < drops; i++) {
            // Concentrate sparse rain within sight, unlike the earlier 31x31 distribution.
            double x = game.player.getX() + (rng.nextDouble() - 0.5) * 15.0;
            double z = game.player.getZ() + (rng.nextDouble() - 0.5) * 15.0;
            double y = eyeY + 2.0 + rng.nextDouble() * 5.0;
            BlockPos spawn = BlockPos.containing(x, y, z);
            if (!level.canSeeSky(spawn) || !level.getBlockState(spawn).isAir()) continue;
            // Vanilla droplet asset and particle physics, no third-party assets.
            level.addParticle(ParticleTypes.FALLING_WATER, x, y, z, 0.0, -0.25, 0.0);
        }

        // Occasional subtle drops hitting nearby exposed ground.
        // Bound checks prevent splashes on distant rooftops or inside blocks.
        int splashChance = switch (density) { case 1 -> 22; case 2 -> 13; default -> 8; };
        if (rng.nextInt(splashChance) != 0) return;

        int x = game.player.getBlockX() + rng.nextInt(13) - 6;
        int z = game.player.getBlockZ() + rng.nextInt(13) - 6;
        if (!level.hasChunk(x >> 4, z >> 4)) return;
        BlockPos surface = level.getHeightmapPos(
            Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, level.getMinBuildHeight(), z));
        if (Math.abs(surface.getY() - game.player.getY()) > 4) return;
        if (!level.canSeeSky(surface) || !level.getBlockState(surface).isAir()) return;
        level.addParticle(ParticleTypes.SPLASH,
            x + rng.nextDouble(), surface.getY() + 0.03, z + rng.nextDouble(),
            0.0, 0.02, 0.0);
    }
}

package com.daldon.winterphenomena;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ClimateEngine {
    private static MinecraftServer previousServer;

    private ClimateEngine() {}

    public static void tick(MinecraftServer server) {
        ServerLevel level = server.overworld();
        long time = level.getGameTime();
        ClimateData data = ClimateData.get(server);
        data.ensureReady(time, level.random);

        if (previousServer != server) {
            previousServer = server;
            WeatherSurface.reset();
            changed(server);
        }

        boolean changed = false;
        if (!data.seasonLocked && time >= data.nextSeasonTick) {
            data.setSeason(data.season.next(), time, level.random);
            changed = true;
        }
        if (!data.weatherLocked && time >= data.nextWeatherTick) {
            data.advanceWeather(time, level.random);
            changed = true;
        }
        if (changed) changed(server);

        WeatherSurface.tick(level, data);
        // Real gameplay effects are applied on the server, independent of
        // camera or particle settings.
        if (data.weather == WeatherKind.HAIL && time % 60 == 0) {
            for (ServerPlayer player : level.players()) {
                BlockPos top = player.blockPosition().above();
                if (!player.isSpectator() && !player.isCreative()
                    && level.canSeeSky(top)) {
                    player.hurt(level.damageSources().generic(), 1.0F);
                }
            }
        }

        // Cloud-only thunder has visible, harmless lightning without rainfall.
        if (data.weather == WeatherKind.THUNDERCLOUDS && time % 80 == 0) {
            List<ServerPlayer> players = level.players();
            if (!players.isEmpty() && level.random.nextInt(8) == 0) {
                ServerPlayer player = players.get(level.random.nextInt(players.size()));
                int x = player.getBlockX() + level.random.nextInt(65) - 32;
                int z = player.getBlockZ() + level.random.nextInt(65) - 32;
                if (level.hasChunk(x >> 4, z >> 4)) {
                    BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,
                        new BlockPos(x, level.getMinBuildHeight(), z));
                    LightningBolt flash = EntityType.LIGHTNING_BOLT.create(level);
                    if (flash != null) {
                        flash.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                        flash.setVisualOnly(true);
                        level.addFreshEntity(flash);
                    }
                }
            }
        }
    }

    public static void changed(MinecraftServer server) {
        ClimateData data = ClimateData.get(server);
        ServerLevel level = server.overworld();
        // Vanilla rain controls sky/ambient darkness. Use the native
        // rain state on overcast events; client-side precipitation rendering
        // must be suppressed separately for dry cloudy events.
        boolean overcast = switch (data.weather) {
            case CLEAR, THAW -> false;
            default -> true;
        };
        boolean vanillaThunder = data.weather == WeatherKind.THUNDERCLOUDS;
        level.setWeatherParameters(0, 24000, overcast, vanillaThunder);
        WeatherNetwork.broadcast(server);
        WeatherSurface.rescanVisible(level);
    }
    public static void stopped() {
        previousServer = null;
        WeatherSurface.reset();
    }
}

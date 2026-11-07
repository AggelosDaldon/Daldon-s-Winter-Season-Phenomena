package com.daldon.winterphenomena;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Bounded, server-only overworld surface updates. Never loads unvisited chunks
 * or mutates the biome registry / world generation settings.
 */
public final class WeatherSurface {
    private static final int MAX_COLUMNS_PER_TICK = 256;
    private static final ConcurrentLinkedQueue<ChunkJob> QUEUE = new ConcurrentLinkedQueue<>();
    private static final Set<Long> PENDING = ConcurrentHashMap.newKeySet();

    private WeatherSurface() {}

    public static void queue(ChunkPos pos) {
        long key = pos.toLong();
        if (PENDING.add(key)) QUEUE.add(new ChunkJob(pos));
    }

    public static void reset() {
        QUEUE.clear();
        PENDING.clear();
    }

    public static void tick(ServerLevel level, ClimateData climate) {
        if (level.dimension() != Level.OVERWORLD) return;

        if (level.getGameTime() % 60L == 0L) {
            for (ServerPlayer player : level.players()) {
                ChunkPos origin = player.chunkPosition();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        queue(new ChunkPos(origin.x + dx, origin.z + dz));
                    }
                }
            }
        }

        int remaining = MAX_COLUMNS_PER_TICK;
        while (remaining > 0) {
            ChunkJob job = QUEUE.peek();
            if (job == null) return;
            if (!level.hasChunk(job.pos.x, job.pos.z)) {
                QUEUE.poll();
                PENDING.remove(job.pos.toLong());
                continue;
            }
            int amount = Math.min(remaining, 256 - job.column);
            for (int i = 0; i < amount; i++) {
                int index = job.column++;
                processColumn(level, climate, job.pos.getMinBlockX() + (index & 15),
                    job.pos.getMinBlockZ() + (index >> 4));
            }
            remaining -= amount;
            if (job.column >= 256) {
                QUEUE.poll();
                PENDING.remove(job.pos.toLong());
            }
        }
    }

    private static void processColumn(ServerLevel level, ClimateData climate, int x, int z) {
        BlockPos air = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE,
            new BlockPos(x, level.getMinBuildHeight(), z));
        if (!level.isInWorldBounds(air) || air.getY() >= level.getMaxBuildHeight()) return;

        BlockPos ground = air.below();
        BlockState existing = level.getBlockState(ground);
        boolean winter = climate.season == Season.WINTER;
        boolean freezing = climate.season == Season.FREEZING;
        boolean snowing = climate.weather == WeatherKind.SNOW ||
            climate.weather == WeatherKind.BLIZZARD || climate.weather == WeatherKind.SLEET;
        boolean thawing = climate.season == Season.PREWINTER ||
            (freezing && climate.weather == WeatherKind.CLEAR);

        // Remove only our temporary layers; preserve all original snow.
        if (existing.is(WeatherBlocks.SEASONAL_SNOW.get())) {
            if (thawing && level.random.nextInt(freezing ? 10 : 3) == 0) {
                int layers = existing.getValue(SnowLayerBlock.LAYERS);
                level.setBlock(ground, layers <= 1 ? Blocks.AIR.defaultBlockState()
                    : existing.setValue(SnowLayerBlock.LAYERS, layers - 1), 3);
            } else if (snowing && level.random.nextInt(3) == 0) {
                int layers = existing.getValue(SnowLayerBlock.LAYERS);
                int cap = winter ? 6 : 2;
                if (layers < cap) level.setBlock(ground,
                    existing.setValue(SnowLayerBlock.LAYERS, layers + 1), 3);
            }
            return;
        }

        if (existing.is(WeatherBlocks.SEASONAL_ICE.get())) {
            if (thawing && level.random.nextInt(freezing ? 15 : 4) == 0) {
                level.setBlock(ground, Blocks.WATER.defaultBlockState(), 3);
            }
            return;
        }

        if ((winter || freezing) && existing.is(Blocks.WATER) &&
                existing.getFluidState().isSource() && level.random.nextInt(winter ? 2 : 12) == 0) {
            level.setBlock(ground, WeatherBlocks.SEASONAL_ICE.get().defaultBlockState(), 3);
            return;
        }

        if (!(winter || freezing) || !level.isEmptyBlock(air)) return;
        int chance = winter ? (snowing ? 2 : 7) : (snowing ? 12 : 80);
        if (level.random.nextInt(chance) != 0) return;
        BlockState layer = WeatherBlocks.SEASONAL_SNOW.get().defaultBlockState();
        if (layer.canSurvive(level, air)) level.setBlock(air, layer, 3);
    }

    private static final class ChunkJob {
        final ChunkPos pos;
        int column = 0;
        ChunkJob(ChunkPos pos) { this.pos = pos; }
    }
}

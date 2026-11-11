package com.daldon.winterphenomena;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
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
 * A world-wide CLIMATE, not a player-radius climate.
 *
 * Every FULL overworld chunk queues when loaded (including newly generated
 * chunks); all loaded chunks in player view are reconsidered after a climate
 * transition. We never load chunks just to modify them, and never rewrite
 * biome source / worldgen settings. A chunk newly explored under winter
 * receives the same snow policy as a previously explored chunk.
 */
public final class WeatherSurface {
    // Bounded per-server tick: 8 complete chunk-columns worth. No delayed
    // repeated visits to just the player's 3x3 neighborhood.
    private static final int MAX_COLUMNS_PER_TICK = 384;
    private static final int ACTIVE_VIEW_RADIUS = 8;
    private static final ArrayDeque<ChunkJob> QUEUE = new ArrayDeque<>();
    private static final Set<Long> PENDING = new HashSet<>();

    private WeatherSurface() {}

    public static void queue(ChunkPos pos) {
        if (PENDING.add(pos.toLong())) QUEUE.addLast(new ChunkJob(pos));
    }

    public static void reset() {
        QUEUE.clear();
        PENDING.clear();
    }

    /** Rescan all currently loaded chunks around all players after season/event change. */
    public static void rescanVisible(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            ChunkPos current = player.chunkPosition();
            for (int distance = 0; distance <= ACTIVE_VIEW_RADIUS; distance++) {
                for (int dx = -distance; dx <= distance; dx++) {
                    for (int dz = -distance; dz <= distance; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != distance) continue;
                        int cx = current.x + dx;
                        int cz = current.z + dz;
                        if (level.hasChunk(cx, cz)) queue(new ChunkPos(cx, cz));
                    }
                }
            }
        }
    }

    public static void tick(ServerLevel level, ClimateData climate) {
        if (level.dimension() != Level.OVERWORLD) return;

        // Weather keeps evolving throughout loaded terrain instead of
        // repeatedly spending every update only on nearby 3x3 chunks.
        if (level.getGameTime() % 240 == 0) rescanVisible(level);

        int remaining = MAX_COLUMNS_PER_TICK;
        while (remaining > 0 && !QUEUE.isEmpty()) {
            ChunkJob job = QUEUE.peekFirst();
            if (!level.hasChunk(job.pos.x, job.pos.z)) {
                finish(job);
                continue;
            }
            int count = Math.min(256 - job.column, remaining);
            for (int i = 0; i < count; i++) {
                int index = job.column++;
                processColumn(level, climate,
                    job.pos.getMinBlockX() + (index & 15),
                    job.pos.getMinBlockZ() + (index >>> 4));
            }
            remaining -= count;
            if (job.column >= 256) finish(job);
        }
    }

    private static void finish(ChunkJob job) {
        QUEUE.removeFirst();
        PENDING.remove(job.pos.toLong());
    }

    private static void processColumn(ServerLevel level, ClimateData climate, int x, int z) {
        BlockPos air = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE,
            new BlockPos(x, level.getMinBuildHeight(), z));
        if (!level.isInWorldBounds(air) || air.getY() >= level.getMaxBuildHeight()) return;

        BlockPos ground = air.below();
        BlockState existing = level.getBlockState(ground);
        boolean winter = climate.season == Season.WINTER;
        boolean freezing = climate.season == Season.FREEZING;
        boolean precipitation = climate.weather == WeatherKind.SNOW ||
            climate.weather == WeatherKind.BLIZZARD || climate.weather == WeatherKind.SLEET;
        boolean thawing = climate.season == Season.PREWINTER ||
            climate.weather == WeatherKind.THAW ||
            (freezing && climate.weather == WeatherKind.CLEAR);
        boolean snowCover = !thawing && precipitation && (winter || freezing);
        boolean freezeWater = !thawing && (winter || freezing);

        // Existing weather-created snow: melt or grow one layer at a time.
        // Vanilla natural snow and player-placed snow are never touched.
        if (existing.is(WeatherBlocks.SEASONAL_SNOW.get())) {
            int layers = existing.getValue(SnowLayerBlock.LAYERS);
            if (thawing && level.random.nextInt(climate.weather == WeatherKind.THAW ? 8 : 18) == 0) {
                // Spread melting over time and across terrain rather than
                // clearing whole chunk columns in a single frame.
                int next = Math.max(0, layers - 1);
                level.setBlock(ground, next == 0 ? Blocks.AIR.defaultBlockState()
                    : existing.setValue(SnowLayerBlock.LAYERS, next), 3);
            } else if (winter && !precipitation && layers > 4
                    && level.random.nextInt(35) == 0) {
                // Blizzard snowpack settles slowly after the storm ends.
                level.setBlock(ground, existing.setValue(SnowLayerBlock.LAYERS, layers - 1), 3);
            } else if (snowCover && layers < (winter ?
                    (climate.weather == WeatherKind.BLIZZARD ? 8 : 4) : 2)
                && level.random.nextInt(climate.weather == WeatherKind.BLIZZARD ? 28 : 95) == 0) {
                level.setBlock(ground, existing.setValue(SnowLayerBlock.LAYERS, layers + 1), 3);
            }
            return;
        }

        // Only our ice changes back into water. Vanilla ice is left alone.
        if (existing.is(WeatherBlocks.SEASONAL_ICE.get())) {
            if (thawing && level.random.nextInt(24) == 0)
                level.setBlock(ground, Blocks.WATER.defaultBlockState(), 3);
            return;
        }

        if (freezeWater && existing.is(Blocks.WATER)
            && existing.getFluidState().isSource()
            && level.random.nextInt(winter ? 32 : 90) == 0) {
            level.setBlock(ground, WeatherBlocks.SEASONAL_ICE.get().defaultBlockState(), 3);
            return;
        }

        if (!snowCover || !level.isEmptyBlock(air)) return;

        // Rare per-column deposition: multiple sweeps gradually scatter layers
        // across terrain; new chunks do not turn all white instantly. Climate
        // state remains worldwide, even when players discover distant terrain.
        int chance = climate.weather == WeatherKind.BLIZZARD ? 80 : (winter ? 300 : 600);
        if (level.random.nextInt(chance) != 0) return;
        BlockState layer = WeatherBlocks.SEASONAL_SNOW.get().defaultBlockState();
        if (layer.canSurvive(level, air)) level.setBlock(air, layer, 3);
    }

    private static final class ChunkJob {
        final ChunkPos pos;
        int column;
        ChunkJob(ChunkPos pos) { this.pos = pos; }
    }
}

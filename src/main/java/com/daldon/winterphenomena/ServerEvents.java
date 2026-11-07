package com.daldon.winterphenomena;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = DaldonWeather.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerEvents {
    private ServerEvents() {}

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        WeatherCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) ClimateEngine.tick(server);
    }

    @SubscribeEvent
    public static void chunkLoad(ChunkEvent.Load event) {
        // Do not access blocks during ChunkEvent.Load: the chunk may not be FULL yet.
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            WeatherSurface.queue(event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void playerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) WeatherNetwork.toPlayer(player);
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        ClimateEngine.stopped();
    }
}

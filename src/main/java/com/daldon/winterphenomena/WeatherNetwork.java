package com.daldon.winterphenomena;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class WeatherNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(DaldonWeather.MOD_ID, "main"),
        () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals
    );
    private WeatherNetwork() {}
    public static void init() {
        CHANNEL.registerMessage(0, ClimateSync.class, ClimateSync::encode,
            ClimateSync::decode, ClimateSync::handle);
    }
    public static void broadcast(MinecraftServer server) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new ClimateSync(ClimateData.get(server)));
    }
    public static void toPlayer(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new ClimateSync(ClimateData.get(player.getServer())));
    }
    public record ClimateSync(String season, String weather, String next, boolean seasonLocked,
                              boolean weatherLocked, int drizzleDensity, long nextEventTick, long now) {
        public ClimateSync(ClimateData d) {
            this(d.season.id(), d.weather.id(), d.nextWeather.id(), d.seasonLocked,
                 d.weatherLocked, d.drizzleDensity, d.nextWeatherTick, 0L);
        }
        public static void encode(ClimateSync p, FriendlyByteBuf b) {
            b.writeUtf(p.season);
            b.writeUtf(p.weather);
            b.writeUtf(p.next);
            b.writeBoolean(p.seasonLocked);
            b.writeBoolean(p.weatherLocked);
            b.writeVarInt(p.drizzleDensity);
            b.writeLong(p.nextEventTick);
            b.writeLong(p.now);
        }
        public static ClimateSync decode(FriendlyByteBuf b) {
            return new ClimateSync(b.readUtf(), b.readUtf(), b.readUtf(),
                b.readBoolean(), b.readBoolean(), b.readVarInt(), b.readLong(), b.readLong());
        }
        public static void handle(ClimateSync p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientWeatherState.accept(p)));
            ctx.get().setPacketHandled(true);
        }
    }
}

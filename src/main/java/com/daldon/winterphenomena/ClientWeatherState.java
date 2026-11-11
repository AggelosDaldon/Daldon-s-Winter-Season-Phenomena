package com.daldon.winterphenomena;

import net.minecraft.client.Minecraft;

public final class ClientWeatherState {
    public static Season season = Season.PREWINTER;
    public static WeatherKind weather = WeatherKind.CLEAR;
    public static WeatherKind next = WeatherKind.DRIZZLE;
    public static boolean seasonLocked = false;
    public static boolean weatherLocked = false;
    public static int drizzleDensity = 1;
    public static long nextEventTick = 0L;

    private ClientWeatherState() {}

    public static void accept(WeatherNetwork.ClimateSync message) {
        Season incoming = Season.from(message.season());
        boolean differentSeason = incoming != season;
        season = incoming;
        weather = WeatherKind.from(message.weather());
        next = WeatherKind.from(message.next());
        nextEventTick = message.nextEventTick();
        seasonLocked = message.seasonLocked();
        weatherLocked = message.weatherLocked();
        drizzleDensity = Math.max(1, Math.min(3, message.drizzleDensity()));
        Minecraft minecraft = Minecraft.getInstance();
        if (differentSeason && minecraft.levelRenderer != null) {
            // Forces chunk tint and grass colors to refresh when the season changes.
            minecraft.levelRenderer.allChanged();
        }
    }
    public static void reset() {
        season = Season.PREWINTER;
        weather = WeatherKind.CLEAR;
        next = WeatherKind.DRIZZLE;
        drizzleDensity = 1;
        nextEventTick = 0L;
    }
}

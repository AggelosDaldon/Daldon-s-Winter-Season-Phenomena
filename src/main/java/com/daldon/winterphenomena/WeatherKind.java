package com.daldon.winterphenomena;

import java.util.Locale;
import net.minecraft.util.RandomSource;

public enum WeatherKind {
    CLEAR, DRIZZLE, RAIN, CLOUDY, THUNDERCLOUDS, SLEET, SNOW, BLIZZARD, THAW;

    public String id() { return name().toLowerCase(Locale.ROOT); }
    public static WeatherKind from(String id) {
        for (WeatherKind weather : values()) if (weather.id().equalsIgnoreCase(id)) return weather;
        return CLEAR;
    }

    public static WeatherKind choose(Season season, RandomSource rng) {
        // Weighted duplicates intentionally make each season feel different.
        WeatherKind[] pool = switch (season) {
            case PREWINTER -> new WeatherKind[] {
                CLEAR, CLEAR, CLEAR, DRIZZLE, DRIZZLE, RAIN, RAIN,
                CLOUDY, CLOUDY, THUNDERCLOUDS
            };
            case FREEZING -> new WeatherKind[] {
                CLEAR, CLOUDY, CLOUDY, DRIZZLE, SLEET, SLEET,
                SNOW, SNOW, THAW, THUNDERCLOUDS
            };
            case WINTER -> new WeatherKind[] {
                CLEAR, CLOUDY, CLOUDY, SLEET, SNOW, SNOW, SNOW,
                BLIZZARD, BLIZZARD, THAW, THUNDERCLOUDS
            };
        };
        return pool[rng.nextInt(pool.length)];
    }
}

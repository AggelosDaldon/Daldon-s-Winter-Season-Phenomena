package com.daldon.winterphenomena;

import java.util.Locale;
import net.minecraft.util.RandomSource;

public enum WeatherKind {
    CLEAR, DRIZZLE, RAIN, CLOUDY, THUNDERCLOUDS, SLEET, SNOW, BLIZZARD, THAW, HAIL;

    /** Every weather is legal only in the selected season.
     * Rain/drizzle remain in prewinter, snow/blizzard in freezing/winter.
     * Vanilla clear, rain and thunderclouds remain in the automatic cycle
     * whenever climate permits them.
     */
    public boolean allowedIn(Season season) {
        return switch (season) {
            case PREWINTER -> switch (this) {
                case CLEAR, DRIZZLE, RAIN, CLOUDY, THUNDERCLOUDS, HAIL -> true;
                default -> false;
            };
            case FREEZING -> switch (this) {
                case CLEAR, CLOUDY, THUNDERCLOUDS, SLEET, SNOW, HAIL, THAW -> true;
                default -> false;
            };
            case WINTER -> switch (this) {
                case CLEAR, CLOUDY, THUNDERCLOUDS, SLEET, SNOW, BLIZZARD, HAIL, THAW -> true;
                default -> false;
            };
        };
    }

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
                CLOUDY, CLOUDY, THUNDERCLOUDS, HAIL
            };
            case FREEZING -> new WeatherKind[] {
                CLEAR, CLOUDY, CLOUDY, SLEET, HAIL, SLEET,
                SNOW, SNOW, THAW, THUNDERCLOUDS
            };
            case WINTER -> new WeatherKind[] {
                CLEAR, CLOUDY, CLOUDY, SLEET, SNOW, SNOW, SNOW,
                BLIZZARD, BLIZZARD, HAIL, THAW, THUNDERCLOUDS
            };
        };
        return pool[rng.nextInt(pool.length)];
    }
}

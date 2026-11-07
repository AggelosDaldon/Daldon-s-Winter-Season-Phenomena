package com.daldon.winterphenomena;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;

public final class ClimateData extends SavedData {
    public Season season = Season.PREWINTER;
    public WeatherKind weather = WeatherKind.CLEAR;
    public WeatherKind nextWeather = WeatherKind.DRIZZLE;
    public long nextSeasonTick = 0;
    public long nextWeatherTick = 0;
    public boolean seasonLocked = false;
    public boolean weatherLocked = false;

    public static ClimateData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            ClimateData::load, ClimateData::new, "daldon_climate");
    }
    private static ClimateData load(CompoundTag tag) {
        ClimateData d = new ClimateData();
        d.season = Season.from(tag.getString("season"));
        d.weather = WeatherKind.from(tag.getString("weather"));
        d.nextWeather = WeatherKind.from(tag.getString("nextWeather"));
        d.nextSeasonTick = tag.getLong("nextSeasonTick");
        d.nextWeatherTick = tag.getLong("nextWeatherTick");
        d.seasonLocked = tag.getBoolean("seasonLocked");
        d.weatherLocked = tag.getBoolean("weatherLocked");
        return d;
    }
    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putString("season", season.id());
        tag.putString("weather", weather.id());
        tag.putString("nextWeather", nextWeather.id());
        tag.putLong("nextSeasonTick", nextSeasonTick);
        tag.putLong("nextWeatherTick", nextWeatherTick);
        tag.putBoolean("seasonLocked", seasonLocked);
        tag.putBoolean("weatherLocked", weatherLocked);
        return tag;
    }
    public void ensureReady(long now, RandomSource rng) {
        boolean changed = false;
        if (nextSeasonTick <= 0) { nextSeasonTick = now + season.durationTicks; changed = true; }
        if (nextWeatherTick <= 0) { scheduleWeather(now, rng); changed = true; }
        if (changed) setDirty();
    }
    public void scheduleWeather(long now, RandomSource rng) {
        nextWeather = WeatherKind.choose(season, rng);
        nextWeatherTick = now + 6000L + rng.nextInt(10000);
        setDirty();
    }
    public void setSeason(Season value, long now, RandomSource rng) {
        season = value;
        nextSeasonTick = now + season.durationTicks;
        if (!weatherLocked) {
            weather = WeatherKind.choose(season, rng);
            scheduleWeather(now, rng);
        }
        setDirty();
    }
    public void setWeather(WeatherKind value, long now, RandomSource rng) {
        weather = value;
        scheduleWeather(now, rng);
        setDirty();
    }
    public void advanceWeather(long now, RandomSource rng) {
        weather = nextWeather;
        scheduleWeather(now, rng);
        setDirty();
    }
}

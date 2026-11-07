package com.daldon.winterphenomena;

import java.util.Locale;

public enum Season {
    PREWINTER(72000), FREEZING(48000), WINTER(168000);

    public final long durationTicks;
    Season(long durationTicks) { this.durationTicks = durationTicks; }
    public String id() { return name().toLowerCase(Locale.ROOT); }
    public Season next() { return values()[(ordinal() + 1) % values().length]; }
    public static Season from(String id) {
        for (Season season : values()) if (season.id().equalsIgnoreCase(id)) return season;
        return PREWINTER;
    }
}

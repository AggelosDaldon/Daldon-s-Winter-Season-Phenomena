package com.daldon.winterphenomena;

import net.minecraft.world.phys.Vec3;

/**
 * Client-only Layer A: shared, smoothly blended sky, cloud and air palette.
 * Colors are deliberately neutral-grey for snow (no blue wash).
 * Darkness is a visual tint coefficient, not a change to block light levels.
 */
public final class AtmosphereLayerA {
    private AtmosphereLayerA() {}

    public record Palette(float darkness, int sky, int clouds, int fog, float overcast) {}

    private static final Palette CLEAR = new Palette(0f, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0f);
    private static float darkness, overcast, snowGrey;
    private static float skyR = 1f, skyG = 1f, skyB = 1f;
    private static float cloudR = 1f, cloudG = 1f, cloudB = 1f;
    private static float fogR = 1f, fogG = 1f, fogB = 1f;

    public static Palette preset(WeatherKind weather) {
        return switch (weather) {
            case CLOUDY -> new Palette(.18f, 0x64676A, 0x55585A, 0x747778, .58f);
            case DRIZZLE -> new Palette(.24f, 0x55595B, 0x484B4D, 0x696D6D, .44f);
            case RAIN -> new Palette(.33f, 0x494D50, 0x3D4144, 0x616566, .68f);
            case THUNDERCLOUDS -> new Palette(.48f, 0x33383B, 0x292D30, 0x4C5052, .78f);
            case SLEET -> new Palette(.35f, 0x555758, 0x454748, 0x67696A, .70f);
            case SNOW -> new Palette(.29f, 0x777878, 0x616363, 0x929292, .62f);
            case BLIZZARD -> new Palette(.42f, 0x555656, 0x444646, 0x777979, .88f);
            case HAIL -> new Palette(.46f, 0x424648, 0x34383A, 0x5E6263, .84f);
            case THAW -> new Palette(.16f, 0x797979, 0x686868, 0x858585, .36f);
            default -> CLEAR;
        };
    }

    public static void tick() {
        Palette p = preset(ClientWeatherState.weather);
        // ~2-3 seconds for a full weather transition at 20 TPS.
        float speed = .022f;
        darkness = move(darkness, p.darkness(), speed);
        snowGrey = move(snowGrey, (ClientWeatherState.weather == WeatherKind.SNOW || ClientWeatherState.weather == WeatherKind.BLIZZARD) ? .94f : 0f, speed);
        overcast = move(overcast, p.overcast(), speed);
        skyR = move(skyR, channel(p.sky(),16), speed);
        skyG = move(skyG, channel(p.sky(),8), speed);
        skyB = move(skyB, channel(p.sky(),0), speed);
        cloudR = move(cloudR, channel(p.clouds(),16), speed);
        cloudG = move(cloudG, channel(p.clouds(),8), speed);
        cloudB = move(cloudB, channel(p.clouds(),0), speed);
        fogR = move(fogR, channel(p.fog(),16), speed);
        fogG = move(fogG, channel(p.fog(),8), speed);
        fogB = move(fogB, channel(p.fog(),0), speed);
    }

    public static void reset() {
        darkness = 0f;
        overcast = 0f;
        snowGrey = 0f;
        skyR = skyG = skyB = cloudR = cloudG = cloudB = fogR = fogG = fogB = 1f;
    }

    private static float move(float a, float b, float speed) {
        return a + Math.max(-speed, Math.min(speed, b-a));
    }
    private static float channel(int rgb, int shift) { return ((rgb >> shift) & 255) / 255f; }

    public static float overcast() { return overcast; }
    public static float darkness() { return darkness; }
    public static float cloudRed() { return cloudR; }
    public static float cloudGreen() { return cloudG; }
    public static float cloudBlue() { return cloudB; }

    // Multiplicative tint: never makes the night brighter.
    private static float shade(float base, float target, float strength) {
        return Math.max(0f, Math.min(1f,
            base * (1f - darkness * .66f) * (1f - strength + strength * (.64f + .36f * target))));
    }

    public static Vec3 sky(Vec3 vanilla) {
        if (vanilla == null) return null;
        Vec3 neutral = neutralize(vanilla);
        return new Vec3(shade((float)neutral.x, skyR, .48f),
                        shade((float)neutral.y, skyG, .48f),
                        shade((float)neutral.z, skyB, .48f));
    }
    public static Vec3 clouds(Vec3 vanilla) {
        if (vanilla == null) return null;
        Vec3 neutral = neutralize(vanilla);
        return new Vec3(shade((float)neutral.x, cloudR, .77f),
                        shade((float)neutral.y, cloudG, .77f),
                        shade((float)neutral.z, cloudB, .77f));
    }
    private static Vec3 neutralize(Vec3 vanilla) {
        double grey = vanilla.x * .299 + vanilla.y * .587 + vanilla.z * .114;
        return new Vec3(vanilla.x + (grey - vanilla.x) * snowGrey,
                        vanilla.y + (grey - vanilla.y) * snowGrey,
                        vanilla.z + (grey - vanilla.z) * snowGrey);
    }

    public static Vec3 fog(Vec3 vanilla) {
        Vec3 neutral = neutralize(vanilla);
        return new Vec3(shade((float)neutral.x, fogR, .60f),
                        shade((float)neutral.y, fogG, .60f),
                        shade((float)neutral.z, fogB, .60f));
    }
}

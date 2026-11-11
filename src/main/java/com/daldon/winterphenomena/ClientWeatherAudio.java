package com.daldon.winterphenomena;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * A continuous, looped weather sound rather than replaying a 2-6s clip
 * every six seconds. The loop is stopped when weather changes.
 */
public final class ClientWeatherAudio {
    private static LoopSound active;
    private static WeatherKind kind;

    private ClientWeatherAudio() {}

    public static void tick(Minecraft minecraft, WeatherKind current) {
        SoundEvent nextSound = switch (current) {
            case DRIZZLE, RAIN, SLEET, SNOW, HAIL -> SoundEvents.WEATHER_RAIN;
            case BLIZZARD, THUNDERCLOUDS -> SoundEvents.AMBIENT_SOUL_SAND_VALLEY_LOOP.value();
            default -> null;
        };
        if (nextSound == null) {
            if (active != null) minecraft.getSoundManager().stop(active);
            active = null;
            kind = null;
            return;
        }
        if (active == null || active.isStopped() || kind != current) {
            if (active != null) minecraft.getSoundManager().stop(active);
            kind = current;
            float volume = switch (current) {
                case DRIZZLE -> .12f;
                case SNOW -> .10f;
                case SLEET -> .22f;
                case HAIL -> .43f;
                case BLIZZARD -> .62f;
                default -> .35f;
            };
            active = new LoopSound(nextSound, volume, current == WeatherKind.BLIZZARD ? .80f : 1f);
            minecraft.getSoundManager().play(active);
        }
    }

    private static final class LoopSound extends AbstractTickableSoundInstance {
        private LoopSound(SoundEvent sound, float volume, float pitch) {
            super(sound, SoundSource.WEATHER, RandomSource.create());
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.volume = volume;
            this.pitch = pitch;
            this.attenuation = Attenuation.NONE;
        }

        @Override public void tick() {}
    }
}

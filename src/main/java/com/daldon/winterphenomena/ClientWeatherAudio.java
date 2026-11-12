package com.daldon.winterphenomena;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * Thunder Clouds: two original wind recordings, independent slow rumble one-shots,
 * plus vanilla lightning flashes, strikes and thunder managed by the server.
 */
public final class ClientWeatherAudio {
    private static LoopSound active;
    private static LoopSound thunderCalm;
    private static LoopSound thunderClose;
    private static WeatherKind kind;
    private static int rumbleCountdown = 280;
    private static final RandomSource RNG = RandomSource.create();

    private ClientWeatherAudio() {}

    public static void tick(Minecraft minecraft, WeatherKind current) {
        if (current == WeatherKind.THUNDERCLOUDS) {
            if (active != null) minecraft.getSoundManager().stop(active);
            active = null;
            if (kind != current || thunderCalm == null || thunderCalm.isStopped()) {
                stopStorm(minecraft);
                thunderCalm = new LoopSound(WeatherSounds.THUNDER_WIND_CALM.get(), .29f, 1f);
                thunderClose = new LoopSound(WeatherSounds.THUNDER_WIND_CLOSE.get(), .18f, .98f);
                minecraft.getSoundManager().play(thunderCalm);
                minecraft.getSoundManager().play(thunderClose);
                rumbleCountdown = 180 + RNG.nextInt(220);
            }
            if (--rumbleCountdown <= 0) {
                SoundEvent rumble = RNG.nextBoolean()
                    ? WeatherSounds.THUNDER_RUMBLE_DEEP.get()
                    : WeatherSounds.THUNDER_RUMBLE_ROLLING.get();
                // These are distant rolling rumbles, never a replacement for a lightning bolt.
                minecraft.getSoundManager().play(new RumbleSound(rumble,
                    .42f + RNG.nextFloat() * .14f, .93f + RNG.nextFloat() * .12f));
                rumbleCountdown = 340 + RNG.nextInt(460);
            }
            kind = current;
            return;
        }

        stopStorm(minecraft);
        SoundEvent nextSound = switch(current) {
            case DRIZZLE, RAIN, SLEET, HAIL -> SoundEvents.WEATHER_RAIN;
            case CLOUDY, SNOW, BLIZZARD -> SoundEvents.AMBIENT_SOUL_SAND_VALLEY_LOOP.value();
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
            float volume = switch(current) {
                case DRIZZLE -> .12f;
                case CLOUDY -> .09f;
                case SNOW -> .13f;
                case SLEET -> .22f;
                case HAIL -> .43f;
                case BLIZZARD -> .62f;
                default -> .35f;
            };
            active = new LoopSound(nextSound,volume,current==WeatherKind.BLIZZARD?.80f:1f);
            minecraft.getSoundManager().play(active);
        }
        kind = current;
    }

    public static void stopAll(Minecraft minecraft) {
        if (active != null) minecraft.getSoundManager().stop(active);
        active = null;
        stopStorm(minecraft);
        kind = null;
    }

    private static void stopStorm(Minecraft game) {
        if (thunderCalm != null) game.getSoundManager().stop(thunderCalm);
        if (thunderClose != null) game.getSoundManager().stop(thunderClose);
        thunderCalm = null;
        thunderClose = null;
    }

    private static final class LoopSound extends AbstractTickableSoundInstance {
        private LoopSound(SoundEvent sound,float volume,float pitch) {
            super(sound,SoundSource.WEATHER,RandomSource.create());
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.volume = volume;
            this.pitch = pitch;
            this.attenuation = Attenuation.NONE;
        }
        @Override public void tick() {}
    }

    private static final class RumbleSound extends AbstractSoundInstance {
        private RumbleSound(SoundEvent sound,float volume,float pitch) {
            super(sound,SoundSource.WEATHER,RandomSource.create());
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.volume = volume;
            this.pitch = pitch;
            this.looping = false;
        }
    }
}

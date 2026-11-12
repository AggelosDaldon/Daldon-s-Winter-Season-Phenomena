package com.daldon.winterphenomena;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Original user-selected audio assets. Bundled through the accompanying sound resource pack. */
public final class WeatherSounds {
    private WeatherSounds() {}
    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, DaldonWeather.MOD_ID);
    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () ->
            SoundEvent.createVariableRangeEvent(new ResourceLocation(DaldonWeather.MOD_ID, name)));
    }
    public static final RegistryObject<SoundEvent> THUNDER_WIND_CALM = register("thunder_wind_calm");
    public static final RegistryObject<SoundEvent> THUNDER_WIND_CLOSE = register("thunder_wind_close");
    public static final RegistryObject<SoundEvent> THUNDER_RUMBLE_DEEP = register("thunder_rumble_deep");
    public static final RegistryObject<SoundEvent> THUNDER_RUMBLE_ROLLING = register("thunder_rumble_rolling");
}

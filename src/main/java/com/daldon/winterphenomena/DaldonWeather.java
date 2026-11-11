package com.daldon.winterphenomena;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(DaldonWeather.MOD_ID)
public final class DaldonWeather {
    // Stable technical identifier; display name can change without breaking saves.
    public static final String MOD_ID = "daldonweather";

    public DaldonWeather() {
        WeatherBlocks.BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        WeatherBlocks.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        WeatherBlocks.BLOCK_ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
        WeatherNetwork.init();
    }
}

package com.daldon.winterphenomena;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = DaldonWeather.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT)
public final class ForecastClientRegistration {
    private ForecastClientRegistration() {}
    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(WeatherBlocks.FORECAST_COMPUTER_ENTITY.get(),
            ForecastComputerRenderer::new);
    }
}

package com.daldon.winterphenomena.mixin;

import com.daldon.winterphenomena.ClientWeatherState;
import com.daldon.winterphenomena.WeatherKind;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keep Minecraft's vanilla sky/rain darkness, but render precipitation
 * ourselves for custom weather. Vanilla rain is kept for normal rain,
 * and prewinter thunderstorms only.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererWeatherMixin {
    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
    private void daldon$hideVanillaPrecipitation(LightTexture light, float partialTick,
            double x, double y, double z, CallbackInfo ci) {
        WeatherKind event = ClientWeatherState.weather;
        if (event != WeatherKind.RAIN &&
            !(event == WeatherKind.THUNDERCLOUDS &&
              ClientWeatherState.season == com.daldon.winterphenomena.Season.PREWINTER)) {
            ci.cancel();
        }
    }
}

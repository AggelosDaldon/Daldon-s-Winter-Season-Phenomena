package com.daldon.winterphenomena.mixin;

import com.daldon.winterphenomena.AtmosphereLayerA;
import com.daldon.winterphenomena.ClientWeatherState;
import com.daldon.winterphenomena.WeatherKind;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Apply event-specific sky/cloud colors after vanilla and other normal computations. */
@Mixin(ClientLevel.class)
public abstract class ClientLevelAtmosphereMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void daldon$sky(Vec3 cameraPosition, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (ClientWeatherState.weather != WeatherKind.CLEAR)
            cir.setReturnValue(AtmosphereLayerA.sky(cir.getReturnValue()));
    }

    @Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
    private void daldon$clouds(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (ClientWeatherState.weather != WeatherKind.CLEAR)
            cir.setReturnValue(AtmosphereLayerA.clouds(cir.getReturnValue()));
    }
}

package com.daldon.winterphenomena;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Lightweight overcast cloud deck. Drawn in world space AFTER sky and BEFORE
 * terrain, so blocks, trees, mountains and roofs occlude the clouds correctly.
 * No biome mutation and no dependence on vanilla rain state.
 */
@Mod.EventBusSubscriber(modid = DaldonWeather.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
    value = Dist.CLIENT)
public final class ClientSkyOverlay {
    private ClientSkyOverlay() {}
    private static float currentOvercast = 0f;

    @SubscribeEvent
    public static void afterSky(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.level.dimension() != Level.OVERWORLD) return;

        float strength = switch (ClientWeatherState.weather) {
            case CLOUDY -> 0.40f;
            case DRIZZLE -> 0.24f;
            case RAIN -> 0.54f;
            case THUNDERCLOUDS -> 0.82f;
            case SLEET -> 0.56f;
            case SNOW -> 0.36f;
            case BLIZZARD -> 0.75f;
            case HAIL -> 0.80f;
            default -> 0.0f;
        };
        // Gentle blending rather than sudden changes between cloud events.
        float step = 0.012f;
        currentOvercast += Math.max(-step, Math.min(step, strength - currentOvercast));
        strength = currentOvercast;
        if (strength <= 0.003f) return;
        float red = ClientWeatherState.weather == WeatherKind.THUNDERCLOUDS ? 0.11f : 0.31f;
        float green = ClientWeatherState.weather == WeatherKind.THUNDERCLOUDS ? 0.13f : 0.34f;
        float blue = ClientWeatherState.weather == WeatherKind.THUNDERCLOUDS ? 0.18f : 0.39f;

        // Position in camera-relative coordinates; leave depth testing enabled
        // so physical terrain rendered afterward is not obscured.
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        // Event matrices are already camera-relative. Translating them by the
        // camera coordinates again pushed the overcast deck out of view.
        // Keep the cloud sheet at a consistent height above the camera.
        // The sheets are centered on the viewer and extend beyond the near particle field.
        Matrix4f mat = pose.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();

        // Darkened upper sky base, tiled translucent cloud banks.
        // Two layers increase the impression of rolling overcast skies.
        for (int layer = 0; layer < 2; layer++) {
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            float height = (float)(layer == 0 ? 64.0 : 78.0);
            int alphaBase = Math.round(175f * strength);
            for (int x = -5; x <= 5; x++) {
                for (int z = -5; z <= 5; z++) {
                    int noise = Math.floorMod(x * 37 + z * 59 + layer * 83, 13);
                    if (noise == 0 || noise == 3) continue;
                    float alpha = (alphaBase / 255f) * (noise > 7 ? .72f : 1f);
                    float size = 46f;
                    float left = x * size - 12f + layer * 9f;
                    float near = z * size - 12f + layer * 12f;
                    buffer.vertex(mat, left, height, near).color(red, green, blue, alpha).endVertex();
                    buffer.vertex(mat, left + size, height, near).color(red, green, blue, alpha).endVertex();
                    buffer.vertex(mat, left + size, height, near + size).color(red, green, blue, alpha).endVertex();
                    buffer.vertex(mat, left, height, near + size).color(red, green, blue, alpha).endVertex();
                }
            }
            BufferUploader.drawWithShader(buffer.end());
        }
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        pose.popPose();
    }
}

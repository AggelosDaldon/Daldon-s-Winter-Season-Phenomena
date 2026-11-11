package com.daldon.winterphenomena;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
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
 * World-space sheets of remote snow/rain: unlike engine particles, these
 * render beyond the 32 block particle culling radius, out to ~100 blocks.
 * The weather state itself is synchronized globally across all players.
 * No server-side particle entities, chunk loads, or weather biome overrides.
 */
@Mod.EventBusSubscriber(modid = DaldonWeather.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
    value = Dist.CLIENT)
public final class DistantPrecipitation {
    private DistantPrecipitation() {}

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.level.dimension() != Level.OVERWORLD) return;

        WeatherKind weather = ClientWeatherState.weather;
        boolean snow = weather == WeatherKind.SNOW || weather == WeatherKind.BLIZZARD
            || weather == WeatherKind.SLEET;
        boolean rain = weather == WeatherKind.DRIZZLE || weather == WeatherKind.RAIN
            || weather == WeatherKind.SLEET || weather == WeatherKind.HAIL;
        if (!snow && !rain) return;

        double camX = mc.gameRenderer.getMainCamera().getPosition().x;
        double camY = mc.gameRenderer.getMainCamera().getPosition().y;
        double camZ = mc.gameRenderer.getMainCamera().getPosition().z;

        Matrix4f pose = event.getPoseStack().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        double time = (mc.level.getGameTime() + event.getPartialTick()) * 0.10;
        int density = weather == WeatherKind.BLIZZARD ? 5 : 3;
        for (int tileX = -8; tileX <= 8; tileX++) {
            for (int tileZ = -8; tileZ <= 8; tileZ++) {
                int key = tileX * 7349 ^ tileZ * 1931;
                if (Math.floorMod(key, 5) > density) continue;
                // Fixed spatial coordinates with deterministic drift / fall:
                // no jarring particle bubble following the player.
                double worldX = Math.floor(camX / 12) * 12 + tileX * 12 + Math.floorMod(key, 9);
                double worldZ = Math.floor(camZ / 12) * 12 + tileZ * 12 + Math.floorMod(key >> 4, 9);
                double dx = worldX - camX, dz = worldZ - camZ;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist < 30 || dist > 102) continue;
                float alpha = (float)(0.37 * Math.max(0, (102 - dist) / 75));
                if (alpha < .012f) continue;
                double phase = Math.floorMod(key, 200) / 200.0;
                double y = 7.0 - ((phase * 25 + time * (weather == WeatherKind.BLIZZARD ? 4 : 1.6)) % 25);
                double x = dx - (weather == WeatherKind.BLIZZARD ? (time % 12) * .36 : 0);
                double z = dz;
                // Snow: light short streaks; rain: thinner, longer lines.
                float width = snow ? .23f : .085f;
                float height = snow ? .72f : 1.55f;
                float red = snow ? .86f : .66f;
                float green = snow ? .91f : .75f;
                float blue = snow ? .97f : .87f;
                buffer.vertex(pose, (float)x, (float)(y-camY+mc.player.getY()), (float)z)
                    .color(red, green, blue, alpha).endVertex();
                buffer.vertex(pose, (float)(x+width), (float)(y-camY+mc.player.getY()), (float)z)
                    .color(red, green, blue, alpha).endVertex();
                buffer.vertex(pose, (float)(x+width), (float)(y+height-camY+mc.player.getY()), (float)z)
                    .color(red, green, blue, alpha).endVertex();
                buffer.vertex(pose, (float)x, (float)(y+height-camY+mc.player.getY()), (float)z)
                    .color(red, green, blue, alpha).endVertex();
            }
        }
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }
}

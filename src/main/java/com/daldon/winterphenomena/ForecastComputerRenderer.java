package com.daldon.winterphenomena;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws live text directly on the CRT screen, not an inventory GUI.
 * Client weather is synced from the server and is universal across Overworld.
 */
public final class ForecastComputerRenderer implements BlockEntityRenderer<ForecastComputerBlockEntity> {
    private final Font font;

    public ForecastComputerRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(ForecastComputerBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        if (!state.hasProperty(ForecastComputerBlock.FACING)) return;

        pose.pushPose();
        pose.translate(0.5, 0.64, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-state.getValue(ForecastComputerBlock.FACING).toYRot()));
        pose.translate(0, 0, 0.326);
        pose.scale(0.0047f, -0.0047f, 0.0047f);

        String[] lines = {
            "DALDON WX",
            "SEASON " + ClientWeatherState.season.id().toUpperCase(),
            "NOW " + ClientWeatherState.weather.id().toUpperCase(),
            "NEXT " + (ClientWeatherState.weatherLocked ? "MANUAL" : ClientWeatherState.next.id().toUpperCase()),
            "ETA " + remaining(be)
        };
        for (int i = 0; i < lines.length; i++) {
            int shade = i == 0 ? 0xB7F6A8 : 0x83DB79;
            int x = -font.width(lines[i]) / 2;
            font.drawInBatch(lines[i], x, -35 + i * 14, shade, false,
                pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        }
        pose.popPose();
    }

    private static String remaining(ForecastComputerBlockEntity be) {
        if (ClientWeatherState.weatherLocked) return "--:--";
        if (be.getLevel() == null) return "--:--";
        long ticks = Math.max(0, ClientWeatherState.nextEventTick - be.getLevel().getGameTime());
        long seconds = ticks / 20;
        return (seconds / 60) + "m " + (seconds % 60) + "s";
    }
}

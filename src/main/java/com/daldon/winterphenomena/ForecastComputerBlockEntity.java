package com.daldon.winterphenomena;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ForecastComputerBlockEntity extends BlockEntity {
    public ForecastComputerBlockEntity(BlockPos pos, BlockState state) {
        super(WeatherBlocks.FORECAST_COMPUTER_ENTITY.get(), pos, state);
    }
}

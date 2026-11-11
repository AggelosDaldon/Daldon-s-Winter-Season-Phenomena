package com.daldon.winterphenomena;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Separate block IDs let thawing remove only weather-created snow and ice.
 * No vanilla snow, player-placed ice, or worldgen biome data are rewritten.
 */
public final class WeatherBlocks {
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, DaldonWeather.MOD_ID);

    public static final RegistryObject<Block> SEASONAL_SNOW = BLOCKS.register(
        "seasonal_snow", () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(Blocks.SNOW)) {
            @Override
            public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
                // Seasonal melting is handled by WeatherSurface, not by vanilla light-based melting.
            }
        });
    public static final RegistryObject<Block> SEASONAL_ICE = BLOCKS.register(
        "seasonal_ice", () -> new IceBlock(BlockBehaviour.Properties.copy(Blocks.ICE)) {
            @Override
            public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
                // WeatherSurface restores water when the world warms.
            }
        });

    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, DaldonWeather.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DaldonWeather.MOD_ID);

    public static final RegistryObject<Block> FORECAST_COMPUTER = BLOCKS.register(
        "forecast_computer",
        () -> new ForecastComputerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(2.0f)));

    public static final RegistryObject<Item> FORECAST_COMPUTER_ITEM = ITEMS.register(
        "forecast_computer",
        () -> new BlockItem(FORECAST_COMPUTER.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<ForecastComputerBlockEntity>>
        FORECAST_COMPUTER_ENTITY = BLOCK_ENTITIES.register("forecast_computer",
        () -> BlockEntityType.Builder.of(ForecastComputerBlockEntity::new,
            FORECAST_COMPUTER.get()).build(null));

    private WeatherBlocks() {}
}

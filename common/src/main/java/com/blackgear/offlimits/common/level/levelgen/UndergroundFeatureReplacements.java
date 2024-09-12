package com.blackgear.offlimits.common.level.levelgen;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.core.registry.ModFeatures;
import com.blackgear.platform.common.providers.height.BiasedToBottomHeight;
import com.blackgear.platform.common.providers.height.VeryBiasedToBottomHeight;
import com.blackgear.platform.common.worldgen.WorldGenRegistry;
import com.blackgear.platform.common.worldgen.decorator.RangedConfiguration;
import com.blackgear.platform.common.worldgen.height.VerticalAnchor;
import com.google.common.collect.ImmutableSet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SpringConfiguration;
import net.minecraft.world.level.levelgen.placement.ConfiguredDecorator;
import net.minecraft.world.level.material.Fluids;

import java.util.Set;

public class UndergroundFeatureReplacements {
    public static final WorldGenRegistry FEATURES = WorldGenRegistry.create(Offlimits.MOD_ID);
    
    public static final ConfiguredFeature<?, ?> LAKE_WATER = FEATURES.register(
        "lake_water",
        ModFeatures.LAKE.get()
            .configured(
                new BlockStateConfiguration(Blocks.WATER.defaultBlockState())
            )
            .decorated(Decorators.FULL_RANGE)
            .squared()
            .chance(32)
    );
    public static final ConfiguredFeature<?, ?> LAKE_LAVA = FEATURES.register(
        "lake_lava",
        ModFeatures.LAKE.get()
            .configured(
                new BlockStateConfiguration(Blocks.LAVA.defaultBlockState())
            )
            .decorated(RangedConfiguration.of(BiasedToBottomHeight.of(VerticalAnchor.bottom(), VerticalAnchor.top(), 8)))
            .squared()
            .chance(8)
    );
    public static final ConfiguredFeature<?, ?> SPRING_WATER = FEATURES.register(
        "spring_water",
        Feature.SPRING
            .configured(
                new SpringConfiguration(
                    Fluids.WATER.defaultFluidState(),
                    true,
                    4,
                    1,
                    Settings.WATER_SPRING_REPLACEABLES
                )
            )
            .decorated(RangedConfiguration.uniform(VerticalAnchor.bottom(), VerticalAnchor.top()))
            .squared()
            .count(40)
    );
    public static final ConfiguredFeature<?, ?> SPRING_LAVA = FEATURES.register(
        "spring_lava",
        Feature.SPRING
            .configured(
                new SpringConfiguration(
                    Fluids.LAVA.defaultFluidState(),
                    true,
                    4,
                    1,
                    Settings.LAVA_SPRING_REPLACEABLES
                )
            )
            .decorated(RangedConfiguration.of(VeryBiasedToBottomHeight.of(VerticalAnchor.bottom(), VerticalAnchor.belowTop(8), 8)))
            .squared()
            .count(20)
    );
    public static final ConfiguredFeature<?, ?> MONSTER_ROOM = FEATURES.register(
        "monster_room",
        Feature.MONSTER_ROOM
            .configured(FeatureConfiguration.NONE)
            .decorated(Decorators.FULL_RANGE)
            .squared()
            .count(8)
    );
    
    
    public static final class Settings {
        private static final Set<Block> WATER_SPRING_REPLACEABLES = ImmutableSet.of(
            Blocks.STONE,
            Blocks.GRANITE,
            Blocks.DIORITE,
            Blocks.ANDESITE,
            Blocks.DIRT,
            Blocks.SNOW_BLOCK,
            Blocks.PACKED_ICE
        );
        private static final Set<Block> LAVA_SPRING_REPLACEABLES = ImmutableSet.of(
            Blocks.STONE,
            Blocks.GRANITE,
            Blocks.DIORITE,
            Blocks.ANDESITE
        );
    }
    
    public static final class Decorators {
        public static final ConfiguredDecorator<?> FULL_RANGE = RangedConfiguration.uniform(VerticalAnchor.bottom(), VerticalAnchor.top());
    }
}
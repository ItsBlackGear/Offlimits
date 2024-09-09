package com.blackgear.offlimits.common.level.levelgen;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.platform.common.providers.math.UniformInt;
import com.blackgear.platform.common.registry.PlatformFeatures;
import com.blackgear.platform.common.worldgen.WorldGenRegistry;
import com.blackgear.platform.common.worldgen.decorator.CountConfiguration;
import com.blackgear.platform.common.worldgen.decorator.CountDecorator;
import com.blackgear.platform.common.worldgen.decorator.RangedConfiguration;
import com.blackgear.platform.common.worldgen.feature.OverlayOreConfiguration;
import com.blackgear.platform.common.worldgen.height.VerticalAnchor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

public class OreFeatures {
    public static final WorldGenRegistry FEATURES = WorldGenRegistry.create(Offlimits.MOD_ID);
    
    public static final ConfiguredFeature<?, ?> ORE_COAL_UPPER = FEATURES.register(
        "ore_coal_upper",
        PlatformFeatures.OVERLAY_ORE.get()
            .configured(
                new OverlayOreConfiguration(
                    OreConfiguration.Predicates.NATURAL_STONE,
                    Blocks.COAL_ORE.defaultBlockState(),
                    17
                )
            )
            .decorated(RangedConfiguration.uniform(VerticalAnchor.absolute(136), VerticalAnchor.top()))
            .squared()
            .count(30)
    );
    public static final ConfiguredFeature<?, ?> ORE_COAL_LOWER = FEATURES.register(
        "ore_coal_lower",
        PlatformFeatures.OVERLAY_ORE.get()
            .configured(
                new OverlayOreConfiguration(
                    OreConfiguration.Predicates.NATURAL_STONE,
                    Blocks.COAL_ORE.defaultBlockState(),
                    17,
                    0.5F
                )
            )
            .decorated(RangedConfiguration.triangle(VerticalAnchor.absolute(0), VerticalAnchor.absolute(192)))
            .squared()
            .count(20)
    );
    public static final ConfiguredFeature<?, ?> ORE_IRON_UPPER = FEATURES.register(
        "ore_iron_upper",
        PlatformFeatures.OVERLAY_ORE.get()
            .configured(
                new OverlayOreConfiguration(
                    OreConfiguration.Predicates.NATURAL_STONE,
                    Blocks.IRON_ORE.defaultBlockState(),
                    9
                )
            )
            .decorated(RangedConfiguration.triangle(VerticalAnchor.absolute(80), VerticalAnchor.absolute(384)))
            .squared()
            .count(90)
    );
    public static final ConfiguredFeature<?, ?> ORE_IRON_MIDDLE = FEATURES.register(
        "ore_iron_middle",
        PlatformFeatures.OVERLAY_ORE.get()
            .configured(
                new OverlayOreConfiguration(
                    OreConfiguration.Predicates.NATURAL_STONE,
                    Blocks.IRON_ORE.defaultBlockState(),
                    9
                )
            )
            .decorated(RangedConfiguration.triangle(VerticalAnchor.absolute(-24), VerticalAnchor.absolute(56)))
            .squared()
            .count(10)
    );
    public static final ConfiguredFeature<?, ?> ORE_IRON_SMALL = FEATURES.register(
        "ore_iron_small",
        PlatformFeatures.OVERLAY_ORE.get()
            .configured(
                new OverlayOreConfiguration(
                    OreConfiguration.Predicates.NATURAL_STONE,
                    Blocks.IRON_ORE.defaultBlockState(),
                    4
                )
            )
            .decorated(RangedConfiguration.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(72)))
            .squared()
            .count(10)
    );
    public static final ConfiguredFeature<?, ?> ORE_GOLD_EXTRA = FEATURES.register(
        "ore_gold_extra",
        PlatformFeatures.OVERLAY_ORE.get()
            .configured(
                new OverlayOreConfiguration(
                    OreConfiguration.Predicates.NATURAL_STONE,
                    Blocks.GOLD_ORE.defaultBlockState(),
                    9
                )
            )
            .decorated(RangedConfiguration.uniform(VerticalAnchor.absolute(32), VerticalAnchor.absolute(256)))
            .squared()
            .count(50)
    );
    public static final ConfiguredFeature<?, ?> ORE_GOLD = FEATURES.register(
        "ore_gold",
        PlatformFeatures.OVERLAY_ORE.get()
            .configured(
                new OverlayOreConfiguration(
                    OreConfiguration.Predicates.NATURAL_STONE,
                    Blocks.GOLD_ORE.defaultBlockState(),
                    9,
                    0.5F
                )
            )
            .decorated(RangedConfiguration.triangle(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(32)))
            .squared()
            .count(4)
    );
    public static final ConfiguredFeature<?, ?> ORE_GOLD_LOWER = FEATURES.register(
        "ore_gold_lower",
        PlatformFeatures.OVERLAY_ORE.get()
            .configured(
                new OverlayOreConfiguration(
                    OreConfiguration.Predicates.NATURAL_STONE,
                    Blocks.GOLD_ORE.defaultBlockState(),
                    9,
                    0.5F
                )
            )
            .decorated(RangedConfiguration.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(-48)))
            .squared()
            .decorated(CountConfiguration.of(UniformInt.of(0, 1)))
    );
    public static final ConfiguredFeature<?, ?> ORE_REDSTONE = FEATURES.register(
        "ore_redstone",
        PlatformFeatures.OVERLAY_ORE.get()
            .configured(
                new OverlayOreConfiguration(
                    OreConfiguration.Predicates.NATURAL_STONE,
                    Blocks.REDSTONE_ORE.defaultBlockState(),
                    8
                )
            )
            .decorated(RangedConfiguration.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(15)))
            .squared()
            .count(4)
    );
    public static final ConfiguredFeature<?, ?> ORE_REDSTONE_LOWER = FEATURES.register(
        "ore_redstone_lower",
        PlatformFeatures.OVERLAY_ORE.get()
            .configured(
                new OverlayOreConfiguration(
                    OreConfiguration.Predicates.NATURAL_STONE,
                    Blocks.REDSTONE_ORE.defaultBlockState(),
                    8
                )
            )
            .decorated(RangedConfiguration.triangle(VerticalAnchor.aboveBottom(-32), VerticalAnchor.aboveBottom(32)))
            .squared()
            .count(20)
    );
    public static final ConfiguredFeature<?, ?> ORE_DIAMOND = FEATURES.register(
        "ore_diamond",
        PlatformFeatures.OVERLAY_ORE.get()
            .configured(
                new OverlayOreConfiguration(
                    OreConfiguration.Predicates.NATURAL_STONE,
                    Blocks.DIAMOND_ORE.defaultBlockState(),
                    4,
                    0.5F
                )
            )
            .decorated(RangedConfiguration.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80)))
            .squared()
            .count(7)
    );
    public static final ConfiguredFeature<?, ?> ORE_DIAMOND_MEDIUM = FEATURES.register(
        "ore_diamond_medium",
        PlatformFeatures.OVERLAY_ORE.get()
            .configured(
                new OverlayOreConfiguration(
                    OreConfiguration.Predicates.NATURAL_STONE,
                    Blocks.DIAMOND_ORE.defaultBlockState(),
                    8,
                    0.5F
                )
            )
            .decorated(RangedConfiguration.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(-4)))
            .squared()
            .count(2)
    );
    public static final ConfiguredFeature<?, ?> ORE_DIAMOND_LARGE = FEATURES.register(
        "ore_diamond_large",
        PlatformFeatures.OVERLAY_ORE.get()
            .configured(
                new OverlayOreConfiguration(
                    OreConfiguration.Predicates.NATURAL_STONE,
                    Blocks.DIAMOND_ORE.defaultBlockState(),
                    12,
                    0.7F
                )
            )
            .decorated(RangedConfiguration.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80)))
            .squared()
            .count(9)
    );
    public static final ConfiguredFeature<?, ?> ORE_DIAMOND_BURIED = FEATURES.register(
        "ore_diamond_buried",
        PlatformFeatures.OVERLAY_ORE.get()
            .configured(
                new OverlayOreConfiguration(
                    OreConfiguration.Predicates.NATURAL_STONE,
                    Blocks.DIAMOND_ORE.defaultBlockState(),
                    8,
                    1.0F
                )
            )
            .decorated(RangedConfiguration.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80)))
            .squared()
            .count(4)
    );
    public static final ConfiguredFeature<?, ?> ORE_LAPIS = FEATURES.register(
        "ore_lapis",
        PlatformFeatures.OVERLAY_ORE.get()
            .configured(
                new OverlayOreConfiguration(
                    OreConfiguration.Predicates.NATURAL_STONE,
                    Blocks.LAPIS_ORE.defaultBlockState(),
                    7
                )
            )
            .decorated(RangedConfiguration.triangle(VerticalAnchor.absolute(-32), VerticalAnchor.absolute(32)))
            .squared()
            .count(2)
    );
    public static final ConfiguredFeature<?, ?> ORE_LAPIS_BURIED = FEATURES.register(
        "ore_lapis_buried",
        PlatformFeatures.OVERLAY_ORE.get()
            .configured(
                new OverlayOreConfiguration(
                    OreConfiguration.Predicates.NATURAL_STONE,
                    Blocks.LAPIS_ORE.defaultBlockState(),
                    7,
                    1.0F
                )
            )
            .decorated(RangedConfiguration.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(64)))
            .squared()
            .count(4)
    );
    public static final ConfiguredFeature<?, ?> ORE_EMERALD = FEATURES.register(
        "ore_emerald",
        PlatformFeatures.OVERLAY_ORE.get()
            .configured(
                new OverlayOreConfiguration(
                    OreConfiguration.Predicates.NATURAL_STONE,
                    Blocks.EMERALD_ORE.defaultBlockState(),
                    3
                )
            )
            .decorated(RangedConfiguration.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(480)))
            .squared()
            .count(100)
    );
    public static final ConfiguredFeature<?, ?> ORE_INFESTED = FEATURES.register(
        "ore_infested",
        PlatformFeatures.OVERLAY_ORE.get()
            .configured(
                new OverlayOreConfiguration(
                    OreConfiguration.Predicates.NATURAL_STONE,
                    Blocks.INFESTED_STONE.defaultBlockState(),
                    9
                )
            )
            .decorated(RangedConfiguration.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(63)))
            .squared()
            .count(14)
    );
}
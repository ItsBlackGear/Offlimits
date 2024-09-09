package com.blackgear.offlimits.core.registry;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.common.level.levelgen.features.ModLakeFeature;
import com.blackgear.platform.core.CoreRegistry;
import net.minecraft.core.Registry;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;

import java.util.function.Supplier;

public class ModFeatures {
    public static final CoreRegistry<Feature<?>> FEATURES = CoreRegistry.create(Registry.FEATURE, Offlimits.MOD_ID);
    
    public static final Supplier<Feature<BlockStateConfiguration>> LAKE = FEATURES.register("lake", () -> new ModLakeFeature(BlockStateConfiguration.CODEC));
}
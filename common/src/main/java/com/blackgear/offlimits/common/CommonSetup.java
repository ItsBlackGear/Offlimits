package com.blackgear.offlimits.common;

import com.blackgear.offlimits.common.level.levelgen.OreFeatures;
import com.blackgear.offlimits.common.level.levelgen.UndergroundFeatures;
import com.blackgear.platform.common.worldgen.modifier.BiomeContext;
import com.blackgear.platform.common.worldgen.modifier.BiomeManager;
import com.blackgear.platform.common.worldgen.modifier.BiomeWriter;
import com.blackgear.platform.core.ParallelDispatch;
import net.minecraft.data.worldgen.Features;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class CommonSetup {
    public static void postStartup(ParallelDispatch dispatch) {
        dispatch.enqueueWork(() -> {
            UndergroundFeatures.FEATURES.register();
            OreFeatures.FEATURES.register();
            
            BiomeManager.add((writer, context) -> {
                if (context.is(BiomeContext.OVERWORLD_BIOME)) {
                    checkAndReplace(writer, context, Decoration.LAKES, UndergroundFeatures.LAKE_WATER, Features.LAKE_WATER);
                    checkAndReplace(writer, context, Decoration.LAKES, UndergroundFeatures.LAKE_LAVA, Features.LAKE_LAVA);
                    checkAndReplace(writer, context, Decoration.VEGETAL_DECORATION, UndergroundFeatures.SPRING_WATER, Features.SPRING_WATER);
                    checkAndReplace(writer, context, Decoration.VEGETAL_DECORATION, UndergroundFeatures.SPRING_LAVA, Features.SPRING_LAVA);
                    
                    if (context.hasFeature(Features.ORE_COAL)) {
                        writer.addFeature(Decoration.UNDERGROUND_ORES, OreFeatures.ORE_COAL_UPPER);
                        writer.addFeature(Decoration.UNDERGROUND_ORES, OreFeatures.ORE_COAL_LOWER);
                        writer.removeFeature(Decoration.UNDERGROUND_ORES, Features.ORE_COAL);
                    }
                    
                    if (context.hasFeature(Features.ORE_IRON)) {
                        writer.addFeature(Decoration.UNDERGROUND_ORES, OreFeatures.ORE_IRON_UPPER);
                        writer.addFeature(Decoration.UNDERGROUND_ORES, OreFeatures.ORE_IRON_MIDDLE);
                        writer.addFeature(Decoration.UNDERGROUND_ORES, OreFeatures.ORE_IRON_SMALL);
                        writer.removeFeature(Decoration.UNDERGROUND_ORES, Features.ORE_IRON);
                    }
                    
                    checkAndReplace(writer, context, Decoration.UNDERGROUND_ORES, OreFeatures.ORE_GOLD_EXTRA, Features.ORE_GOLD_EXTRA);
                    
                    if (context.hasFeature(Features.ORE_GOLD)) {
                        writer.addFeature(Decoration.UNDERGROUND_ORES, OreFeatures.ORE_GOLD);
                        writer.addFeature(Decoration.UNDERGROUND_ORES, OreFeatures.ORE_GOLD_LOWER);
                        writer.removeFeature(Decoration.UNDERGROUND_ORES, Features.ORE_GOLD);
                    }
                    
                    if (context.hasFeature(Features.ORE_REDSTONE)) {
                        writer.addFeature(Decoration.UNDERGROUND_ORES, OreFeatures.ORE_REDSTONE);
                        writer.addFeature(Decoration.UNDERGROUND_ORES, OreFeatures.ORE_REDSTONE_LOWER);
                        writer.removeFeature(Decoration.UNDERGROUND_ORES, Features.ORE_REDSTONE);
                    }
                    
                    if (context.hasFeature(Features.ORE_DIAMOND)) {
                        writer.addFeature(Decoration.UNDERGROUND_ORES, OreFeatures.ORE_DIAMOND);
                        writer.addFeature(Decoration.UNDERGROUND_ORES, OreFeatures.ORE_DIAMOND_MEDIUM);
                        writer.addFeature(Decoration.UNDERGROUND_ORES, OreFeatures.ORE_DIAMOND_LARGE);
                        writer.addFeature(Decoration.UNDERGROUND_ORES, OreFeatures.ORE_DIAMOND_BURIED);
                        writer.removeFeature(Decoration.UNDERGROUND_ORES, Features.ORE_DIAMOND);
                    }
                    
                    if (context.hasFeature(Features.ORE_LAPIS)) {
                        writer.addFeature(Decoration.UNDERGROUND_ORES, OreFeatures.ORE_LAPIS);
                        writer.addFeature(Decoration.UNDERGROUND_ORES, OreFeatures.ORE_LAPIS_BURIED);
                        writer.removeFeature(Decoration.UNDERGROUND_ORES, Features.ORE_LAPIS);
                    }
                    
                    if (context.hasFeature(Features.ORE_EMERALD)) {
                        writer.addFeature(Decoration.UNDERGROUND_ORES, OreFeatures.ORE_EMERALD);
                        writer.removeFeature(Decoration.UNDERGROUND_ORES, Features.ORE_EMERALD);
                    }
                    
                    if (context.hasFeature(Features.ORE_INFESTED)) {
                        writer.addFeature(Decoration.UNDERGROUND_ORES, OreFeatures.ORE_INFESTED);
                        writer.removeFeature(Decoration.UNDERGROUND_ORES, Features.ORE_INFESTED);
                    }
                }
            });
        });
    }
    
    private static void checkAndReplace(BiomeWriter writer, BiomeContext context, Decoration decoration, ConfiguredFeature<?, ?> replacement, ConfiguredFeature<?, ?> overriden) {
        if (context.hasFeature(overriden)) {
            writer.replaceFeature(decoration, replacement, overriden);
        }
    }
}
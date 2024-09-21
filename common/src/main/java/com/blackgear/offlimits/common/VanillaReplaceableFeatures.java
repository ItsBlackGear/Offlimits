package com.blackgear.offlimits.common;

import com.blackgear.offlimits.common.level.levelgen.OreFeatureReplacements;
import com.blackgear.offlimits.common.level.levelgen.UndergroundFeatureReplacements;
import com.blackgear.platform.common.worldgen.modifier.BiomeContext;
import com.blackgear.platform.common.worldgen.modifier.BiomeWriter;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.data.worldgen.Features;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;

public class VanillaReplaceableFeatures {
    public static void addDefaultCarvers(BiomeWriter writer, BiomeContext context) {
        if (context.is(Biome.BiomeCategory.OCEAN)) {
            writer.removeCarver(GenerationStep.Carving.LIQUID, Carvers.UNDERWATER_CAVE);
            writer.removeCarver(GenerationStep.Carving.LIQUID, Carvers.UNDERWATER_CANYON);
            writer.addCarver(GenerationStep.Carving.LIQUID, Carvers.CAVE);
        }
    
        writer.removeCarver(GenerationStep.Carving.AIR, Carvers.CANYON);
    }
    
    public static void addMonsterRoom(BiomeWriter writer, BiomeContext context) {
        if (context.hasFeature(Features.MONSTER_ROOM)) {
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_STRUCTURES, UndergroundFeatureReplacements.MONSTER_ROOM);
            writer.removeFeature(GenerationStep.Decoration.UNDERGROUND_STRUCTURES, Features.MONSTER_ROOM);
        }
    }
    
    public static void addDefaultLakes(BiomeWriter writer, BiomeContext context) {
        if (context.hasFeature(Features.LAKE_WATER)) {
            writer.addFeature(GenerationStep.Decoration.LAKES, UndergroundFeatureReplacements.LAKE_WATER);
            writer.removeFeature(GenerationStep.Decoration.LAKES, Features.LAKE_WATER);
        }
        
        if (context.hasFeature(Features.LAKE_LAVA)) {
            writer.addFeature(GenerationStep.Decoration.LAKES, UndergroundFeatureReplacements.LAKE_LAVA);
            writer.removeFeature(GenerationStep.Decoration.LAKES, Features.LAKE_LAVA);
        }
    }
    
    public static void addDefaultSprings(BiomeWriter writer, BiomeContext context) {
        if (context.hasFeature(Features.SPRING_WATER)) {
            writer.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, UndergroundFeatureReplacements.SPRING_WATER);
            writer.removeFeature(GenerationStep.Decoration.VEGETAL_DECORATION, Features.SPRING_WATER);
        }
        
        if (context.hasFeature(Features.SPRING_LAVA)) {
            writer.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, UndergroundFeatureReplacements.SPRING_LAVA);
            writer.removeFeature(GenerationStep.Decoration.VEGETAL_DECORATION, Features.SPRING_LAVA);
        }
    }
    
    public static void addDefaultOres(BiomeWriter writer, BiomeContext context) {
        if (context.hasFeature(Features.ORE_COAL)) {
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OreFeatureReplacements.ORE_COAL_UPPER);
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OreFeatureReplacements.ORE_COAL_LOWER);
            writer.removeFeature(GenerationStep.Decoration.UNDERGROUND_ORES, Features.ORE_COAL);
        }
        
        if (context.hasFeature(Features.ORE_IRON)) {
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OreFeatureReplacements.ORE_IRON_UPPER);
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OreFeatureReplacements.ORE_IRON_MIDDLE);
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OreFeatureReplacements.ORE_IRON_SMALL);
            writer.removeFeature(GenerationStep.Decoration.UNDERGROUND_ORES, Features.ORE_IRON);
        }
        
        if (context.hasFeature(Features.ORE_GOLD_EXTRA)) {
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OreFeatureReplacements.ORE_GOLD_EXTRA);
            writer.removeFeature(GenerationStep.Decoration.UNDERGROUND_ORES, Features.ORE_GOLD_EXTRA);
        }
        
        if (context.hasFeature(Features.ORE_GOLD)) {
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OreFeatureReplacements.ORE_GOLD);
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OreFeatureReplacements.ORE_GOLD_LOWER);
            writer.removeFeature(GenerationStep.Decoration.UNDERGROUND_ORES, Features.ORE_GOLD);
        }
        
        if (context.hasFeature(Features.ORE_REDSTONE)) {
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OreFeatureReplacements.ORE_REDSTONE);
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OreFeatureReplacements.ORE_REDSTONE_LOWER);
            writer.removeFeature(GenerationStep.Decoration.UNDERGROUND_ORES, Features.ORE_REDSTONE);
        }
        
        if (context.hasFeature(Features.ORE_DIAMOND)) {
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OreFeatureReplacements.ORE_DIAMOND);
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OreFeatureReplacements.ORE_DIAMOND_MEDIUM);
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OreFeatureReplacements.ORE_DIAMOND_LARGE);
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OreFeatureReplacements.ORE_DIAMOND_BURIED);
            writer.removeFeature(GenerationStep.Decoration.UNDERGROUND_ORES, Features.ORE_DIAMOND);
        }
        
        if (context.hasFeature(Features.ORE_LAPIS)) {
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OreFeatureReplacements.ORE_LAPIS);
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OreFeatureReplacements.ORE_LAPIS_BURIED);
            writer.removeFeature(GenerationStep.Decoration.UNDERGROUND_ORES, Features.ORE_LAPIS);
        }
        
        if (context.hasFeature(Features.ORE_EMERALD)) {
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OreFeatureReplacements.ORE_EMERALD);
            writer.removeFeature(GenerationStep.Decoration.UNDERGROUND_ORES, Features.ORE_EMERALD);
        }
        
        if (context.hasFeature(Features.ORE_INFESTED)) {
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OreFeatureReplacements.ORE_INFESTED);
            writer.removeFeature(GenerationStep.Decoration.UNDERGROUND_ORES, Features.ORE_INFESTED);
        }
    }
    
    public static void addDefaultStonesAndSoils(BiomeWriter writer, BiomeContext context) {
        if (context.hasFeature(Features.ORE_DIRT)) {
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, UndergroundFeatureReplacements.ORE_DIRT_LOWER);
        }
        
        if (context.hasFeature(Features.ORE_GRAVEL)) {
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, UndergroundFeatureReplacements.ORE_GRAVEL_LOWER);
        }
        
        if (context.hasFeature(Features.ORE_GRANITE)) {
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, UndergroundFeatureReplacements.ORE_GRANITE_LOWER);
        }
        
        if (context.hasFeature(Features.ORE_DIORITE)) {
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, UndergroundFeatureReplacements.ORE_DIORITE_LOWER);
        }
        
        if (context.hasFeature(Features.ORE_ANDESITE)) {
            writer.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, UndergroundFeatureReplacements.ORE_ANDESITE_LOWER);
        }
    }
}
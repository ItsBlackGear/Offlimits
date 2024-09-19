package com.blackgear.offlimits.common;

import com.blackgear.platform.common.worldgen.modifier.BiomeManager;
import net.minecraft.world.level.biome.Biome;

public class WorldGeneration {
    public static void bootstrap() {
        BiomeManager.add((writer, context) -> {
            if (!context.is(Biome.BiomeCategory.NETHER) && !context.is(Biome.BiomeCategory.THEEND)) {
//            if (context.is(BiomeContext.OVERWORLD_BIOME)) {
                VanillaReplaceableFeatures.addDefaultLakes(writer, context);
                VanillaReplaceableFeatures.addDefaultSprings(writer, context);
                VanillaReplaceableFeatures.addDefaultOres(writer, context);
                VanillaReplaceableFeatures.addMonsterRoom(writer, context);
                VanillaReplaceableFeatures.addDefaultCarvers(writer, context);
            }
        });
    }
}

package com.blackgear.offlimits.common;

import com.blackgear.platform.common.worldgen.modifier.BiomeContext;
import com.blackgear.platform.common.worldgen.modifier.BiomeManager;

public class WorldGeneration {
    public static void bootstrap() {
        BiomeManager.add((writer, context) -> {
            if (context.is(BiomeContext.OVERWORLD_BIOME)) {
                VanillaReplaceableFeatures.addDefaultLakes(writer, context);
                VanillaReplaceableFeatures.addDefaultSprings(writer, context);
                VanillaReplaceableFeatures.addDefaultOres(writer, context);
                VanillaReplaceableFeatures.addMonsterRoom(writer, context);
                VanillaReplaceableFeatures.addDefaultCarvers(writer, context);
            }
        });
    }
}

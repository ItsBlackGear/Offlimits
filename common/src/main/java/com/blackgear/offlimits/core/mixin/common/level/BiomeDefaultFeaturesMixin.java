package com.blackgear.offlimits.core.mixin.common.level;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BiomeDefaultFeatures.class)
public class BiomeDefaultFeaturesMixin {
    @Inject(
        method = "addOceanCarvers",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void offlimits$addOceanCarvers(BiomeGenerationSettings.Builder builder, CallbackInfo callback) {
        if (Offlimits.CONFIG.allowTerrainModifications.get()) {
            builder.addCarver(GenerationStep.Carving.AIR, Carvers.CAVE);
            builder.addCarver(GenerationStep.Carving.AIR, Carvers.CANYON);
            callback.cancel();
        }
    }
}
package com.blackgear.offlimits.core.mixin.common.level;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.StructureSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NoiseGeneratorSettings.class)
public interface NoiseGeneratorSettingsAccessor {
    @Invoker("<init>")
    static NoiseGeneratorSettings createNoiseGeneratorSettings(
        StructureSettings structureSettings, NoiseSettings noiseSettings, BlockState blockState, BlockState blockState2, int i, int j, int k, boolean bl
    ) {
        throw new UnsupportedOperationException();
    }
}

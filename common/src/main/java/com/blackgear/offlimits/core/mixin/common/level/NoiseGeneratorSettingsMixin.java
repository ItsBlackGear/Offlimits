package com.blackgear.offlimits.core.mixin.common.level;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.common.level.noise.NoiseGeneratorSettingsExtension;
import com.blackgear.offlimits.common.level.noise.NoiseSettingsExtension;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NoiseGeneratorSettings.class)
public class NoiseGeneratorSettingsMixin implements NoiseGeneratorSettingsExtension {
    @Unique private boolean aquifersEnabled = false;
    @Unique private boolean noiseCavesEnabled = false;
    @Unique private boolean deepslateEnabled = false;
    @Unique private boolean oreVeinsEnabled = false;
    @Unique private boolean noodleCavesEnabled = false;
    
    @Override public boolean aquifersEnabled() { return this.aquifersEnabled; }
    @Override public void setAquifersEnabled(boolean enabled) { this.aquifersEnabled = enabled; }
    @Override public boolean noiseCavesEnabled() { return this.noiseCavesEnabled; }
    @Override public void setNoiseCavesEnabled(boolean enabled) { this.noiseCavesEnabled = enabled; }
    @Override public boolean deepslateEnabled() { return this.deepslateEnabled; }
    @Override public void setDeepslateEnabled(boolean enabled) { this.deepslateEnabled = enabled; }
    @Override public boolean oreVeinsEnabled() { return this.oreVeinsEnabled; }
    @Override public void setOreVeinsEnabled(boolean enabled) { this.oreVeinsEnabled = enabled; }
    @Override public boolean noodleCavesEnabled() { return this.noodleCavesEnabled; }
    @Override public void setNoodleCavesEnabled(boolean enabled) { this.noodleCavesEnabled = enabled; }
    
    @Redirect(
        method = "overworld",
        at = @At(
            value = "NEW",
            target = "(Lnet/minecraft/world/level/levelgen/StructureSettings;Lnet/minecraft/world/level/levelgen/NoiseSettings;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;IIIZ)Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;"
        )
    )
    private static NoiseGeneratorSettings off$addOverworld(
        StructureSettings structures,
        NoiseSettings noise,
        BlockState defaultBlock,
        BlockState defaultFluid,
        int bedrockRoofPosition,
        int bedrockFloorPosition,
        int seaLevel,
        boolean disableMobGeneration
    ) {
        NoiseGeneratorSettings settings = NoiseGeneratorSettingsAccessor.createNoiseGeneratorSettings(
            structures,
            noise,
            defaultBlock,
            defaultFluid,
            bedrockRoofPosition,
            bedrockFloorPosition,
            seaLevel,
            disableMobGeneration
        );
        
        if (settings instanceof NoiseGeneratorSettingsExtension) {
            NoiseGeneratorSettingsExtension extension = (NoiseGeneratorSettingsExtension) settings;
            extension.setAquifersEnabled(Offlimits.CONFIG.areAquifersEnabled.get());
            extension.setNoiseCavesEnabled(Offlimits.CONFIG.areNoiseCavesEnabled.get());
            extension.setDeepslateEnabled(true);
            extension.setOreVeinsEnabled(true);
            extension.setNoodleCavesEnabled(Offlimits.CONFIG.areNoiseCavesEnabled.get());
        }
        
        return settings;
    }
    
    @Redirect(
        method = "overworld",
        at = @At(
            value = "NEW",
            target = "(ILnet/minecraft/world/level/levelgen/NoiseSamplingSettings;Lnet/minecraft/world/level/levelgen/NoiseSlideSettings;Lnet/minecraft/world/level/levelgen/NoiseSlideSettings;IIDDZZZZ)Lnet/minecraft/world/level/levelgen/NoiseSettings;"
        )
    )
    private static NoiseSettings off$increaseOverworldHeight(
        int height,
        NoiseSamplingSettings sampling,
        NoiseSlideSettings topSlide,
        NoiseSlideSettings bottomSlide,
        int sizeHorizontal,
        int sizeVertical,
        double densityFactor,
        double densityOffset,
        boolean simplexSurfaceNoise,
        boolean randomDensityOffset,
        boolean islandNoiseOverride,
        boolean amplified
    ) {
        NoiseSettings settings = new NoiseSettings(
            height,
            sampling,
            topSlide,
            bottomSlide,
            sizeHorizontal,
            sizeVertical,
            densityFactor,
            densityOffset,
            simplexSurfaceNoise,
            randomDensityOffset,
            islandNoiseOverride,
            amplified
        );
        
        NoiseSettingsExtension extension = (NoiseSettingsExtension) settings;
        extension.setMinY(Offlimits.CONFIG.worldGenMinY.get());
        extension.setHeight(Offlimits.LEVEL.getHeight());
        
        return settings;
    }
}
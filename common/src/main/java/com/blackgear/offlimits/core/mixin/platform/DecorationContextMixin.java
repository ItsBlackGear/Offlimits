package com.blackgear.offlimits.core.mixin.platform;

import com.blackgear.offlimits.common.level.chunk.noise.NoiseSettingsExtension;
import com.blackgear.offlimits.core.mixin.common.level.NoiseBasedChunkGeneratorAccessor;
import com.blackgear.platform.common.worldgen.height.HeightHolder;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.placement.DecorationContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DecorationContext.class)
public abstract class DecorationContextMixin implements HeightHolder {
    @Shadow public abstract int getGenDepth();
    
    @Shadow @Final private ChunkGenerator generator;
    
    @Override
    public int minY() {
        if (this.generator instanceof NoiseBasedChunkGenerator) {
            NoiseSettings settings = ((NoiseBasedChunkGeneratorAccessor) this.generator).getSettings().get().noiseSettings();
            return ((NoiseSettingsExtension) settings).minY();
        }
        
        return 0;
    }
    
    @Override
    public int genDepth() {
        return this.getGenDepth();
    }
}
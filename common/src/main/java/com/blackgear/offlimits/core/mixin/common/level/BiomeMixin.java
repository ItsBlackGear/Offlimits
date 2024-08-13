package com.blackgear.offlimits.core.mixin.common.level;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.common.level.surface.BiomeExtension;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Biome.class)
public class BiomeMixin implements BiomeExtension {
    @Unique private int preliminarySurfaceLevel;
    
    @ModifyConstant(
        method = "shouldSnow",
        constant = @Constant(intValue = 256)
    )
    private int offlimits$shouldSnow(int original) {
        return Offlimits.INSTANCE.getMaxBuildHeight();
    }
    
    @ModifyConstant(
        method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z",
        constant = @Constant(intValue = 256)
    )
    private int offlimits$shouldFreeze(int original) {
        return Offlimits.INSTANCE.getMaxBuildHeight();
    }
    
    @Override
    public int getPreliminarySurfaceLevel() {
        return this.preliminarySurfaceLevel;
    }
    
    @Override
    public void setPreliminarySurfaceLevel(int level) {
        this.preliminarySurfaceLevel = level;
    }
}
package com.blackgear.offlimits.mixin.common.level;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Biome.class)
public class BiomeMixin {
    @ModifyConstant(
        method = "shouldSnow",
        constant = @Constant(intValue = 256)
    )
    private int offlimits$shouldSnowMax(int original) {
        return Offlimits.INSTANCE.getMaxBuildHeight();
    }
    
    @ModifyConstant(
        method = "shouldSnow",
        constant = @Constant(intValue = 0)
    )
    private int offlimits$shouldSnowMin(int original) {
        return Offlimits.INSTANCE.getMinBuildHeight();
    }
    
    @ModifyConstant(
        method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z",
        constant = @Constant(intValue = 256)
    )
    private int offlimits$shouldFreezeMax(int original) {
        return Offlimits.INSTANCE.getMaxBuildHeight();
    }
    
    @ModifyConstant(
        method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z",
        constant = @Constant(intValue = 0)
    )
    private int offlimits$shouldFreezeMin(int original) {
        return Offlimits.INSTANCE.getMinBuildHeight();
    }
}
package com.blackgear.offlimits.core.mixin.platform;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.platform.common.worldgen.WorldGenerationContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(WorldGenerationContext.class)
public class WorldGenerationContextMixin {
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 256))
    private int off$modifyHeight(int original) {
        return Offlimits.LEVEL.getHeight();
    }
    
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 0))
    private int off$modifyMinBuildHeight(int original) {
        return Offlimits.LEVEL.getMinBuildHeight();
    }
}
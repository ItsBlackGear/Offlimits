package com.blackgear.offlimits.core.mixin.client;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @ModifyConstant(
        method = "getRelativeFrom",
        constant = @Constant(intValue = 256)
    )
    private int offlimits$getRelativeFrom(int original) {
        return Offlimits.INSTANCE.getMaxBuildHeight();
    }
    
    @ModifyConstant(
        method = "renderWorldBounds",
        constant = @Constant(intValue = 256),
        expect = 8
    )
    private int offlimits$renderWorldBounds(int original) {
        return Offlimits.INSTANCE.getMaxBuildHeight();
    }
}
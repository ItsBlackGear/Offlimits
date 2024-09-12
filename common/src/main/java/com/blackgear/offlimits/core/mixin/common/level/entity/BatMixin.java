package com.blackgear.offlimits.core.mixin.common.level.entity;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.world.entity.ambient.Bat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Bat.class)
public class BatMixin {
    @ModifyConstant(
        method = "customServerAiStep",
        constant = @Constant(intValue = 1)
    )
    private int offlimits$getTargetPositionMinHeight(int constant) {
        return Offlimits.LEVEL.getMinBuildHeight();
    }
}
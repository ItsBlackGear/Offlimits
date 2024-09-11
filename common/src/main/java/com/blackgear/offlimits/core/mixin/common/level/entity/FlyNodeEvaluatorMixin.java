package com.blackgear.offlimits.core.mixin.common.level.entity;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(FlyNodeEvaluator.class)
public class FlyNodeEvaluatorMixin {
    @ModifyConstant(
        method = "getBlockPathType(Lnet/minecraft/world/level/BlockGetter;III)Lnet/minecraft/world/level/pathfinder/BlockPathTypes;",
        constant = @Constant(intValue = 1, ordinal = 0)
    )
    private int off$updateFlyingMinRange(int constant) {
        return Offlimits.LEVEL.getMinBuildHeight() + 1;
    }
}
package com.blackgear.offlimits.core.mixin.common.level.entity;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Entity.class)
public class EntityMixin {
    @ModifyConstant(
        method = "baseTick",
        constant = @Constant(doubleValue = -64.0)
    )
    private double off$baseTick(double constant) {
        return Offlimits.LEVEL.getMinBuildHeight() - 64.0;
    }
}
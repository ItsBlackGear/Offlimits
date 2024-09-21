package com.blackgear.offlimits.core.mixin.common.level.portal;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.world.level.portal.PortalShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PortalShape.class)
public class PortalShapeMixin {
    @ModifyConstant(
        method = "calculateBottomLeft",
        constant = @Constant(intValue = 0, ordinal = 0)
    )
    private int offlimits$updateMinHeight(int constant) {
        return Offlimits.LEVEL.getMinBuildHeight();
    }
}
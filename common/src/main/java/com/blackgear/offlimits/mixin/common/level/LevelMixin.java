package com.blackgear.offlimits.mixin.common.level;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Level.class)
public class LevelMixin {
    @ModifyConstant(
        method = "isOutsideBuildHeight(I)Z",
        constant = @Constant(intValue = 256)
    )
    private static int offlimits$isOutsideBuildHeight(int original) {
        return Offlimits.INSTANCE.getMaxBuildHeight();
    }
}
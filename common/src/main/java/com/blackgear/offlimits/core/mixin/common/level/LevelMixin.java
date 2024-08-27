package com.blackgear.offlimits.core.mixin.common.level;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Level.class)
public class LevelMixin {
//    @ModifyConstant(
//        method = "isOutsideBuildHeight(I)Z",
//        constant = @Constant(intValue = 256)
//    )
//    private static int off$isOutsideBuildHeightMax(int original) {
//        return Offlimits.INSTANCE.getMaxBuildHeight();
//    }
//
//    @ModifyConstant(
//        method = "isOutsideBuildHeight(I)Z",
//        constant = @Constant(intValue = 0)
//    )
//    private static int off$isOutsideBuildHeightMin(int original) {
//        return Offlimits.INSTANCE.getMinBuildHeight();
//    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    public static boolean isOutsideBuildHeight(int i) {
        return i < Offlimits.INSTANCE.getMinBuildHeight() || i >= Offlimits.INSTANCE.getMaxBuildHeight();
    }
}
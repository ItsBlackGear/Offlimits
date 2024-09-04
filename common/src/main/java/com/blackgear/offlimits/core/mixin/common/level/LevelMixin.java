package com.blackgear.offlimits.core.mixin.common.level;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Level.class)
public class LevelMixin {
    @ModifyConstant(
        method = "getHeight",
        constant = @Constant(intValue = 0)
    )
    private int off$getMinBuildHeightFromHeightmap(int constant) {
        return Offlimits.LEVEL.getMinBuildHeight();
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    public static boolean isOutsideBuildHeight(int i) {
        return i < Offlimits.LEVEL.getMinBuildHeight() || i >= Offlimits.LEVEL.getMaxBuildHeight();
    }
}
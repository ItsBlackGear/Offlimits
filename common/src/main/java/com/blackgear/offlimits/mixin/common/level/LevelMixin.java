package com.blackgear.offlimits.mixin.common.level;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Level.class)
public class LevelMixin {
    /**
     * @author ItsBlackGear
     * @reason TODO; too lazy to figure this out right now...
     */
    @Overwrite
    public static boolean isOutsideBuildHeight(int y) {
        return y < Offlimits.INSTANCE.getMinBuildHeight() || y >= Offlimits.INSTANCE.getMaxBuildHeight();
    }
}
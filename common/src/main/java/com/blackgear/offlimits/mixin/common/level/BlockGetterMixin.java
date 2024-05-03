package com.blackgear.offlimits.mixin.common.level;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BlockGetter.class)
public interface BlockGetterMixin {
    /**
     * @author ItsBlackGear
     * @reason we cannot modify the constant in an interface.
     */
    @Overwrite
    default int getMaxBuildHeight() {
        return Offlimits.INSTANCE.getMaxBuildHeight();
    }
}
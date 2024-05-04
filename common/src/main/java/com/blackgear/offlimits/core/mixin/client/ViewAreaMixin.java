package com.blackgear.offlimits.core.mixin.client;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.client.renderer.ViewArea;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

//TODO
@Mixin(ViewArea.class)
public class ViewAreaMixin {
    @ModifyConstant(
        method = "setViewDistance",
        constant = @Constant(intValue = 16)
    )
    private int setViewDistance(int original) {
        return Offlimits.INSTANCE.getSectionsCount();
//        return Math.min(Math.max(original, this.chunkGridSizeX), 32);
    }
}
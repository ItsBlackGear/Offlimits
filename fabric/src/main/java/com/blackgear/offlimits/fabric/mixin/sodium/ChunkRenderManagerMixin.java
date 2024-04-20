package com.blackgear.offlimits.fabric.mixin.sodium;

import com.blackgear.offlimits.Offlimits;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Pseudo @Mixin(ChunkRenderManager.class)
public class ChunkRenderManagerMixin {
    @ModifyConstant(
        method = "loadSections",
        constant = @Constant(intValue = 16),
        remap = false
    )
    private int offlimits$loadSections(int original) {
        return Offlimits.INSTANCE.getMaxSection();
    }

    @ModifyConstant(
        method = "unloadSections",
        constant = @Constant(intValue = 16),
        remap = false
    )
    private int offlimits$unloadSections(int original) {
        return Offlimits.INSTANCE.getMaxSection();
    }

    @ModifyConstant(
        method = "getTotalSections",
        constant = @Constant(intValue = 16),
        remap = false
    )
    private int offlimits$getTotalSections(int original) {
        return Offlimits.INSTANCE.getSectionsCount();
    }
}
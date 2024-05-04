package com.blackgear.offlimits.core.fabric.mixin.sodium;

import com.blackgear.offlimits.Offlimits;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderColumn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Pseudo @Mixin(ChunkRenderColumn.class)
public class ChunkRenderColumnMixin {
     @ModifyConstant(
         method = "<init>",
         constant = @Constant(intValue = 16),
         remap = false
     )
     private int offlimits$init(int original) {
         return Offlimits.INSTANCE.getSectionsCount();
     }
}
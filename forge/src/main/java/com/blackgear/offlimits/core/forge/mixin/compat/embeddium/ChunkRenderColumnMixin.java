package com.blackgear.offlimits.core.forge.mixin.compat.embeddium;

import com.blackgear.offlimits.Offlimits;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderColumn;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo @Mixin(ChunkRenderColumn.class)
public class ChunkRenderColumnMixin<T extends ChunkGraphicsState> {
    @Shadow @Final private ChunkRenderContainer<T>[] renders;
    
    @ModifyConstant(
        method = "<init>",
        constant = @Constant(intValue = 16),
        remap = false
    )
    private int offlimits$updateRenderSections(int original) {
        return Offlimits.LEVEL.getSectionsCount();
    }
    
    @Inject(
        method = "setRender",
        at = @At("HEAD"),
        remap = false,
        cancellable = true
    )
    private void offlimits$setRender(int y, ChunkRenderContainer<T> render, CallbackInfo ci) {
        int yIndex = Offlimits.LEVEL.getSectionIndex(y);
        this.renders[yIndex] = render;
        ci.cancel();
    }
    
    @Inject(
        method = "getRender",
        at = @At("HEAD"),
        remap = false,
        cancellable = true
    )
    public void offlimits$getRender(int y, CallbackInfoReturnable<ChunkRenderContainer<T>> cir) {
        int yIndex = Offlimits.LEVEL.getSectionIndex(y);
        
        if (yIndex < 0 || yIndex >= this.renders.length) {
            cir.setReturnValue(null);
        }
        
        cir.setReturnValue(this.renders[yIndex]);
    }
}
package com.blackgear.offlimits.core.mixin.client;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @ModifyConstant(
        method = "getRelativeFrom",
        constant = @Constant(intValue = 256)
    )
    private int offlimits$getRelativeFrom(int original) {
        return Offlimits.INSTANCE.getMaxBuildHeight();
    }
    
    @Inject(
        method = "getRelativeFrom",
        at = @At("HEAD"),
        cancellable = true
    )
    private void off$getRelativeFrom(BlockPos playerPos, ChunkRenderDispatcher.RenderChunk renderChunkBase, Direction facing, CallbackInfoReturnable<ChunkRenderDispatcher.RenderChunk> cir) {
        BlockPos pos = renderChunkBase.getRelativeOrigin(facing);
        if (pos.getY() < Offlimits.INSTANCE.getMinBuildHeight() || pos.getY() >= Offlimits.INSTANCE.getMaxBuildHeight()) {
            cir.setReturnValue(null);
        }
    }
    
    @ModifyConstant(
        method = "renderWorldBounds",
        constant = @Constant(intValue = 256),
        expect = 8
    )
    private int off$renderWorldBounds(int original) {
        return Offlimits.INSTANCE.getMaxBuildHeight();
    }
}
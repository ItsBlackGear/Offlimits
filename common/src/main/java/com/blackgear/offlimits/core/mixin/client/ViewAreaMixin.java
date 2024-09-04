package com.blackgear.offlimits.core.mixin.client;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ViewArea.class)
public abstract class ViewAreaMixin {
    @Shadow protected int chunkGridSizeX;
    
    @Shadow protected int chunkGridSizeY;
    
    @Shadow protected int chunkGridSizeZ;
    
    @Shadow public ChunkRenderDispatcher.RenderChunk[] chunks;
    
    @Shadow protected abstract int getChunkIndex(int x, int y, int z);
    
    @ModifyConstant(
        method = "setViewDistance",
        constant = @Constant(intValue = 16)
    )
    private int off$setViewDistance(int original) {
        return Offlimits.LEVEL.getSectionsCount();
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    public void repositionCamera(double viewEntityX, double viewEntityZ) {
        int i = Mth.floor(viewEntityX);
        int j = Mth.floor(viewEntityZ);
        
        for(int k = 0; k < this.chunkGridSizeX; ++k) {
            int l = this.chunkGridSizeX * 16;
            int m = i - 8 - l / 2;
            int n = m + Math.floorMod(k * 16 - m, l);
            
            for(int o = 0; o < this.chunkGridSizeZ; ++o) {
                int p = this.chunkGridSizeZ * 16;
                int q = j - 8 - p / 2;
                int r = q + Math.floorMod(o * 16 - q, p);
                
                for(int s = 0; s < this.chunkGridSizeY; ++s) {
                    int t = Offlimits.LEVEL.getMinBuildHeight() + s * 16;
                    ChunkRenderDispatcher.RenderChunk renderChunk = this.chunks[this.getChunkIndex(k, s, o)];
                    renderChunk.setOrigin(n, t, r);
                }
            }
        }
    }
    
    @Inject(
        method = "setDirty",
        at = @At("HEAD"),
        cancellable = true
    )
    private void off$setDirty(int sectionX, int sectionY, int sectionZ, boolean rerenderOnMainThread, CallbackInfo ci) {
        int x = Math.floorMod(sectionX, this.chunkGridSizeX);
        int y = Math.floorMod(sectionY - Offlimits.LEVEL.getMinSection(), this.chunkGridSizeY);
        int z = Math.floorMod(sectionZ, this.chunkGridSizeZ);
        ChunkRenderDispatcher.RenderChunk renderChunk = this.chunks[this.getChunkIndex(x, y, z)];
        renderChunk.setDirty(rerenderOnMainThread);
        ci.cancel();
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    public @Nullable ChunkRenderDispatcher.RenderChunk getRenderChunkAt(BlockPos blockPos) {
        int i = Mth.intFloorDiv(blockPos.getX(), 16);
        int j = Mth.intFloorDiv(blockPos.getY() - Offlimits.LEVEL.getMinBuildHeight(), 16);
        int k = Mth.intFloorDiv(blockPos.getZ(), 16);
        if (j >= 0 && j < this.chunkGridSizeY) {
            i = Mth.positiveModulo(i, this.chunkGridSizeX);
            k = Mth.positiveModulo(k, this.chunkGridSizeZ);
            return this.chunks[this.getChunkIndex(i, j, k)];
        } else {
            return null;
        }
    }
}
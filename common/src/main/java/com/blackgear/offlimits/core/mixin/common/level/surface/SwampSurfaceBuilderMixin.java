package com.blackgear.offlimits.core.mixin.common.level.surface;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.common.level.chunk.surface.BiomeExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.surfacebuilders.SurfaceBuilder;
import net.minecraft.world.level.levelgen.surfacebuilders.SurfaceBuilderBaseConfiguration;
import net.minecraft.world.level.levelgen.surfacebuilders.SwampSurfaceBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(SwampSurfaceBuilder.class)
public class SwampSurfaceBuilderMixin {
    @Inject(
        method = "apply(Ljava/util/Random;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/biome/Biome;IIIDLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;IJLnet/minecraft/world/level/levelgen/surfacebuilders/SurfaceBuilderBaseConfiguration;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    public void offlimits$apply(
        Random random,
        ChunkAccess chunk,
        Biome biome,
        int x,
        int z,
        int startHeight,
        double surfaceNoise,
        BlockState defaultBlock,
        BlockState defaultFluid,
        int seaLevel,
        long seed,
        SurfaceBuilderBaseConfiguration config,
        CallbackInfo callback
    ) {
        if (Offlimits.CONFIG.allowTerrainModifications.get()) {
            double noise = Biome.BIOME_INFO_NOISE.getValue((double)x * 0.25, (double)z * 0.25, false);
            
            if (noise > 0.0) {
                int localX = x & 15;
                int localZ = z & 15;
                BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
                
                for(int localY = startHeight; localY >= ((BiomeExtension) biome).getMinSurfaceLevel(); --localY) {
                    mutable.set(localX, localY, localZ);
                    
                    if (!chunk.getBlockState(mutable).isAir()) {
                        if (localY == 62 && !chunk.getBlockState(mutable).is(defaultFluid.getBlock())) {
                            chunk.setBlockState(mutable, defaultFluid, false);
                        }
                        
                        break;
                    }
                }
            }
            
            SurfaceBuilder.DEFAULT.apply(random, chunk, biome, x, z, startHeight, surfaceNoise, defaultBlock, defaultFluid, seaLevel, seed, config);
            callback.cancel();
        }
    }
}

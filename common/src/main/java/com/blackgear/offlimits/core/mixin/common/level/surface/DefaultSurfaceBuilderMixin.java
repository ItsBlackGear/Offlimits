package com.blackgear.offlimits.core.mixin.common.level.surface;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.common.level.surface.BiomeExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.surfacebuilders.DefaultSurfaceBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(DefaultSurfaceBuilder.class)
public class DefaultSurfaceBuilderMixin {
    @Inject(
        method = "apply(Ljava/util/Random;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/biome/Biome;IIIDLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;I)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void offlimits$apply(
        Random random,
        ChunkAccess chunk,
        Biome biome,
        int x,
        int z,
        int startHeight,
        double noise,
        BlockState defaultBlock,
        BlockState defaultFluid,
        BlockState topState,
        BlockState middleState,
        BlockState underwaterState,
        int seaLevel,
        CallbackInfo callback
    ) {
        if (Offlimits.CONFIG.allowTerrainModifications.get()) {
            int maxHeight = Integer.MIN_VALUE;
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            int noiseFactor = (int) (noise / 3.0 + 3.0 + random.nextDouble() * 0.25);
            BlockState currentState = middleState;
            int remainingDepth = -1;
            
            // Loop through the height from the startHeight down to the preliminary surface level.
            for(int level = startHeight; level >= ((BiomeExtension) biome).getMinSurfaceLevel(); --level) {
                mutable.set(x, level, z);
                BlockState localState = chunk.getBlockState(mutable);
                
                if (localState.isAir()) {
                    remainingDepth = -1;
                    maxHeight = Integer.MIN_VALUE;
                } else if (!localState.is(defaultBlock.getBlock())) {
                    maxHeight = Math.max(level, maxHeight);
                } else if (remainingDepth == -1) {
                    // Determine the block state to set based on the height and other conditions.
                    remainingDepth = noiseFactor;
                    
                    BlockState updatedState;
                    if (level >= maxHeight + 2) {
                        updatedState = topState;
                    } else if (level >= maxHeight - 1) {
                        currentState = middleState;
                        updatedState = middleState;
                    } else if (level >= maxHeight - 4) {
                        currentState = middleState;
                        updatedState = middleState;
                    } else if (level >= maxHeight - (7 + noiseFactor)) {
                        updatedState = currentState;
                    } else {
                        currentState = defaultBlock;
                        updatedState = underwaterState;
                    }
                    
                    chunk.setBlockState(mutable, updatedState, false);
                } else if (remainingDepth > 0) {
                    --remainingDepth;
                    chunk.setBlockState(mutable, currentState, false);
                    if (remainingDepth == 0 && currentState.is(Blocks.SAND) && noiseFactor > 1) {
                        remainingDepth = random.nextInt(4) + Math.max(0, level - maxHeight);
                        currentState = currentState.is(Blocks.RED_SAND)
                            ? Blocks.RED_SANDSTONE.defaultBlockState()
                            : Blocks.SANDSTONE.defaultBlockState();
                    }
                }
            }
            
            callback.cancel();
        }
    }
}
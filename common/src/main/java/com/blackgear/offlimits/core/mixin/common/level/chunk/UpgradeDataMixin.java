package com.blackgear.offlimits.core.mixin.common.level.chunk;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.UpgradeData;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PalettedContainer.class)
interface PalettedContainerAccessor {
    @Invoker
    <T> T callGet(int index);
}

@Mixin(UpgradeData.class)
public abstract class UpgradeDataMixin {
    @Shadow @Final private int[][] index;
    @Shadow private static BlockState updateState(BlockState state, Direction direction, LevelAccessor level, BlockPos pos, BlockPos offsetPos) { return null; }
    
    @Shadow @Final private static Logger LOGGER;
    
    @ModifyConstant(
        method = "<init>()V",
        constant = @Constant(intValue = 16)
    )
    private int off$constructor(int constant) {
        return Offlimits.INSTANCE.getSectionsCount();
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    private void upgradeInside(LevelChunk levelChunk) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos mutableBlockPos2 = new BlockPos.MutableBlockPos();
        ChunkPos chunkPos = levelChunk.getPos();
        LevelAccessor levelAccessor = levelChunk.getLevel();
        
        for(int i = 0; i < this.index.length; ++i) {
            LevelChunkSection levelChunkSection = levelChunk.getSections()[i];
            int[] is = this.index[i];
            this.index[i] = null;
            if (levelChunkSection != null && is != null && is.length > 0) {
                Direction[] directions = Direction.values();
                PalettedContainer<BlockState> palettedContainer = levelChunkSection.getStates();
                
                for(int j : is) {
                    int k = j & 15;
                    int l = j >> 8 & 15;
                    int m = j >> 4 & 15;
                    mutableBlockPos.set(chunkPos.getMinBlockX() + k, levelChunkSection.bottomBlockY() + l, chunkPos.getMinBlockZ() + m);
                    BlockState blockState = ((PalettedContainerAccessor) palettedContainer).callGet(j);
                    BlockState blockState2 = blockState;
                    
                    for(Direction direction : directions) {
                        mutableBlockPos2.setWithOffset(mutableBlockPos, direction);
                        if (SectionPos.blockToSectionCoord(mutableBlockPos.getX()) == chunkPos.x && SectionPos.blockToSectionCoord(mutableBlockPos.getZ()) == chunkPos.z) {
                            blockState2 = updateState(blockState2, direction, levelAccessor, mutableBlockPos, mutableBlockPos2);
                        }
                    }
                    
                    Block.updateOrDestroy(blockState, blockState2, levelAccessor, mutableBlockPos, 18);
                }
            }
        }
        
        for(int i = 0; i < this.index.length; ++i) {
            if (this.index[i] != null) {
                LOGGER.warn("Discarding update data for section {} for chunk ({} {})", Offlimits.INSTANCE.getSectionYFromSectionIndex(i), chunkPos.x, chunkPos.z);
            }
            
            this.index[i] = null;
        }
    }
}
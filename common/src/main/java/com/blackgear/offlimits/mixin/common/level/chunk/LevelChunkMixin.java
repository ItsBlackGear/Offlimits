package com.blackgear.offlimits.mixin.common.level.chunk;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.Map;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
    @Shadow @Final @Nullable public static LevelChunkSection EMPTY_SECTION;
    
    @Shadow @Final private LevelChunkSection[] sections;
    
    @Shadow @Final private Map<Heightmap.Types, Heightmap> heightmaps;
    
    @Shadow @Final private Level level;
    
    @Shadow public abstract @Nullable BlockEntity getBlockEntity(BlockPos pos, LevelChunk.EntityCreationType creationType);
    
    @Shadow private volatile boolean unsaved;
    
    @ModifyConstant(
        method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/ChunkBiomeContainer;Lnet/minecraft/world/level/chunk/UpgradeData;Lnet/minecraft/world/level/TickList;Lnet/minecraft/world/level/TickList;J[Lnet/minecraft/world/level/chunk/LevelChunkSection;Ljava/util/function/Consumer;)V",
        constant = @Constant(intValue = 16),
        expect = 3
    )
    private int offlimits$init(int original) {
        return Offlimits.INSTANCE.getSectionsCount();
    }
    
    /**
     * @author ItsBlackGear
     * @reason TODO; too lazy to figure this out right now...
     */
    @Overwrite @Nullable
    public BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving) {
        int i = pos.getY();
        int j = Offlimits.INSTANCE.getSectionIndex(i);
        LevelChunkSection levelChunkSection = this.sections[j];
        if (levelChunkSection == EMPTY_SECTION) {
            if (state.isAir()) {
                return null;
            }
            
            levelChunkSection = new LevelChunkSection(SectionPos.blockToSectionCoord(j) << 4);
            this.sections[j] = levelChunkSection;
        }
        
        boolean bl = levelChunkSection.isEmpty();
        int k = pos.getX() & 15;
        int l = i & 15;
        int m = pos.getZ() & 15;
        BlockState blockState2 = levelChunkSection.setBlockState(k, l, m, state);
        if (blockState2 == state) {
            return null;
        } else {
            Block block = state.getBlock();
            Block block2 = blockState2.getBlock();
            this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING).update(k, i, m, state);
            this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES).update(k, i, m, state);
            this.heightmaps.get(Heightmap.Types.OCEAN_FLOOR).update(k, i, m, state);
            this.heightmaps.get(Heightmap.Types.WORLD_SURFACE).update(k, i, m, state);
            boolean bl2 = levelChunkSection.isEmpty();
            if (bl != bl2) {
                this.level.getChunkSource().getLightEngine().updateSectionStatus(pos, bl2);
            }
            
            if (!this.level.isClientSide) {
                blockState2.onRemove(this.level, pos, state, isMoving);
            } else if (block2 != block && block2 instanceof EntityBlock) {
                this.level.removeBlockEntity(pos);
            }
            
            if (!levelChunkSection.getBlockState(k, l, m).is(block)) {
                return null;
            } else {
                BlockEntity blockEntity;
                if (block2 instanceof EntityBlock) {
                    blockEntity = this.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
                    if (blockEntity != null) {
                        blockEntity.clearCache();
                    }
                }
                
                if (!this.level.isClientSide) {
                    state.onPlace(this.level, pos, blockState2, isMoving);
                }
                
                if (block instanceof EntityBlock) {
                    blockEntity = this.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
                    if (blockEntity == null) {
                        blockEntity = ((EntityBlock)block).newBlockEntity(this.level);
                        this.level.setBlockEntity(pos, blockEntity);
                    } else {
                        blockEntity.clearCache();
                    }
                }
                
                this.unsaved = true;
                return blockState2;
            }
        }
    }
}
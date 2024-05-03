package com.blackgear.offlimits.mixin.common.level.chunk;

import com.blackgear.offlimits.Offlimits;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ProtoChunk.class)
public abstract class ProtoChunkMixin {
    @Shadow public abstract LevelChunkSection[] getSections();
    
    @Shadow @Final private ShortList[] postProcessing;
    
    @Shadow public static short packOffsetCoordinates(BlockPos pos) {
        return 0;
    }
    
    @ModifyConstant(
        method = "<init>(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/UpgradeData;[Lnet/minecraft/world/level/chunk/LevelChunkSection;Lnet/minecraft/world/level/chunk/ProtoTickList;Lnet/minecraft/world/level/chunk/ProtoTickList;)V",
        constant = @Constant(intValue = 16)
    )
    private int offlimits$init(int original) {
        return Offlimits.INSTANCE.getSectionsCount();
    }
    
    /**
     * @author ItsBlackGear
     * @reason TODO; too lazy to figure this out right now...
     */
    @Overwrite
    public void markPosForPostprocessing(BlockPos pos) {
        if (!Level.isOutsideBuildHeight(pos)) {
            ChunkAccess.getOrCreateOffsetList(this.postProcessing, Offlimits.INSTANCE.getSectionIndex(pos.getY())).add(packOffsetCoordinates(pos));
        }
        
    }
    
    /**
     * @author ItsBlackGear
     * @reason TODO; too lazy to figure this out right now...
     */
    @Overwrite
    public BlockState getBlockState(BlockPos pos) {
        int i = pos.getY();
        if (Level.isOutsideBuildHeight(i)) {
            return Blocks.VOID_AIR.defaultBlockState();
        } else {
            LevelChunkSection section = this.getSections()[Offlimits.INSTANCE.getSectionIndex(i)];
            return LevelChunkSection.isEmpty(section)
                ? Blocks.AIR.defaultBlockState()
                : section.getBlockState(pos.getX() & 15, i & 15, pos.getZ() & 15);
        }
    }
    
    /**
     * @author ItsBlackGear
     * @reason TODO; too lazy to figure this out right now...
     */
    @Overwrite
    public FluidState getFluidState(BlockPos blockPos) {
        int i = blockPos.getY();
        if (Level.isOutsideBuildHeight(i)) {
            return Fluids.EMPTY.defaultFluidState();
        } else {
            LevelChunkSection levelChunkSection = this.getSections()[Offlimits.INSTANCE.getSectionIndex(i)];
            return LevelChunkSection.isEmpty(levelChunkSection)
                ? Fluids.EMPTY.defaultFluidState()
                : levelChunkSection.getFluidState(blockPos.getX() & 15, i & 15, blockPos.getZ() & 15);
        }
    }
}
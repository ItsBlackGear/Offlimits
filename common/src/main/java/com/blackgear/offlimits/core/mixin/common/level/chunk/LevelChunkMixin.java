package com.blackgear.offlimits.core.mixin.common.level.chunk;

import com.blackgear.offlimits.Offlimits;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
    @Shadow @Final private Map<BlockPos, BlockEntity> blockEntities;
    
    @Shadow @Final private Level level;
    
    @Shadow @Final private LevelChunkSection[] sections;
    
    @Shadow @Final @Nullable public static LevelChunkSection EMPTY_SECTION;
    
    @Shadow private ChunkBiomeContainer biomes;
    
    @Shadow public abstract void setHeightmap(Heightmap.Types type, long[] data);
    
    @Shadow public abstract ChunkPos getPos();
    
    @Shadow @Final private ShortList[] postProcessing;
    
    @Shadow public abstract void unpackTicks();
    
    @Shadow @Final private Map<BlockPos, CompoundTag> pendingBlockEntities;
    
    @Shadow @Nullable public abstract BlockEntity getBlockEntity(BlockPos pos);
    @Shadow @Nullable public abstract BlockEntity getBlockEntity(BlockPos pos, LevelChunk.EntityCreationType creationMode);
    
    @Shadow @Final private UpgradeData upgradeData;
    
    @Shadow @Final private Map<Heightmap.Types, Heightmap> heightmaps;
    
    @Shadow private volatile boolean unsaved;
    @Unique private final LevelChunk self = (LevelChunk) (Object) this;
    
    @ModifyConstant(
        method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/ChunkBiomeContainer;Lnet/minecraft/world/level/chunk/UpgradeData;Lnet/minecraft/world/level/TickList;Lnet/minecraft/world/level/TickList;J[Lnet/minecraft/world/level/chunk/LevelChunkSection;Ljava/util/function/Consumer;)V",
        constant = @Constant(intValue = 16),
        expect = 3
    )
    private int offlimits$init(int original) {
        return Offlimits.LEVEL.getSectionsCount();
    }
    
    @Inject(
        method = "replaceWithPacketData",
        at = @At("HEAD"),
        cancellable = true
    )
    public void replaceWithPacketData(
        ChunkBiomeContainer chunkBiomeContainer,
        FriendlyByteBuf friendlyByteBuf,
        CompoundTag compoundTag,
        int bit,
        CallbackInfo ci
    ) {
        boolean bl = chunkBiomeContainer != null;
        Predicate<BlockPos> predicate = bl ? (blockPos) -> true : (blockPos) -> {
            return (bit & 1 << Offlimits.LEVEL.getSectionIndex(blockPos.getY())) != 0;
        };
        Stream<BlockPos> blockEntities = Sets.newHashSet(this.blockEntities.keySet()).stream().filter(predicate);
        blockEntities.forEach(this.level::removeBlockEntity);
        
        for(int i = 0; i < this.sections.length; ++i) {
            LevelChunkSection section = this.sections[i];
            if ((bit & 1 << i) == 0) {
                if (bl && section != EMPTY_SECTION) {
                    this.sections[i] = EMPTY_SECTION;
                }
            } else {
                if (section == EMPTY_SECTION) {
                    section = new LevelChunkSection(Offlimits.LEVEL.getSectionYFromSectionIndex(i) << 4);
                    this.sections[i] = section;
                }
                
                section.read(friendlyByteBuf);
            }
        }
        
        if (chunkBiomeContainer != null) {
            this.biomes = chunkBiomeContainer;
        }
        
        for (Heightmap.Types types : Heightmap.Types.values()) {
            String string = types.getSerializationKey();
            if (compoundTag.contains(string, 12)) {
                this.setHeightmap(types, compoundTag.getLongArray(string));
            }
        }
        
        for (BlockEntity blockEntity : this.blockEntities.values()) {
            blockEntity.clearCache();
        }
        
        ci.cancel();
    }
    
    @Inject(
        method = "postProcessGeneration",
        at = @At("HEAD"),
        cancellable = true
    )
    public void postProcessGeneration(CallbackInfo ci) {
        ChunkPos chunkPos = this.getPos();
        
        for(int i = 0; i < this.postProcessing.length; ++i) {
            if (this.postProcessing[i] != null) {
                
                for (Short short_ : this.postProcessing[i]) {
                    BlockPos blockPos = ProtoChunk.unpackOffsetCoordinates(short_, Offlimits.LEVEL.getSectionYFromSectionIndex(i), chunkPos);
                    BlockState blockState = this.getBlockState(blockPos);
                    BlockState blockState2 = Block.updateFromNeighbourShapes(blockState, this.level, blockPos);
                    this.level.setBlock(blockPos, blockState2, 20);
                }
                
                this.postProcessing[i].clear();
            }
        }
        
        this.unpackTicks();
        
        for (BlockPos blockPos2 : Sets.newHashSet(this.pendingBlockEntities.keySet())) {
            this.getBlockEntity(blockPos2);
        }
        
        this.pendingBlockEntities.clear();
        this.upgradeData.upgrade(this.self);
        ci.cancel();
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    public BlockState getBlockState(BlockPos blockPos) {
        int i = blockPos.getX();
        int j = blockPos.getY();
        int k = blockPos.getZ();
        if (this.level.isDebug()) {
            BlockState blockState = null;
            if (j == 60) {
                blockState = Blocks.BARRIER.defaultBlockState();
            }
            
            if (j == 70) {
                blockState = DebugLevelSource.getBlockStateFor(i, k);
            }
            
            return blockState == null ? Blocks.AIR.defaultBlockState() : blockState;
        } else {
            try {
                int l = Offlimits.LEVEL.getSectionIndex(j);
                if (l >= 0 && l < this.sections.length) {
                    LevelChunkSection levelChunkSection = this.sections[l];
                    if (!LevelChunkSection.isEmpty(levelChunkSection)) {
                        return levelChunkSection.getBlockState(i & 15, j & 15, k & 15);
                    }
                }
                
                return Blocks.AIR.defaultBlockState();
            } catch (Throwable var8) {
                CrashReport crashReport = CrashReport.forThrowable(var8, "Getting block state");
                CrashReportCategory crashReportCategory = crashReport.addCategory("Block being got");
                crashReportCategory.setDetail("Location", () -> CrashReportCategory.formatLocation(i, j, k));
                throw new ReportedException(crashReport);
            }
        }
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    public FluidState getFluidState(int i, int j, int k) {
        try {
            int l = Offlimits.LEVEL.getSectionIndex(j);
            if (l >= 0 && l < this.sections.length) {
                LevelChunkSection levelChunkSection = this.sections[l];
                if (!LevelChunkSection.isEmpty(levelChunkSection)) {
                    return levelChunkSection.getFluidState(i & 15, j & 15, k & 15);
                }
            }
            
            return Fluids.EMPTY.defaultFluidState();
        } catch (Throwable var7) {
            CrashReport crashReport = CrashReport.forThrowable(var7, "Getting fluid state");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Block being got");
            crashReportCategory.setDetail("Location", () -> CrashReportCategory.formatLocation(i, j, k));
            throw new ReportedException(crashReport);
        }
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    public @Nullable BlockState setBlockState(BlockPos blockPos, BlockState blockState, boolean isMoving) {
        int i = blockPos.getY();
        int j = Offlimits.LEVEL.getSectionIndex(i);
        LevelChunkSection levelChunkSection = this.sections[j];
        if (levelChunkSection == EMPTY_SECTION) {
            if (blockState.isAir()) {
                return null;
            }
            
            levelChunkSection = new LevelChunkSection(i >> 4 << 4);
            this.sections[j] = levelChunkSection;
        }
        
        boolean bl2 = levelChunkSection.isEmpty();
        int k = blockPos.getX() & 15;
        int l = i & 15;
        int m = blockPos.getZ() & 15;
        BlockState blockState2 = levelChunkSection.setBlockState(k, l, m, blockState);
        if (blockState2 == blockState) {
            return null;
        } else {
            Block block = blockState.getBlock();
            Block block2 = blockState2.getBlock();
            this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING).update(k, i, m, blockState);
            this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES).update(k, i, m, blockState);
            this.heightmaps.get(Heightmap.Types.OCEAN_FLOOR).update(k, i, m, blockState);
            this.heightmaps.get(Heightmap.Types.WORLD_SURFACE).update(k, i, m, blockState);
            boolean bl3 = levelChunkSection.isEmpty();
            if (bl2 != bl3) {
                this.level.getChunkSource().getLightEngine().updateSectionStatus(blockPos, bl3);
            }
            
            if (!this.level.isClientSide) {
                blockState2.onRemove(this.level, blockPos, blockState, isMoving);
            } else if (block2 != block && block2 instanceof EntityBlock) {
                this.level.removeBlockEntity(blockPos);
            }
            
            if (!levelChunkSection.getBlockState(k, l, m).is(block)) {
                return null;
            } else {
                if (block2 instanceof EntityBlock) {
                    BlockEntity blockEntity = this.getBlockEntity(blockPos, LevelChunk.EntityCreationType.CHECK);
                    if (blockEntity != null) {
                        blockEntity.clearCache();
                    }
                }
                
                if (!this.level.isClientSide) {
                    blockState.onPlace(this.level, blockPos, blockState2, isMoving);
                }
                
                if (block instanceof EntityBlock) {
                    BlockEntity blockEntity = this.getBlockEntity(blockPos, LevelChunk.EntityCreationType.CHECK);
                    if (blockEntity == null) {
                        blockEntity = ((EntityBlock)block).newBlockEntity(this.level);
                        this.level.setBlockEntity(blockPos, blockEntity);
                    } else {
                        blockEntity.clearCache();
                    }
                }
                
                this.unsaved = true;
                return blockState2;
            }
        }
    }
    
    //TODO: getLights
}
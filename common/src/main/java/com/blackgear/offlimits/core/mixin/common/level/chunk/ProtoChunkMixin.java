package com.blackgear.offlimits.core.mixin.common.level.chunk;

import com.blackgear.offlimits.Offlimits;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Mixin(ProtoChunk.class)
public abstract class ProtoChunkMixin {
    @Shadow public abstract LevelChunkSection[] getSections();
    
    @Shadow public abstract void addLight(BlockPos lightPos);
    
    @Shadow public static BlockPos unpackOffsetCoordinates(short packedPos, int yOffset, ChunkPos chunkPos) { return null; }
    
    @Shadow @Final private ChunkPos chunkPos;
    
    @Shadow @Final private List<BlockPos> lights;
    
    @Shadow
    public static short packOffsetCoordinates(BlockPos pos) {
        return 0;
    }
    
    @Shadow @Final private ShortList[] postProcessing;
    
    @Shadow @Final private LevelChunkSection[] sections;
    
    @Shadow public abstract ChunkPos getPos();
    
    @Shadow private volatile ChunkStatus status;
    
    @Shadow @Nullable private volatile LevelLightEngine lightEngine;
    
    @Shadow public abstract ChunkStatus getStatus();
    
    @Shadow @Final private Map<Heightmap.Types, Heightmap> heightmaps;
    
    @Shadow @Final private Map<GenerationStep.Carving, BitSet> carvingMasks;
    
    @ModifyConstant(
        method = "<init>(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/UpgradeData;[Lnet/minecraft/world/level/chunk/LevelChunkSection;Lnet/minecraft/world/level/chunk/ProtoTickList;Lnet/minecraft/world/level/chunk/ProtoTickList;)V",
        constant = @Constant(intValue = 16)
    )
    private int offlimits$init(int original) {
        return Offlimits.INSTANCE.getSectionsCount();
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    public LevelChunkSection getOrCreateSection(int i) {
        LevelChunkSection[] sections = this.getSections();
        if (sections[i] == LevelChunk.EMPTY_SECTION) {
            sections[i] = new LevelChunkSection(Offlimits.INSTANCE.getSectionYFromSectionIndex(i) << 4);
        }

        return sections[i];
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    public BlockState getBlockState(BlockPos blockPos) {
        int i = blockPos.getY();
        if (Level.isOutsideBuildHeight(i)) {
            return Blocks.VOID_AIR.defaultBlockState();
        } else {
            LevelChunkSection levelChunkSection = this.getSections()[Offlimits.INSTANCE.getSectionIndex(i)];
            return LevelChunkSection.isEmpty(levelChunkSection)
                ? Blocks.AIR.defaultBlockState()
                : levelChunkSection.getBlockState(blockPos.getX() & 15, i & 15, blockPos.getZ() & 15);
        }
    }
    
    /**
     * @author
     * @reason
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
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    public ShortList[] getPackedLights() {
        ShortList[] shortLists = new ShortList[Offlimits.INSTANCE.getSectionsCount()];
        
        for(BlockPos blockPos : this.lights) {
            ChunkAccess.getOrCreateOffsetList(shortLists, Offlimits.INSTANCE.getSectionIndex(blockPos.getY())).add(packOffsetCoordinates(blockPos));
        }
        
        return shortLists;
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    public void addLight(short s, int i) {
        this.addLight(unpackOffsetCoordinates(s, Offlimits.INSTANCE.getSectionYFromSectionIndex(i), this.chunkPos));
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    public void markPosForPostprocessing(BlockPos blockPos) {
        if (!Level.isOutsideBuildHeight(blockPos)) {
            ChunkAccess.getOrCreateOffsetList(this.postProcessing, Offlimits.INSTANCE.getSectionIndex(blockPos.getY())).add(packOffsetCoordinates(blockPos));
        }
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    public @Nullable BlockState setBlockState(BlockPos blockPos, BlockState blockState, boolean bl) {
        int i = blockPos.getX();
        int j = blockPos.getY();
        int k = blockPos.getZ();
        if (j >= Offlimits.INSTANCE.getMinBuildHeight() && j < Offlimits.INSTANCE.getMaxBuildHeight()) {
            int l = Offlimits.INSTANCE.getSectionIndex(j);
            if (this.sections[l] == LevelChunk.EMPTY_SECTION && blockState.is(Blocks.AIR)) {
                return blockState;
            } else {
                if (blockState.getLightEmission() > 0) {
                    this.lights.add(new BlockPos((i & 15) + this.getPos().getMinBlockX(), j, (k & 15) + this.getPos().getMinBlockZ()));
                }
                
                LevelChunkSection levelChunkSection = this.getOrCreateSection(l);
                BlockState blockState2 = levelChunkSection.setBlockState(i & 15, j & 15, k & 15, blockState);
                if (this.status.isOrAfter(ChunkStatus.FEATURES)
                    && blockState != blockState2
                    && (
                    blockState.getLightBlock((ProtoChunk)(Object)this, blockPos) != blockState2.getLightBlock((ProtoChunk)(Object)this, blockPos)
                        || blockState.getLightEmission() != blockState2.getLightEmission()
                        || blockState.useShapeForLightOcclusion()
                        || blockState2.useShapeForLightOcclusion()
                )) {
                    this.lightEngine.checkBlock(blockPos);
                }
                
                EnumSet<Heightmap.Types> enumSet = this.getStatus().heightmapsAfter();
                EnumSet<Heightmap.Types> enumSet2 = null;
                
                for(Heightmap.Types types : enumSet) {
                    Heightmap heightmap = this.heightmaps.get(types);
                    if (heightmap == null) {
                        if (enumSet2 == null) {
                            enumSet2 = EnumSet.noneOf(Heightmap.Types.class);
                        }
                        
                        enumSet2.add(types);
                    }
                }
                
                if (enumSet2 != null) {
                    Heightmap.primeHeightmaps((ProtoChunk)(Object)this, enumSet2);
                }
                
                for(Heightmap.Types types : enumSet) {
                    this.heightmaps.get(types).update(i & 15, j, k & 15, blockState);
                }
                
                return blockState2;
            }
        } else {
            return Blocks.VOID_AIR.defaultBlockState();
        }
    }
    
    @Inject(
        method = "getOrCreateCarvingMask",
        at = @At("HEAD"),
        cancellable = true
    )
    private void off$getOrCreateCarvingMask(GenerationStep.Carving carving, CallbackInfoReturnable<BitSet> cir) {
        cir.setReturnValue(this.carvingMasks.computeIfAbsent(carving, (carving_) -> new BitSet(98304)));
    }
}
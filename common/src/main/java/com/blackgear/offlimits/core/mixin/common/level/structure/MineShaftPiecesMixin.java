package com.blackgear.offlimits.core.mixin.common.level.structure;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.MineshaftFeature;
import net.minecraft.world.level.levelgen.feature.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.MineShaftPieces;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(MineShaftPieces.MineShaftCorridor.class)
public abstract class MineShaftPiecesMixin extends MineShaftPieces.MineShaftPiece {
    @Shadow @Final private int numSections;
    
    public MineShaftPiecesMixin(StructurePieceType structurePieceType, int i, MineshaftFeature.Type type) {
        super(structurePieceType, i, type);
    }
    
    @Inject(
        method = "postProcess",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/level/levelgen/structure/MineShaftPieces$MineShaftCorridor;hasRails:Z"
        )
    )
    private void a(WorldGenLevel level, StructureFeatureManager structureManager, ChunkGenerator chunkGenerator, Random random, BoundingBox box, ChunkPos chunkPos, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        this.placeDoubleLowerOrUpperSupport(level, box, 0, -1, 2);
        if (this.numSections > 1) {
            int o = (this.numSections * 5 - 1) - 2;
            this.placeDoubleLowerOrUpperSupport(level, box, 0, -1, o);
        }
    }
    
    @Unique
    private void placeDoubleLowerOrUpperSupport(WorldGenLevel level, BoundingBox box, int i, int j, int k) {
        BlockState woodState = this.type == MineshaftFeature.Type.NORMAL
            ? Blocks.OAK_LOG.defaultBlockState()
            : Blocks.DARK_OAK_LOG.defaultBlockState();
        BlockState planksState = this.getPlanksBlock();
        
        if (this.getBlock(level, i, j, k, box).is(planksState.getBlock())) {
            this.fillPillarDownOrChainUp(level, woodState, i, j, k, box);
        }
        
        if (this.getBlock(level, i + 2, j, k, box).is(planksState.getBlock())) {
            this.fillPillarDownOrChainUp(level, woodState, i + 2, j, k, box);
        }
    }
    
    private BlockPos.MutableBlockPos getWorldPos(int x, int y, int z) {
        return new BlockPos.MutableBlockPos(this.getWorldX(x, z), this.getWorldY(y), this.getWorldZ(x, z));
    }
    
    @Override
    protected void fillColumnDown(WorldGenLevel level, BlockState state, int x, int y, int z, BoundingBox box) {
        BlockPos.MutableBlockPos mutable = this.getWorldPos(x, y, z);
        
        if (box.isInside(mutable)) {
            int l = mutable.getY();
            
            while (this.isReplaceableByStructures(level.getBlockState(mutable)) && mutable.getY() < Offlimits.LEVEL.getMinBuildHeight() + 1) {
                mutable.move(Direction.DOWN);
            }
            
            if (this.canPlaceColumnOnTopOf(level.getBlockState(mutable))) {
                while (mutable.getY() < l) {
                    mutable.move(Direction.UP);
                    level.setBlock(mutable, state, 2);
                }
            }
        }
    }
    
    @Unique
    private void fillPillarDownOrChainUp(WorldGenLevel level, BlockState state, int i, int j, int k, BoundingBox box) {
        BlockPos.MutableBlockPos mutable = this.getWorldPos(i, j, k);
        
        if (box.isInside(mutable)) {
            int l = mutable.getY();
            int m = 1;
            boolean bl = true;
            
            for (boolean bl2 = true; bl || bl2; m++) {
                if (bl) {
                    mutable.setY(l - m);
                    BlockState blockState2 = level.getBlockState(mutable);
                    boolean bl3 = this.isReplaceableByStructures(blockState2) && !blockState2.is(Blocks.LAVA);
                    if (!bl3 && this.canPlaceColumnOnTopOf(blockState2)) {
                        fillColumnBetween(level, state, mutable, l - m + 1, l);
                        return;
                    }
                    
                    bl = m <= 20 && bl3 && mutable.getY() > Offlimits.LEVEL.getMinBuildHeight() + 1;
                }
                
                if (bl2) {
                    mutable.setY(l + m);
                    BlockState blockState2 = level.getBlockState(mutable);
                    boolean bl3 = this.isReplaceableByStructures(blockState2);
                    if (!bl3 && this.canHangChainBelow(level, mutable, blockState2)) {
                        level.setBlock(mutable.set(mutable.getX(), l + 1, mutable.getZ()), this.getFenceBlock(), 2);
                        fillColumnBetween(level, Blocks.CHAIN.defaultBlockState(), mutable, l + 2, l + m);
                        return;
                    }
                    
                    bl2 = m <= 20 && bl3 && mutable.getY() < Offlimits.LEVEL.getMaxBuildHeight() - 1;
                }
            }
        }
    }
    
    @Unique
    private void fillColumnBetween(WorldGenLevel level, BlockState state, BlockPos.MutableBlockPos mutable, int i, int j) {
        for (int k = i; k < j; k++) {
            level.setBlock(mutable.set(mutable.getX(), k, mutable.getZ()), state, 2);
        }
    }
    
    @Unique
    private boolean canPlaceColumnOnTopOf(BlockState state) {
        return !state.is(Blocks.RAIL) && !state.is(Blocks.LAVA);
    }
    
    @Unique
    private boolean canHangChainBelow(LevelReader level, BlockPos pos, BlockState state) {
        return Block.canSupportCenter(level, pos, Direction.DOWN) && !(state.getBlock() instanceof FallingBlock);
    }
    
    @Unique
    private boolean isReplaceableByStructures(BlockState state) {
        return state.isAir()
            || state.getMaterial().isLiquid()
            || state.is(Blocks.SEAGRASS)
            || state.is(Blocks.TALL_GRASS);
    }
}
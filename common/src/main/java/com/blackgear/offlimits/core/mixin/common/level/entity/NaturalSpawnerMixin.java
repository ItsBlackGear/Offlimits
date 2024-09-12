package com.blackgear.offlimits.core.mixin.common.level.entity;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.platform.core.util.MathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.PathComputationType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(NaturalSpawner.class)
public class NaturalSpawnerMixin {
    @ModifyConstant(
        method = "spawnCategoryForChunk",
        constant = @Constant(intValue = 1)
    )
    private static int off$increaseRange(int constant) {
        return Offlimits.LEVEL.getMinBuildHeight() + 1;
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    private static BlockPos getRandomPosWithin(Level level, LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        int x = pos.getMinBlockX() + level.random.nextInt(16);
        int z = pos.getMinBlockZ() + level.random.nextInt(16);
        int height = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 1;
        int y = MathUtils.randomBetweenInclusive(level.random, Offlimits.LEVEL.getMinBuildHeight(), height);
        return new BlockPos(x, y, z);
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    private static BlockPos getTopNonCollidingPos(LevelReader level, EntityType<?> type, int x, int z) {
        int y = level.getHeight(SpawnPlacements.getHeightmapType(type), x, z);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(x, y, z);
        
        // If the dimension has a ceiling, move down until an air block is found.
        if (level.dimensionType().hasCeiling()) {
            do {
                mutable.move(Direction.DOWN);
            } while(!level.getBlockState(mutable).isAir());
            
            // Continue moving down until a non-air block is found or the minimum build height is reached.
            do {
                mutable.move(Direction.DOWN);
            } while(level.getBlockState(mutable).isAir() && mutable.getY() > Offlimits.LEVEL.getMinBuildHeight());
        }
        
        // If the entity should be placed on the ground, check if the block below is pathfindable.
        if (SpawnPlacements.getPlacementType(type) == SpawnPlacements.Type.ON_GROUND) {
            BlockPos pos = mutable.below();
            if (level.getBlockState(pos).isPathfindable(level, pos, PathComputationType.LAND)) {
                return pos;
            }
        }
        
        return mutable.immutable();
    }
}
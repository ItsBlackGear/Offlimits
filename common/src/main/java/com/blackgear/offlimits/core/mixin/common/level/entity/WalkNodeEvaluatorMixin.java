package com.blackgear.offlimits.core.mixin.common.level.entity;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.*;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin extends NodeEvaluator {
    @Shadow public static double getFloorLevel(BlockGetter level, BlockPos pos) { throw new AssertionError(); }
    @Shadow protected abstract BlockPathTypes getCachedBlockType(Mob entity, int x, int y, int z);
    @Shadow protected abstract BlockPathTypes getBlockPathType(Mob entity, BlockPos pos);
    @Shadow protected abstract boolean canReachWithoutCollision(Node arg);
    @Shadow protected abstract boolean hasCollisions(AABB arg);
    @Shadow protected static BlockPathTypes getBlockPathTypeRaw(BlockGetter arg, BlockPos arg2) { throw new AssertionError(); }
    @Shadow public static BlockPathTypes checkNeighbourBlocks(BlockGetter level, BlockPos.MutableBlockPos centerPos, BlockPathTypes nodeType) { throw new AssertionError(); }
    @Shadow protected abstract boolean hasPositiveMalus(BlockPos arg);
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    public Node getStart() {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int y = Mth.floor(this.mob.getY());
        BlockState state = this.level.getBlockState(mutable.set(this.mob.getX(), y, this.mob.getZ()));
        
        // Check if the entity can stand on the current fluid.
        if (!this.mob.canStandOnFluid(state.getFluidState().getType())) {
            // If the entity can float and is in water, find the surface level.
            if (this.canFloat() && this.mob.isInWater()) {
                while (true) {
                    if (state.getBlock() != Blocks.WATER && state.getFluidState() != Fluids.WATER.getSource(false)) {
                        y--;
                        break;
                    }
                    
                    state = this.level.getBlockState(mutable.set(this.mob.getX(), ++y, this.mob.getZ()));
                }
            } else if (this.mob.isOnGround()) {
                // If the entity is on the ground, adjust the y-coordinate slightly.
                y = Mth.floor(this.mob.getY() + 0.5);
            } else {
                // If the entity is in the air, find the nearest ground level.
                BlockPos pos = this.mob.blockPosition();
                
                while (
                    (this.level.getBlockState(pos).isAir() || this.level.getBlockState(pos).isPathfindable(this.level, pos, PathComputationType.LAND))
                    && pos.getY() > Offlimits.LEVEL.getMinBuildHeight()
                ) {
                    pos = pos.below();
                }
                
                y = pos.above().getY();
            }
        } else {
            // If the entity can stand in the fluid, find the surface level.
            while (this.mob.canStandOnFluid(state.getFluidState().getType())) {
                state = this.level.getBlockState(mutable.set(this.mob.getX(), ++y, this.mob.getZ()));
            }
            
            y--;
        }
        
        BlockPos pos = this.mob.blockPosition();
        BlockPathTypes blockPath = this.getCachedBlockType(this.mob, pos.getX(), y, pos.getZ());
        
        // Check if the pathfinding malus for the current path type is negative.
        if (this.mob.getPathfindingMalus(blockPath) < 0.0F) {
            AABB boundingBox = this.mob.getBoundingBox();
            if (
                this.hasPositiveMalus(mutable.set(boundingBox.minX, y, boundingBox.minZ))
                || this.hasPositiveMalus(mutable.set(boundingBox.minX, y, boundingBox.maxZ))
                || this.hasPositiveMalus(mutable.set(boundingBox.maxX, y, boundingBox.minZ))
                || this.hasPositiveMalus(mutable.set(boundingBox.maxX, y, boundingBox.maxZ))
            ) {
                Node node = this.getNode(mutable);
                node.type = this.getBlockPathType(this.mob, node.asBlockPos());
                node.costMalus = this.mob.getPathfindingMalus(node.type);
                return node;
            }
        }
        
        // Create and return the start node.
        Node node = this.getNode(pos.getX(), y, pos.getZ());
        node.type = this.getBlockPathType(this.mob, node.asBlockPos());
        node.costMalus = this.mob.getPathfindingMalus(node.type);
        return node;
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    private @Nullable Node getLandNode(int x, int y, int z, int steps, double surfaceLevel, Direction direction, BlockPathTypes blockPath) {
        Node landNode = null;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        double floorLevel = getFloorLevel(this.level, mutable.set(x, y, z));
        
        // If the floor level is too high compared to the surface level, return null.
        if (floorLevel - surfaceLevel > 1.125) {
            return null;
        } else {
            BlockPathTypes cachedBlockPath = this.getCachedBlockType(this.mob, x, y, z);
            float pathfindingMalus = this.mob.getPathfindingMalus(cachedBlockPath);
            double width = (double) this.mob.getBbWidth() / 2.0;
            
            // If the pathfinding malus is non-negative, create a new node.
            if (pathfindingMalus >= 0.0F) {
                landNode = this.getNode(x, y, z);
                landNode.type = cachedBlockPath;
                landNode.costMalus = Math.max(landNode.costMalus, pathfindingMalus);
            }
            
            // If the block path is a fence and the node cannot be reached without a collision, set the node to null.
            if (
                blockPath == BlockPathTypes.FENCE
                && landNode != null
                && landNode.costMalus >= 0.0F
                && !this.canReachWithoutCollision(landNode)
            ) {
                landNode = null;
            }
            
            // If the cached block path is not walkable, attempt to find a walkable node.
            if (cachedBlockPath != BlockPathTypes.WALKABLE) {
                if (
                    (landNode == null || landNode.costMalus < 0.0F)
                    && steps > 0
                    && cachedBlockPath != BlockPathTypes.FENCE
                    && cachedBlockPath != BlockPathTypes.UNPASSABLE_RAIL
                    && cachedBlockPath != BlockPathTypes.TRAPDOOR
                ) {
                    landNode = this.getLandNode(x, y + 1, z, steps - 1, surfaceLevel, direction, blockPath);
                    
                    // Check if the node is open or walkable and the entity's width is less than 1.0.
                    if (
                        landNode != null
                        && (landNode.type == BlockPathTypes.OPEN || landNode.type == BlockPathTypes.WALKABLE)
                        && this.mob.getBbWidth() < 1.0F
                    ) {
                        double xOffset = (double) (x - direction.getStepX()) + 0.5;
                        double zOffset = (double) (z - direction.getStepZ()) + 0.5;
                        
                        // Create a bounding box for collision detection.
                        AABB boundingBox = new AABB(
                            xOffset - width,
                            getFloorLevel(this.level, mutable.set(xOffset, y + 1, zOffset)) + 0.001,
                            zOffset - width,
                            xOffset + width,
                            (double) this.mob.getBbHeight() + getFloorLevel(this.level, mutable.set(landNode.x, landNode.y, (double) landNode.z)) - 0.002,
                            zOffset + width
                        );
                        
                        // If there are no collisions, set the node to null.
                        if (this.hasCollisions(boundingBox)) {
                            landNode = null;
                        }
                    }
                }
                
                // If the cached block path is water and the entity cannot float, attempt to find a land node below.
                if (cachedBlockPath == BlockPathTypes.WATER && !this.canFloat()) {
                    if (this.getCachedBlockType(this.mob, x, y - 1, z) != BlockPathTypes.WATER) {
                        return landNode;
                    }
                    
                    while (y > Offlimits.LEVEL.getMinBuildHeight()) {
                        cachedBlockPath = this.getCachedBlockType(this.mob, x, y--, z);
                        
                        if (cachedBlockPath != BlockPathTypes.WATER) {
                            return landNode;
                        }
                        
                        landNode = this.getNode(x, y, z);
                        landNode.type = cachedBlockPath;
                        landNode.costMalus = Math.max(landNode.costMalus, this.mob.getPathfindingMalus(cachedBlockPath));
                    }
                }
                
                // If the cached block path is open, attempt to find a walkable node below.
                if (cachedBlockPath == BlockPathTypes.OPEN) {
                    int fallDistance = 0;
                    int localY = y;
                    
                    while (cachedBlockPath == BlockPathTypes.OPEN) {
                        if (y-- < Offlimits.LEVEL.getMinBuildHeight()) {
                            Node node = this.getNode(x, localY, z);
                            node.type = BlockPathTypes.BLOCKED;
                            node.costMalus = -1.0F;
                            return node;
                        }
                        
                        if (fallDistance++ >= this.mob.getMaxFallDistance()) {
                            Node node = this.getNode(x, y, z);
                            node.type = BlockPathTypes.BLOCKED;
                            node.costMalus = -1.0F;
                            return node;
                        }
                        
                        cachedBlockPath = this.getCachedBlockType(this.mob, x, y, z);
                        pathfindingMalus = this.mob.getPathfindingMalus(cachedBlockPath);
                        
                        if (cachedBlockPath != BlockPathTypes.OPEN && pathfindingMalus >= 0.0F) {
                            landNode = this.getNode(x, y, z);
                            landNode.type = cachedBlockPath;
                            landNode.costMalus = Math.max(landNode.costMalus, pathfindingMalus);
                            break;
                        }
                        
                        if (pathfindingMalus < 0.0F) {
                            Node node = this.getNode(x, y, z);
                            node.type = BlockPathTypes.BLOCKED;
                            node.costMalus = -1.0F;
                            return node;
                        }
                    }
                }
                
                // If the cached block path is a fence, create a closed node.
                if (cachedBlockPath == BlockPathTypes.FENCE) {
                    landNode = this.getNode(x, y, z);
                    landNode.closed = true;
                    landNode.type = cachedBlockPath;
                    landNode.costMalus = cachedBlockPath.getMalus();
                }
                
            }
            return landNode;
        }
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    public static BlockPathTypes getBlockPathTypeStatic(BlockGetter level, BlockPos.MutableBlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        BlockPathTypes blockPath = getBlockPathTypeRaw(level, pos);
        
        // If the block path is open and the y-coordinate is above the min build height, check the block below.
        if (blockPath == BlockPathTypes.OPEN && y >= Offlimits.LEVEL.getMinBuildHeight() + 1) {
            BlockPathTypes blockPathBelow = getBlockPathTypeRaw(level, pos.set(x, y - 1, z));
            blockPath = blockPathBelow != BlockPathTypes.WALKABLE
                && blockPathBelow != BlockPathTypes.OPEN
                && blockPathBelow != BlockPathTypes.WATER
                && blockPathBelow != BlockPathTypes.LAVA
                ? BlockPathTypes.WALKABLE
                : BlockPathTypes.OPEN;
            
            // Check for a specific block path below and update the block path accordingly.
            if (blockPathBelow == BlockPathTypes.DAMAGE_FIRE) {
                blockPath = BlockPathTypes.DAMAGE_FIRE;
            }
            
            if (blockPathBelow == BlockPathTypes.DAMAGE_CACTUS) {
                blockPath = BlockPathTypes.DAMAGE_CACTUS;
            }
            
            if (blockPathBelow == BlockPathTypes.DAMAGE_OTHER) {
                blockPath = BlockPathTypes.DAMAGE_OTHER;
            }
            
            if (blockPathBelow == BlockPathTypes.STICKY_HONEY) {
                blockPath = BlockPathTypes.STICKY_HONEY;
            }
        }
        
        // If the block path is walkable, check the neighbouring blocks.
        if (blockPath == BlockPathTypes.WALKABLE) {
            blockPath = checkNeighbourBlocks(level, pos.set(x, y, z), blockPath);
        }
        
        return blockPath;
    }
}
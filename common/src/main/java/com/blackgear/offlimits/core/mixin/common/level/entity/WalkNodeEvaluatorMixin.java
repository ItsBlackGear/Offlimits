package com.blackgear.offlimits.core.mixin.common.level.entity;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin extends NodeEvaluator {
    @Shadow public static double getFloorLevel(BlockGetter level, BlockPos pos) { throw new AssertionError(); }
    @Shadow protected abstract BlockPathTypes getCachedBlockType(Mob entity, int x, int y, int z);
    @Shadow protected abstract boolean canReachWithoutCollision(Node arg);
    @Shadow protected abstract boolean hasCollisions(AABB arg);
    @Shadow protected static BlockPathTypes getBlockPathTypeRaw(BlockGetter arg, BlockPos arg2) { throw new AssertionError(); }
    @Shadow public static BlockPathTypes checkNeighbourBlocks(BlockGetter level, BlockPos.MutableBlockPos centerPos, BlockPathTypes nodeType) { throw new AssertionError(); }
    
    @ModifyConstant(
        method = "getStart",
        constant = @Constant(intValue = 0)
    )
    private int off$setStartMinHeight(int constant) {
        return Offlimits.LEVEL.getMinBuildHeight();
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    private Node getLandNode(int i, int j, int k, int l, double d, Direction direction, BlockPathTypes blockPathTypes) {
        Node node = null;
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        double e = getFloorLevel(this.level, mutableBlockPos.set(i, j, k));
        if (e - d > 1.125) {
            return null;
        } else {
            BlockPathTypes blockPathTypes2 = this.getCachedBlockType(this.mob, i, j, k);
            float f = this.mob.getPathfindingMalus(blockPathTypes2);
            double g = (double)this.mob.getBbWidth() / 2.0;
            if (f >= 0.0F) {
                node = this.getNode(i, j, k);
                node.type = blockPathTypes2;
                node.costMalus = Math.max(node.costMalus, f);
            }
            
            if (blockPathTypes == BlockPathTypes.FENCE && node != null && node.costMalus >= 0.0F && !this.canReachWithoutCollision(node)) {
                node = null;
            }
            
            if (blockPathTypes2 != BlockPathTypes.WALKABLE) {
                if ((node == null || node.costMalus < 0.0F)
                    && l > 0
                    && blockPathTypes2 != BlockPathTypes.FENCE
                    && blockPathTypes2 != BlockPathTypes.UNPASSABLE_RAIL
                    && blockPathTypes2 != BlockPathTypes.TRAPDOOR) {
                    node = this.getLandNode(i, j + 1, k, l - 1, d, direction, blockPathTypes);
                    if (node != null && (node.type == BlockPathTypes.OPEN || node.type == BlockPathTypes.WALKABLE) && this.mob.getBbWidth() < 1.0F) {
                        double h = (double) (i - direction.getStepX()) + 0.5;
                        double m = (double) (k - direction.getStepZ()) + 0.5;
                        AABB aABB = new AABB(
                            h - g,
                            getFloorLevel(this.level, mutableBlockPos.set(h, (double) (j + 1), m)) + 0.001,
                            m - g,
                            h + g,
                            (double) this.mob.getBbHeight() + getFloorLevel(this.level, mutableBlockPos.set((double) node.x, (double) node.y, (double) node.z)) - 0.002,
                            m + g
                        );
                        if (this.hasCollisions(aABB)) {
                            node = null;
                        }
                    }
                }
                
                if (blockPathTypes2 == BlockPathTypes.WATER && !this.canFloat()) {
                    if (this.getCachedBlockType(this.mob, i, j - 1, k) != BlockPathTypes.WATER) {
                        return node;
                    }
                    
                    while (j > Offlimits.LEVEL.getMinBuildHeight()) {
                        blockPathTypes2 = this.getCachedBlockType(this.mob, i, --j, k);
                        if (blockPathTypes2 != BlockPathTypes.WATER) {
                            return node;
                        }
                        
                        node = this.getNode(i, j, k);
                        node.type = blockPathTypes2;
                        node.costMalus = Math.max(node.costMalus, this.mob.getPathfindingMalus(blockPathTypes2));
                    }
                }
                
                if (blockPathTypes2 == BlockPathTypes.OPEN) {
                    int n = 0;
                    int o = j;
                    
                    while (blockPathTypes2 == BlockPathTypes.OPEN) {
                        if (--j < Offlimits.LEVEL.getMinBuildHeight()) {
                            Node node2 = this.getNode(i, o, k);
                            node2.type = BlockPathTypes.BLOCKED;
                            node2.costMalus = -1.0F;
                            return node2;
                        }
                        
                        if (n++ >= this.mob.getMaxFallDistance()) {
                            Node node2 = this.getNode(i, j, k);
                            node2.type = BlockPathTypes.BLOCKED;
                            node2.costMalus = -1.0F;
                            return node2;
                        }
                        
                        blockPathTypes2 = this.getCachedBlockType(this.mob, i, j, k);
                        f = this.mob.getPathfindingMalus(blockPathTypes2);
                        if (blockPathTypes2 != BlockPathTypes.OPEN && f >= 0.0F) {
                            node = this.getNode(i, j, k);
                            node.type = blockPathTypes2;
                            node.costMalus = Math.max(node.costMalus, f);
                            break;
                        }
                        
                        if (f < 0.0F) {
                            Node node2 = this.getNode(i, j, k);
                            node2.type = BlockPathTypes.BLOCKED;
                            node2.costMalus = -1.0F;
                            return node2;
                        }
                    }
                }
                
                if (blockPathTypes2 == BlockPathTypes.FENCE) {
                    node = this.getNode(i, j, k);
                    node.closed = true;
                    node.type = blockPathTypes2;
                    node.costMalus = blockPathTypes2.getMalus();
                }
                
            }
            return node;
        }
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    public static BlockPathTypes getBlockPathTypeStatic(BlockGetter level, BlockPos.MutableBlockPos pos) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        BlockPathTypes blockPathTypes = getBlockPathTypeRaw(level, pos);
        if (blockPathTypes == BlockPathTypes.OPEN && j >= Offlimits.LEVEL.getMinBuildHeight() + 1) {
            BlockPathTypes blockPathTypes2 = getBlockPathTypeRaw(level, pos.set(i, j - 1, k));
            blockPathTypes = blockPathTypes2 != BlockPathTypes.WALKABLE
                && blockPathTypes2 != BlockPathTypes.OPEN
                && blockPathTypes2 != BlockPathTypes.WATER
                && blockPathTypes2 != BlockPathTypes.LAVA
                ? BlockPathTypes.WALKABLE
                : BlockPathTypes.OPEN;
            if (blockPathTypes2 == BlockPathTypes.DAMAGE_FIRE) {
                blockPathTypes = BlockPathTypes.DAMAGE_FIRE;
            }
            
            if (blockPathTypes2 == BlockPathTypes.DAMAGE_CACTUS) {
                blockPathTypes = BlockPathTypes.DAMAGE_CACTUS;
            }
            
            if (blockPathTypes2 == BlockPathTypes.DAMAGE_OTHER) {
                blockPathTypes = BlockPathTypes.DAMAGE_OTHER;
            }
            
            if (blockPathTypes2 == BlockPathTypes.STICKY_HONEY) {
                blockPathTypes = BlockPathTypes.STICKY_HONEY;
            }
        }
        
        if (blockPathTypes == BlockPathTypes.WALKABLE) {
            blockPathTypes = checkNeighbourBlocks(level, pos.set(i, j, k), blockPathTypes);
        }
        
        return blockPathTypes;
    }
}
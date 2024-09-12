package com.blackgear.offlimits.core.mixin.common.level.entity;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(PathNavigation.class)
public abstract class PathNavigationMixin {
    @Shadow @Final protected Mob mob;
    @Shadow @Nullable protected Path path;
    @Shadow @Final protected Level level;
    @Shadow @Final private PathFinder pathFinder;
    @Shadow private float maxVisitedNodesMultiplier;
    @Shadow private BlockPos targetPos;
    @Shadow private int reachRange;
    @Shadow protected abstract boolean canUpdatePath();
    @Shadow protected abstract void resetStuckTimeout();
    
    /**
     * @author
     * @reason
     */
    @Overwrite @Nullable
    public Path createPath(Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy) {
        if (targets.isEmpty()) {
            // If there are no targets, return null.
            return null;
        } else if (this.mob.getY() < (double) Offlimits.LEVEL.getMinBuildHeight()) {
            // If the entity's Y-coordinate is below the minimum build height, return null.
            return null;
        } else if (!this.canUpdatePath()) {
            // If the path cannot be updated, return null.
            return null;
        } else if (this.path != null && !this.path.isDone() && targets.contains(this.targetPos)) {
            // Return the existing path if it is not done and the target position is in the targets set.
            return this.path;
        } else {
            // Start profiling the pathfinding process.
            this.level.getProfiler().push("pathfind");
            
            float followRange = (float) this.mob.getAttributeValue(Attributes.FOLLOW_RANGE);
            BlockPos pos = offsetUpward
                ? this.mob.blockPosition().above()
                : this.mob.blockPosition();
            int searchRadius = (int) (followRange + (float) regionOffset);
            PathNavigationRegion region = new PathNavigationRegion(
                this.level,
                pos.offset(-searchRadius, -searchRadius, -searchRadius),
                pos.offset(searchRadius, searchRadius, searchRadius)
            );
            Path path = this.pathFinder.findPath(region, this.mob, targets, followRange, accuracy, this.maxVisitedNodesMultiplier);
            
            // Stop profiling the pathfinding process.
            this.level.getProfiler().pop();
            
            // If a path is found, and it has a target, update the position and reach range, then reset the stuck timeout.
            if (path != null && path.getTarget() != null) {
                this.targetPos = path.getTarget();
                this.reachRange = accuracy;
                this.resetStuckTimeout();
            }
            
            return path;
        }
    }
}
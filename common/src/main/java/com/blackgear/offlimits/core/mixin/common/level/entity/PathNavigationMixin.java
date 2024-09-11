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
    
    @Shadow protected abstract boolean canUpdatePath();
    
    @Shadow @Nullable protected Path path;
    
    @Shadow @Final protected Level level;
    
    @Shadow @Final private PathFinder pathFinder;
    
    @Shadow private float maxVisitedNodesMultiplier;
    
    @Shadow private BlockPos targetPos;
    
    @Shadow private int reachRange;
    
    @Shadow protected abstract void resetStuckTimeout();
    
    /**
     * @author
     * @reason
     */
    @Overwrite @Nullable
    public Path createPath(Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy) {
        if (targets.isEmpty()) {
            return null;
        } else if (this.mob.getY() < (double) Offlimits.LEVEL.getMinBuildHeight()) {
            return null;
        } else if (!this.canUpdatePath()) {
            return null;
        } else if (this.path != null && !this.path.isDone() && targets.contains(this.targetPos)) {
            return this.path;
        } else {
            this.level.getProfiler().push("pathfind");
            float f = (float)this.mob.getAttributeValue(Attributes.FOLLOW_RANGE);
            BlockPos blockPos = offsetUpward ? this.mob.blockPosition().above() : this.mob.blockPosition();
            int i = (int)(f + (float)regionOffset);
            PathNavigationRegion pathNavigationRegion = new PathNavigationRegion(this.level, blockPos.offset(-i, -i, -i), blockPos.offset(i, i, i));
            Path path = this.pathFinder.findPath(pathNavigationRegion, this.mob, targets, f, accuracy, this.maxVisitedNodesMultiplier);
            this.level.getProfiler().pop();
            if (path != null && path.getTarget() != null) {
                this.targetPos = path.getTarget();
                this.reachRange = accuracy;
                this.resetStuckTimeout();
            }
            
            return path;
        }
    }
}
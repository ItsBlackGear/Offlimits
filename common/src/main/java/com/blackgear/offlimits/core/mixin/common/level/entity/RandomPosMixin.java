package com.blackgear.offlimits.core.mixin.common.level.entity;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

@Mixin(RandomPos.class)
public abstract class RandomPosMixin {
    @Shadow @Nullable private static BlockPos getRandomDelta(Random random, int horizontalRange, int verticalRange, int additionalHeight, @Nullable Vec3 offset, double angleRange) { return null; }
    @Shadow static BlockPos moveUpToAboveSolid(BlockPos pos, int minY, int maxY, Predicate<BlockPos> predicate) { return null; }
    
    @Inject(
        method = "generateRandomPos",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void offlimits$generateRandomPos(
        PathfinderMob mob,
        int horizontalRange,
        int verticalRange,
        int additionalHeight,
        @Nullable Vec3 offset,
        boolean avoidWater,
        double angleRange,
        ToDoubleFunction<BlockPos> scoreFunction,
        boolean moveAroundSolid,
        int maxSolidMove,
        int minSolidMove,
        boolean checkStableDestination,
        CallbackInfoReturnable<Vec3> cir
    ) {
        PathNavigation navigation = mob.getNavigation();
        Random random = mob.getRandom();
        
        boolean withinRestriction = mob.hasRestriction() && mob.getRestrictCenter().closerThan(mob.position(), (double) (mob.getRestrictRadius() + (float) horizontalRange) + 1.0);
        
        boolean foundValidPos = false;
        double highestScore = Double.NEGATIVE_INFINITY;
        BlockPos bestPos = mob.blockPosition();
        
        for(int i = 0; i < 10; ++i) {
            BlockPos deltaPos = getRandomDelta(random, horizontalRange, verticalRange, additionalHeight, offset, angleRange);
            
            if (deltaPos == null) continue;
            
            int deltaX = deltaPos.getX();
            int deltaY = deltaPos.getY();
            int deltaZ = deltaPos.getZ();
            
            if (mob.hasRestriction() && horizontalRange > 1) {
                BlockPos restrictCenter = mob.getRestrictCenter();
                
                if (mob.getX() > (double) restrictCenter.getX()) {
                    deltaX -= random.nextInt(horizontalRange / 2);
                } else {
                    deltaX += random.nextInt(horizontalRange / 2);
                }
                
                if (mob.getZ() > (double) restrictCenter.getZ()) {
                    deltaZ -= random.nextInt(horizontalRange / 2);
                } else {
                    deltaZ += random.nextInt(horizontalRange / 2);
                }
            }
            
            BlockPos candidatePos = new BlockPos((double)deltaX + mob.getX(), (double)deltaY + mob.getY(), (double)deltaZ + mob.getZ());
            
            if (
                candidatePos.getY() >= Offlimits.LEVEL.getMinBuildHeight()
                && candidatePos.getY() <= mob.level.getMaxBuildHeight()
                && (!withinRestriction || mob.isWithinRestriction(candidatePos))
                && (!checkStableDestination || navigation.isStableDestination(candidatePos))
            ) {
                if (moveAroundSolid) {
                    candidatePos = moveUpToAboveSolid(
                        candidatePos,
                        random.nextInt(maxSolidMove + 1) + minSolidMove,
                        mob.level.getMaxBuildHeight(),
                        pos -> mob.level.getBlockState(pos).getMaterial().isSolid()
                    );
                }
                
                if (avoidWater || !mob.level.getFluidState(candidatePos).is(FluidTags.WATER)) {
                    BlockPathTypes blockPath = WalkNodeEvaluator.getBlockPathTypeStatic(mob.level, candidatePos.mutable());
                    if (mob.getPathfindingMalus(blockPath) == 0.0F) {
                        double score = scoreFunction.applyAsDouble(candidatePos);
                        if (score > highestScore) {
                            highestScore = score;
                            bestPos = candidatePos;
                            foundValidPos = true;
                        }
                    }
                }
            }
        }
        
        cir.setReturnValue(foundValidPos ? Vec3.atBottomCenterOf(bestPos) : null);
    }
}
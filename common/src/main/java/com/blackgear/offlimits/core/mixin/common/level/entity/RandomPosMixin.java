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
    @Shadow @Nullable private static BlockPos getRandomDelta(Random random, int i, int j, int k, @Nullable Vec3 vec3, double d) { return null; }
    @Shadow static BlockPos moveUpToAboveSolid(BlockPos blockPos, int i, int j, Predicate<BlockPos> predicate) { return null; }
    
    @Inject(
        method = "generateRandomPos",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void offlimits$generateRandomPos(
        PathfinderMob mob,
        int x,
        int z,
        int k,
        @Nullable Vec3 vec3,
        boolean bl,
        double d,
        ToDoubleFunction<BlockPos> toDoubleFunction,
        boolean bl2,
        int l,
        int m,
        boolean bl3,
        CallbackInfoReturnable<Vec3> cir
    ) {
        PathNavigation navigation = mob.getNavigation();
        Random random = mob.getRandom();
        boolean bl4;
        if (mob.hasRestriction()) {
            bl4 = mob.getRestrictCenter().closerThan(mob.position(), (double)(mob.getRestrictRadius() + (float)x) + 1.0);
        } else {
            bl4 = false;
        }
        
        boolean bl5 = false;
        double e = Double.NEGATIVE_INFINITY;
        BlockPos blockPos = mob.blockPosition();
        
        for(int n = 0; n < 10; ++n) {
            BlockPos blockPos2 = getRandomDelta(random, x, z, k, vec3, d);
            if (blockPos2 != null) {
                int o = blockPos2.getX();
                int p = blockPos2.getY();
                int q = blockPos2.getZ();
                if (mob.hasRestriction() && x > 1) {
                    BlockPos blockPos3 = mob.getRestrictCenter();
                    if (mob.getX() > (double)blockPos3.getX()) {
                        o -= random.nextInt(x / 2);
                    } else {
                        o += random.nextInt(x / 2);
                    }
                    
                    if (mob.getZ() > (double)blockPos3.getZ()) {
                        q -= random.nextInt(x / 2);
                    } else {
                        q += random.nextInt(x / 2);
                    }
                }
                
                BlockPos blockPos3 = new BlockPos((double)o + mob.getX(), (double)p + mob.getY(), (double)q + mob.getZ());
                if (blockPos3.getY() >= Offlimits.LEVEL.getMinBuildHeight()
                    && blockPos3.getY() <= mob.level.getMaxBuildHeight()
                    && (!bl4 || mob.isWithinRestriction(blockPos3))
                    && (!bl3 || navigation.isStableDestination(blockPos3))) {
                    if (bl2) {
                        blockPos3 = moveUpToAboveSolid(
                            blockPos3,
                            random.nextInt(l + 1) + m,
                            mob.level.getMaxBuildHeight(),
                            blockPosx -> mob.level.getBlockState(blockPosx).getMaterial().isSolid()
                        );
                    }
                    
                    if (bl || !mob.level.getFluidState(blockPos3).is(FluidTags.WATER)) {
                        BlockPathTypes blockPathTypes = WalkNodeEvaluator.getBlockPathTypeStatic(mob.level, blockPos3.mutable());
                        if (mob.getPathfindingMalus(blockPathTypes) == 0.0F) {
                            double f = toDoubleFunction.applyAsDouble(blockPos3);
                            if (f > e) {
                                e = f;
                                blockPos = blockPos3;
                                bl5 = true;
                            }
                        }
                    }
                }
            }
        }
        
        cir.setReturnValue(bl5 ? Vec3.atBottomCenterOf(blockPos) : null);
    }
}
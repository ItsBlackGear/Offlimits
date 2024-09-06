package com.blackgear.offlimits.core.mixin.common.level.carver;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.common.level.Aquifer;
import com.blackgear.offlimits.common.level.levelgen.WorldGenContext;
import com.blackgear.offlimits.common.level.levelgen.carver.NoiseCarver;
import com.blackgear.offlimits.common.level.surface.WorldCarverExtension;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.BitSet;
import java.util.Random;
import java.util.function.Function;

@Mixin(WorldCarver.class)
public abstract class WorldCarverMixin<C> implements WorldCarverExtension {
    @Mutable @Shadow @Final protected int genHeight;
    
    @Unique private Aquifer aquifer;
    @Unique private WorldGenContext context;
    @Unique private NoiseCarver carver;
    
    @Override public void setAquifer(Aquifer aquifer) { this.aquifer = aquifer; }
    @Override public Aquifer aquifer() { return this.aquifer; }
    @Override public void setContext(WorldGenContext context) { this.context = context; }
    @Override public WorldGenContext context() { return context; }
    
    @Inject(method = "<init>", at = @At("RETURN"))
    private void offlimits$init(Codec<C> codec, int i, CallbackInfo ci) {
        this.genHeight = Offlimits.LEVEL.getMaxBuildHeight();
        this.carver = new NoiseCarver((WorldCarver<?>) (Object) this);
    }
    
    @Inject(
        method = "carveSphere",
        at = @At("HEAD"),
        cancellable = true
    )
    private void offlimits$carveSphere(
        ChunkAccess chunk,
        Function<BlockPos, Biome> biomeGetter,
        long seed,
        int seaLevel,
        int chunkX,
        int chunkZ,
        double x,
        double y,
        double z,
        double horizontalRadius,
        double verticalRadius,
        BitSet carvingMask,
        CallbackInfoReturnable<Boolean> cir
    ) {
        cir.setReturnValue(
            this.carver.carveSphere(
                chunk,
                biomeGetter,
                seed,
                seaLevel,
                chunkX,
                chunkZ,
                x,
                y,
                z,
                horizontalRadius,
                verticalRadius,
                carvingMask
            )
        );
    }
    
    @Inject(
        method = "carveBlock",
        at = @At("HEAD"),
        cancellable = true
    )
    private void offlimits$carveBlock(
        ChunkAccess chunk,
        Function<BlockPos, Biome> biomeGetter,
        BitSet carvingMask,
        Random random,
        BlockPos.MutableBlockPos pos,
        BlockPos.MutableBlockPos checkPos,
        BlockPos.MutableBlockPos replaceSurface,
        int startX,
        int endX,
        int startY,
        int endY,
        int startZ,
        int endZ,
        int minBlockX,
        int minBlockZ,
        MutableBoolean reachedSurface,
        CallbackInfoReturnable<Boolean> cir
    ) {
        cir.setReturnValue(
            this.carver.carveBlock(
                chunk,
                biomeGetter,
                carvingMask,
                random,
                pos,
                checkPos,
                replaceSurface,
                startX,
                endX,
                startY,
                endY,
                startZ,
                endZ,
                minBlockX,
                minBlockZ,
                reachedSurface
            )
        );
    }
}
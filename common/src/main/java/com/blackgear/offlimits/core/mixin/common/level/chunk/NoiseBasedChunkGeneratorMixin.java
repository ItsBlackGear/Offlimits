package com.blackgear.offlimits.core.mixin.common.level.chunk;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.common.level.levelgen.ChunkGenContext;
import com.blackgear.offlimits.common.level.levelgen.OfflimitsChunkGenerator;
import com.blackgear.offlimits.common.level.noise.BlockNoiseColumn;
import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.level.levelgen.synth.SurfaceNoise;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin extends ChunkGenerator {
    @Shadow @Final protected WorldgenRandom random;
    
    @Shadow @Final private int chunkWidth;
    @Shadow @Final private int chunkHeight;
    @Shadow @Final private int chunkCountX;
    @Shadow @Final private int chunkCountY;
    @Shadow @Final private int chunkCountZ;
    
    @Shadow @Final protected Supplier<NoiseGeneratorSettings> settings;
    @Shadow @Final private @Nullable SimplexNoise islandNoise;
    @Shadow @Final private PerlinNoise depthNoise;
    @Shadow @Final protected BlockState defaultFluid;
    @Shadow @Final protected BlockState defaultBlock;
    
    @Shadow protected abstract double[] makeAndFillNoiseColumn(int i, int j);
    @Shadow protected abstract void setBedrock(ChunkAccess chunk, Random random);
    @Shadow public abstract int getSeaLevel();
    
    @Shadow @Final private SurfaceNoise surfaceNoise;
    
    @Shadow @Final private int height;
    @Unique private ChunkGenContext context;
    @Unique private OfflimitsChunkGenerator generator;
    
    public NoiseBasedChunkGeneratorMixin(BiomeSource biomeSource, StructureSettings settings) {
        super(biomeSource, settings);
    }
    
    @Inject(
        method = "<init>(Lnet/minecraft/world/level/biome/BiomeSource;Lnet/minecraft/world/level/biome/BiomeSource;JLjava/util/function/Supplier;)V",
        at = @At("RETURN")
    )
    private void init(
        BiomeSource biomeSource,
        BiomeSource runtimeBiomeSource,
        long seed,
        Supplier<NoiseGeneratorSettings> settings,
        CallbackInfo callback
    ) {
        this.context = new ChunkGenContext(this.defaultBlock, this.defaultFluid, this.chunkCountX, this.chunkCountY, this.chunkCountZ, this.chunkWidth, this.chunkHeight, this.getSeaLevel());
        this.generator = new OfflimitsChunkGenerator(this.random, context, this.settings.get());
        this.generator.initialize(biomeSource, seed, this.islandNoise, this.depthNoise);
    }
    
//    @Inject(
//        method = "fillNoiseColumn",
//        at = @At("HEAD"),
//        cancellable = true
//    )
//    private void offlimits$fillNoiseColumn(double[] slices, int x, int z, CallbackInfo callback) {
//        if (!this.context.allowTerrainModifications()) {
//            return;
//        }
//
//        this.generator.fillNoiseColumn(slices, x, z);
//        callback.cancel();
//    }
    
    @Inject(
        method = "buildSurfaceAndBedrock",
        at = @At("HEAD"),
        cancellable = true
    )
    private void offlimits$buildSurfaceAndBedrock(WorldGenRegion level, ChunkAccess chunk, CallbackInfo callback) {
        if (!this.context.allowTerrainModifications()) {
            return;
        }
        
        this.generator.buildSurface(level, chunk, this.surfaceNoise);
        this.setBedrock(chunk, random);
        
        callback.cancel();
    }
    
    @Override
    public void applyCarvers(long seed, BiomeManager biomeManager, ChunkAccess chunk, GenerationStep.Carving carving) {
//        if (!this.context.allowTerrainModifications()) {
//            super.applyCarvers(seed, biomeManager, chunk, carving);
//        }
//
//        this.generator.applyCarvers(seed, biomeManager, this.biomeSource, chunk, carving);
    }
    
//    @Inject(
//        method = "iterateNoiseColumn",
//        at = @At("HEAD"),
//        cancellable = true
//    )
//    private void offlimits$iterateNoiseColumn(int x, int z, BlockState[] states, Predicate<BlockState> predicate, CallbackInfoReturnable<Integer> callback) {
//        if (!this.context.allowTerrainModifications()) {
//            return;
//        }
//
//        int k;
//        int l;
//
//        int chunkX = Math.floorDiv(x, this.context.chunkWidth());
//        int chunkZ = Math.floorDiv(z, this.context.chunkWidth());
//        int offsetX = Math.floorMod(x, this.context.chunkWidth());
//        int offsetZ = Math.floorMod(z, this.context.chunkWidth());
//        double noiseX = (double)offsetX / (double)this.context.chunkWidth();
//        double noiseZ = (double)offsetZ / (double)this.context.chunkWidth();
//        double[][] slices = new double[][] {
//            this.generator.makeAndFillNoiseColumn(chunkX, chunkZ, k, l),
//            this.generator.makeAndFillNoiseColumn(chunkX, chunkZ + 1, k, l),
//            this.generator.makeAndFillNoiseColumn(chunkX + 1, chunkZ, k, l),
//            this.generator.makeAndFillNoiseColumn(chunkX + 1, chunkZ + 1, k, l)
//        };
//
//        callback.setReturnValue(this.generator.iterateNoiseColumn(slices, x, z, noiseX, noiseZ, states, predicate, k, l));
//    }
    
    @Inject(
        method = "getBaseHeight",
        at = @At("HEAD"),
        cancellable = true
    )
    public void getBaseHeight(int i, int j, Heightmap.Types types, CallbackInfoReturnable<Integer> cir) {
        if (!this.context.allowTerrainModifications()) {
            return;
        }
        
        NoiseSettings settings = this.settings.get().noiseSettings();
        int k = Offlimits.INSTANCE.getMinBuildHeight();
        int l = Math.min(k + settings.height(), Offlimits.INSTANCE.getMaxBuildHeight());
        int m = Mth.intFloorDiv(k, this.chunkHeight);
        int n = Mth.intFloorDiv(l - k, this.chunkHeight);
        cir.setReturnValue(
            n <= 0
                ? Offlimits.INSTANCE.getMinBuildHeight()
                : this.generator.iterateNoiseColumn(i, j, null, types.isOpaque(), m, n).orElse(Offlimits.INSTANCE.getMinBuildHeight())
        );
    }

    @Inject(
        method = "getBaseColumn",
        at = @At("HEAD"),
        cancellable = true
    )
    public void getBaseColumn(int i, int j, CallbackInfoReturnable<BlockGetter> cir) {
        if (!this.context.allowTerrainModifications()) {
            return;
        }
        
        NoiseSettings settings = this.settings.get().noiseSettings();
        int k = Offlimits.INSTANCE.getMinBuildHeight();
        int l = Math.min(k + settings.height(), Offlimits.INSTANCE.getMaxBuildHeight());
        int m = Mth.intFloorDiv(k, this.chunkHeight);
        int n = Mth.intFloorDiv(l - k, this.chunkHeight);
        
        if (n <= 0) {
            cir.setReturnValue(new BlockNoiseColumn(k, new BlockState[0]));
        } else {
            BlockState[] states = new BlockState[n * this.chunkHeight];
            this.generator.iterateNoiseColumn(i, j, states, null, m, n);
            cir.setReturnValue(new BlockNoiseColumn(k, states));
        }
    }
    
    @Inject(
        method = "setBedrock",
        at = @At("HEAD"),
        cancellable = true
    )
    private void off$setBedrock(ChunkAccess chunk, Random random, CallbackInfo ci) {
        if (!this.context.allowTerrainModifications()) {
            return;
        }
        
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int i = chunk.getPos().getMinBlockX();
        int j = chunk.getPos().getMinBlockZ();
        NoiseGeneratorSettings noiseGeneratorSettings = this.settings.get();
        int k = Offlimits.INSTANCE.getMinBuildHeight();
        int l = k - noiseGeneratorSettings.getBedrockFloorPosition();
        int m = this.height - 1 + k - noiseGeneratorSettings.getBedrockRoofPosition();
        int n = 5;
        int o = Offlimits.INSTANCE.getMinBuildHeight();
        int p = Offlimits.INSTANCE.getMaxBuildHeight();
        boolean bl = m + n - 1 >= o && m < p;
        boolean bl2 = l + n - 1 >= o && l < p;
        if (bl || bl2) {
            for (BlockPos pos : BlockPos.betweenClosed(i, 0, j, i + 15, 0, j + 15)) {
//                if (bl) {
//                    for (int q = 0; q < 5; q++) {
//                        if (q <= random.nextInt(n)) {
//                            chunk.setBlockState(mutable.set(pos.getX(), m - q, pos.getZ()), Blocks.BEDROCK.defaultBlockState(), false);
//                        }
//                    }
//                }
                
                if (bl2) {
                    for (int q = 4; q >= 0; q--) {
                        if (q <= random.nextInt(5)) {
                            chunk.setBlockState(mutable.set(pos.getX(), l + q, pos.getZ()), Blocks.BEDROCK.defaultBlockState(), false);
                        }
                    }
                }
            }
        }
        
        ci.cancel();
    }
    
    @Inject(
        method = "fillFromNoise",
        at = @At("HEAD"),
        cancellable = true
    )
    private void offlimits$fillFromNoise(LevelAccessor level, StructureFeatureManager featureManager, ChunkAccess chunk, CallbackInfo ci) {
        if (!this.context.allowsTerrainModifications(level)) {
            return;
        }

        NoiseSettings settings = this.settings.get().noiseSettings();
        int i = Math.max(this.context.minY(), Offlimits.INSTANCE.getMinBuildHeight());
        int j = Math.min(this.context.minY() + settings.height(), chunk.getMaxBuildHeight());
        int k = Mth.intFloorDiv(i, this.chunkHeight);
        int l = Mth.intFloorDiv(j - i, this.chunkHeight);

        if (l <= 0) {
            ci.cancel();
        }

        int m = Offlimits.INSTANCE.getSectionIndex(l * this.chunkHeight - 1 + i);
        int n = Offlimits.INSTANCE.getSectionIndex(i);

        // Use a thread pool for parallel processing
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<Void>> futures = new ArrayList<>();

        Set<LevelChunkSection> sections = Sets.newHashSet();

        try {
            // Submit tasks for parallel execution
            for (int mxx = m; mxx >= n; mxx--) {
                final int sectionY = mxx;
                futures.add(executor.submit(() -> {
                    LevelChunkSection section = ((ProtoChunk) chunk).getOrCreateSection(sectionY);
                    section.acquire();
                    synchronized (sections) {
                        sections.add(section);
                    }
                    return null;
                }));
            }

            // Wait for all tasks to complete
            for (Future<Void> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Call generator after acquiring all necessary sections
            this.generator.fillFromNoise(featureManager, chunk, k, l);
        } finally {
            // Ensure all sections are released
            for (LevelChunkSection section : sections) {
                section.release();
            }
            
            executor.shutdown(); // Shut down the executor
        }

        ci.cancel();
    }
}
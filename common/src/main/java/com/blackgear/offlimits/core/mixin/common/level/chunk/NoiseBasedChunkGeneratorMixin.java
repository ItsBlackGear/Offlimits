package com.blackgear.offlimits.core.mixin.common.level.chunk;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.common.level.levelgen.ChunkGenContext;
import com.blackgear.offlimits.common.level.levelgen.OfflimitsChunkGenerator;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.function.Predicate;
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
    
    @Inject(
        method = "fillNoiseColumn",
        at = @At("HEAD"),
        cancellable = true
    )
    private void offlimits$fillNoiseColumn(double[] slices, int x, int z, CallbackInfo callback) {
        if (!this.context.allowTerrainModifications()) {
            return;
        }
        
        this.generator.fillNoiseColumn(slices, x, z);
        callback.cancel();
    }
    
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
        if (!this.context.allowTerrainModifications()) {
            super.applyCarvers(seed, biomeManager, chunk, carving);
        }
        
        this.generator.applyCarvers(seed, biomeManager, this.biomeSource, chunk, carving);
    }
    
    @Inject(
        method = "iterateNoiseColumn",
        at = @At("HEAD"),
        cancellable = true
    )
    private void offlimits$iterateNoiseColumn(int x, int z, BlockState[] states, Predicate<BlockState> predicate, CallbackInfoReturnable<Integer> callback) {
        if (!this.context.allowTerrainModifications()) {
            return;
        }
        
        int chunkX = Math.floorDiv(x, this.context.chunkWidth());
        int chunkZ = Math.floorDiv(z, this.context.chunkWidth());
        int offsetX = Math.floorMod(x, this.context.chunkWidth());
        int offsetZ = Math.floorMod(z, this.context.chunkWidth());
        double noiseX = (double)offsetX / (double)this.context.chunkWidth();
        double noiseZ = (double)offsetZ / (double)this.context.chunkWidth();
        double[][] slices = new double[][] {
            this.makeAndFillNoiseColumn(chunkX, chunkZ),
            this.makeAndFillNoiseColumn(chunkX, chunkZ + 1),
            this.makeAndFillNoiseColumn(chunkX + 1, chunkZ),
            this.makeAndFillNoiseColumn(chunkX + 1, chunkZ + 1)
        };
        
        callback.setReturnValue(this.generator.iterateNoiseColumn(slices, x, z, noiseX, noiseZ, states, predicate));
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
        int minY = this.context.minY();
        int maxY = Math.min(minY + settings.height(), chunk.getMaxBuildHeight());
        int chunkHeight = this.chunkHeight;

        int minChunkY = Mth.intFloorDiv(minY, chunkHeight);
        int chunkCountY = Mth.intFloorDiv(maxY - minY, chunkHeight);

        if (chunkCountY <= 0) {
            ci.cancel();
        }

        int maxSectionY = Offlimits.INSTANCE.getSectionIndex(chunkCountY * chunkHeight - 1 + minY);
        int minSectionY = Offlimits.INSTANCE.getSectionIndex(minY);

        // Use a thread pool for parallel processing
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<Void>> futures = new ArrayList<>();

        List<LevelChunkSection> sections = new ArrayList<>(maxSectionY - minSectionY + 1);

        try {
            // Submit tasks for parallel execution
            for (int sectionY = maxSectionY; sectionY >= minSectionY; sectionY--) {
                final int currentSectionY = sectionY;
                futures.add(executor.submit(() -> {
                    LevelChunkSection section = ((ProtoChunk) chunk).getOrCreateSection(currentSectionY);
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
            this.generator.fillFromNoise(featureManager, chunk, minChunkY, chunkCountY);
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
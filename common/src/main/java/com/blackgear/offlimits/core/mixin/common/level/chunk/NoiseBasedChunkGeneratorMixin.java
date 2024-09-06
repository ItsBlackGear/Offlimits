package com.blackgear.offlimits.core.mixin.common.level.chunk;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.common.level.levelgen.WorldGenContext;
import com.blackgear.offlimits.common.level.levelgen.chunk.NoiseChunk;
import com.blackgear.offlimits.common.level.levelgen.chunk.OfflimitsNoiseChunk;
import com.blackgear.offlimits.common.level.levelgen.TerrainContext;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.level.levelgen.synth.SurfaceNoise;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin extends ChunkGenerator {
    @Shadow @Final private int chunkCountX;
    @Mutable @Shadow @Final private int chunkCountY;
    @Shadow @Final private int chunkCountZ;
    @Shadow @Final private int chunkHeight;
    @Shadow @Final private int chunkWidth;
    @Shadow @Final protected WorldgenRandom random;
    
    @Shadow @Final protected Supplier<NoiseGeneratorSettings> settings;
    @Shadow @Final private @Nullable SimplexNoise islandNoise;
    @Shadow @Final private SurfaceNoise surfaceNoise;
    @Shadow @Final private PerlinNoise depthNoise;
    
    @Unique private NoiseChunk noiseChunk;
    @Unique private TerrainContext terrainContext;
    @Unique private WorldGenContext worldGenContext;
    
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
        this.chunkCountY = Offlimits.LEVEL.getHeight() / this.chunkHeight;
        
        this.terrainContext = new TerrainContext(this.settings.get(), this.chunkCountX, this.chunkCountY, this.chunkCountZ, this.chunkWidth, this.chunkHeight, this.settings.get().seaLevel());
        this.worldGenContext = new WorldGenContext(this.settings.get(), this.terrainContext);
        
        this.noiseChunk = new OfflimitsNoiseChunk(this.terrainContext, this.settings.get(), this.random);
        this.noiseChunk.initialize(biomeSource, seed, this.islandNoise, this.depthNoise);
    }
    
    @Inject(
        method = "buildSurfaceAndBedrock",
        at = @At("HEAD"),
        cancellable = true
    )
    private void offlimits$buildSurfaceAndBedrock(WorldGenRegion region, ChunkAccess chunk, CallbackInfo ci) {
        this.noiseChunk.buildSurface(region, chunk, this.surfaceNoise);
        ci.cancel();
    }
    
    @Override
    public void applyCarvers(long seed, BiomeManager biomeManager, ChunkAccess chunk, GenerationStep.Carving carving) {
        this.noiseChunk.applyCarvers(seed, biomeManager, this.biomeSource, chunk, carving, this.worldGenContext);
    }
    
    @Inject(
        method = "getBaseHeight",
        at = @At("HEAD"),
        cancellable = true
    )
    public void getBaseHeight(int x, int z, Heightmap.Types types, CallbackInfoReturnable<Integer> cir) {
        if (!this.worldGenContext.shouldGenerate()) return;
        
        cir.setReturnValue(this.noiseChunk.getBaseHeight(x, z, types));
    }

    @Inject(
        method = "getBaseColumn",
        at = @At("HEAD"),
        cancellable = true
    )
    public void getBaseColumn(int x, int z, CallbackInfoReturnable<BlockGetter> cir) {
        if (!this.worldGenContext.shouldGenerate()) return;
        
        cir.setReturnValue(this.noiseChunk.getBaseColumn(x, z));
    }

    @Inject(
        method = "fillFromNoise",
        at = @At("HEAD"),
        cancellable = true
    )
    private void offlimits$fillFromNoise(LevelAccessor level, StructureFeatureManager structureManager, ChunkAccess chunk, CallbackInfo ci) {
        if (!this.worldGenContext.shouldGenerate()) return;
        
        int genDepth = Math.max(this.terrainContext.minY(), this.terrainContext.minBuildHeight());
        int genHeight = Math.min(this.terrainContext.minY() + this.terrainContext.height(), this.terrainContext.maxBuildHeight());
        
        this.noiseChunk.fillFromNoise(level, structureManager, chunk, genDepth, genHeight);
        ci.cancel();
    }
}
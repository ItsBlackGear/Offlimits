package com.blackgear.offlimits.common.level.levelgen.chunk;

import com.blackgear.offlimits.common.level.Aquifer;
import com.blackgear.offlimits.common.level.levelgen.TerrainContext;
import com.blackgear.offlimits.common.level.levelgen.WorldGenContext;
import com.blackgear.offlimits.common.level.levelgen.sampler.NoiseSampler;
import com.blackgear.offlimits.common.level.levelgen.sampler.OfflimitsNoiseSampler;
import com.blackgear.offlimits.common.level.noise.BlendedNoise;
import com.blackgear.offlimits.common.level.levelgen.noisemodifiers.NoiseModifier;
import com.blackgear.offlimits.common.level.noise.NoiseSettingsExtension;
import com.blackgear.offlimits.common.level.surface.BiomeExtension;
import com.blackgear.offlimits.common.level.surface.WorldCarverExtension;
import com.blackgear.offlimits.core.mixin.common.ConfiguredWorldCarverAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.level.levelgen.synth.SurfaceNoise;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.function.Supplier;

public abstract class NoiseChunk {
    protected final TerrainContext context;
    protected final NoiseGeneratorSettings settings;
    protected final WorldgenRandom random;
    protected NoiseSampler sampler;
    
    protected NoiseChunk(TerrainContext context, NoiseGeneratorSettings settings, WorldgenRandom random) {
        this.context = context;
        this.settings = settings;
        this.random = random;
    }
    
    public abstract void initialize(BiomeSource source, long seed, @Nullable SimplexNoise islandNoise, PerlinNoise depthNoise);
    
    public void buildSurface(WorldGenRegion region, ChunkAccess chunk, SurfaceNoise surface) {
        ChunkPos chunkPos = chunk.getPos();
        WorldgenRandom random = new WorldgenRandom();
        random.setBaseChunkSeed(chunkPos.x, chunkPos.z);
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();
        double y = 0.0625;
        
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        
        for (int chunkX = 0; chunkX < 16; chunkX++) {
            for (int chunkZ = 0; chunkZ < 16; chunkZ++) {
                int localX = startX + chunkX;
                int localZ = startZ + chunkZ;
                int startHeight = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, chunkX, chunkZ) + 1;
                double noise = surface.getSurfaceNoiseValue((double) localX * y, (double) localZ * y, y, (double) chunkX * y) * 15.0;
                mutable.set(startX + chunkX, -64, startZ + chunkZ);
                
                int surfaceLevel = this.sampler.getPreliminarySurfaceLevel(mutable.getX(), mutable.getY(), mutable.getZ());
                int minSurfaceLevel = surfaceLevel - 16;
                
                Biome biome = region.getBiome(new BlockPos(mutable.getX(), surfaceLevel, mutable.getZ()));
                ((BiomeExtension) biome).setMinSurfaceLevel(minSurfaceLevel);
                
                biome.buildSurfaceAt(random, chunk, localX, localZ, startHeight, noise, this.context.defaultBlock(), this.context.defaultFluid(), this.context.seaLevel(), region.getSeed());
            }
        }
        
        this.setBedrock(chunk, random);
    }
    
    private void setBedrock(ChunkAccess chunk, Random random) {
        NoiseGeneratorSettings settings = this.settings;
        TerrainContext context = this.context;
        
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockX();
        int startY = this.minY();
        
        int floorY = startY + settings.getBedrockFloorPosition();
        int roofY = this.height() - 1 + startY - settings.getBedrockRoofPosition();
        int minBuildHeight = context.minBuildHeight();
        int maxBuildHeight = context.maxBuildHeight();
        
        boolean generateRoof = roofY + 5 - 1 >= minBuildHeight && roofY < maxBuildHeight;
        boolean generateFloor = floorY + 5 - 1 >= minBuildHeight && floorY < maxBuildHeight;
        
        if (generateRoof || generateFloor) {
            for (BlockPos pos : BlockPos.betweenClosed(startX, 0, startZ, startX + 15, 0, startZ + 15)) {
                if (generateRoof) {
                    for (int i = 0; i < 5; ++i) {
                        if (i <= random.nextInt(5)) {
                            chunk.setBlockState(mutable.set(pos.getX(), roofY - i, pos.getZ()), Blocks.BEDROCK.defaultBlockState(), false);
                        }
                    }
                }
                
                if (generateFloor) {
                    for (int i = 4; i >= 0; --i) {
                        if (i <= random.nextInt(5)) {
                            chunk.setBlockState(mutable.set(pos.getX(), floorY + i, pos.getZ()), Blocks.BEDROCK.defaultBlockState(), false);
                        }
                    }
                }
            }
        }
    }
    
    public void applyCarvers(long seed, BiomeManager biomeManager, BiomeSource biomeSource, ChunkAccess chunk, GenerationStep.Carving carving, WorldGenContext context) {
        BiomeManager biomeGetter = biomeManager.withDifferentSource(biomeSource);
        WorldgenRandom random = new WorldgenRandom();
        ChunkPos chunkPos = chunk.getPos();
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;
        BiomeGenerationSettings generationSettings = biomeSource.getNoiseBiome(chunkPos.x << 2, 0, chunkPos.z << 2).getGenerationSettings();
        BitSet carvingMask = ((ProtoChunk) chunk).getOrCreateCarvingMask(carving);
        
        for(int localX = chunkX - 8; localX <= chunkX + 8; ++localX) {
            for(int localZ = chunkZ - 8; localZ <= chunkZ + 8; ++localZ) {
                List<Supplier<ConfiguredWorldCarver<?>>> carvers = generationSettings.getCarvers(carving);
                ListIterator<Supplier<ConfiguredWorldCarver<?>>> iterator = carvers.listIterator();
                
                while(iterator.hasNext()) {
                    int index = iterator.nextIndex();
                    ConfiguredWorldCarver<?> carver = iterator.next().get();
                    WorldCarverExtension extension = ((WorldCarverExtension) ((ConfiguredWorldCarverAccessor) carver).getWorldCarver());
                    extension.setAquifer(this.createAquifer(chunk));
                    extension.setContext(context);
                    
                    random.setLargeFeatureSeed(seed + (long)index, localX, localZ);
                    if (carver.isStartChunk(random, localX, localZ)) {
                        carver.carve(chunk, biomeGetter::getBiome, random, this.context.seaLevel(), localX, localZ, chunkX, chunkZ, carvingMask);
                    }
                }
            }
        }
    }
    
    public abstract int getBaseHeight(int x, int z, Heightmap.Types types);
    
    public abstract BlockGetter getBaseColumn(int x, int z);
    
    double[] makeAndFillNoiseColumn(int x, int z, int minY, int chunkCountY) {
        double[] buffer = new double[chunkCountY + 1];
        this.fillNoiseColumn(buffer, x, z, minY, chunkCountY);
        return buffer;
    }
    
    public void fillNoiseColumn(double[] buffer, int x, int z, int minY, int chunkCountY) {
        this.sampler.fillNoiseColumn(buffer, x, z, this.settings.noiseSettings(), this.context.seaLevel(), minY, chunkCountY);
    }
    
    public abstract void fillFromNoise(LevelAccessor level, StructureFeatureManager structureManager, ChunkAccess chunk, int genDepth, int genHeight);
    
    public NoiseSampler createNoiseSampler(BiomeSource source, BlendedNoise blendedNoise, SimplexNoise islandNoise, PerlinNoise depthNoise, NoiseModifier caveNoiseModifier) {
        return new OfflimitsNoiseSampler(
            source,
            this.context,
            this.settings.noiseSettings(),
            blendedNoise,
            islandNoise,
            depthNoise,
            caveNoiseModifier
        );
    }
    
    public Aquifer createAquifer(ChunkAccess chunk) {
        return Aquifer.createDisabled(this.context.seaLevel(), this.context.defaultFluid());
    }
    
    public int height() {
        return this.settings.noiseSettings().height();
    }
    
    public int minY() {
        return ((NoiseSettingsExtension) this.settings.noiseSettings()).minY();
    }
}
package com.blackgear.offlimits.common.level.levelgen;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.common.level.Aquifer;
import com.blackgear.offlimits.common.level.Beardifier;
import com.blackgear.offlimits.common.level.Cavifier;
import com.blackgear.offlimits.common.level.NoodleCavifier;
import com.blackgear.offlimits.common.level.levelgen.stonesource.SimpleStoneSource;
import com.blackgear.offlimits.common.level.noise.BlendedNoise;
import com.blackgear.offlimits.common.level.noise.NoiseInterpolator;
import com.blackgear.offlimits.common.level.noise.NoiseModifier;
import com.blackgear.offlimits.common.level.noise.NoiseSampler;
import com.blackgear.offlimits.common.level.noiseModifiers.NoodleCaveNoiseModifier;
import com.blackgear.offlimits.common.level.surface.BiomeExtension;
import com.blackgear.offlimits.common.level.surface.WorldCarverExtension;
import com.blackgear.offlimits.common.utils.NoiseUtils;
import com.blackgear.offlimits.core.mixin.ProtoChunkAccessor;
import com.blackgear.offlimits.core.mixin.common.ConfiguredWorldCarverAccessor;
import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.level.levelgen.synth.SurfaceNoise;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class OfflimitsChunkGenerator {
    private final WorldgenRandom random;
    private final ChunkGenContext context;
    private final NoiseGeneratorSettings settings;
    
    private NormalNoise barrierNoise, waterLevelNoise, lavaNoise;
    
    private NoiseSampler sampler;
    private NoodleCavifier noodleCavifier;
    
    public OfflimitsChunkGenerator(WorldgenRandom random, ChunkGenContext context, NoiseGeneratorSettings settings) {
        this.random = random;
        this.context = context;
        this.settings = settings;
    }
    
    public void initialize(BiomeSource biomeSource, long seed, SimplexNoise islandNoise, PerlinNoise depthNoise) {
        BlendedNoise blendedNoise = new BlendedNoise(this.random);
        this.barrierNoise = NoiseUtils.normal(this.random.nextLong(), -3, 1.0);
        this.waterLevelNoise = NoiseUtils.normal(this.random.nextLong(), -3, 1.0, 0.0, 2.0);
        this.lavaNoise = NoiseUtils.normal(this.random.nextLong(), -1, 1.0, 0.0);
        
        NoiseModifier caveNoiseModifier;
        if (Offlimits.CONFIG.areNoiseCavesEnabled.get()) {
            caveNoiseModifier = new Cavifier(this.random, this.context.minY() / this.context.chunkHeight());
        } else {
            caveNoiseModifier = NoiseModifier.PASSTHROUGH;
        }
        
        this.sampler = new NoiseSampler(biomeSource, this.context.chunkWidth(), this.context.chunkHeight(), this.context.chunkCountY(), this.settings.noiseSettings(), blendedNoise, islandNoise, depthNoise, caveNoiseModifier);
        this.noodleCavifier = new NoodleCavifier(this.context.chunkCountY(), seed);
    }
    
    public void buildSurface(WorldGenRegion region, ChunkAccess chunk, SurfaceNoise surfaceNoise) {
        ChunkPos pos = chunk.getPos();
        int chunkX = pos.x;
        int chunkZ = pos.z;
        
        WorldgenRandom random = new WorldgenRandom();
        random.setBaseChunkSeed(chunkX, chunkZ);
        
        int minChunkX = pos.getMinBlockX();
        int minChunkZ = pos.getMinBlockZ();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        
        for(int localX = 0; localX < 16; ++localX) {
            int x = minChunkX + localX;
            double noiseX = (double) x * 0.0625;
            
            for(int localZ = 0; localZ < 16; ++localZ) {
                int z = minChunkZ + localZ;
                double noiseZ = (double) z * 0.0625;
                
                int startHeight = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX, localZ) + 1;
                double noise = surfaceNoise.getSurfaceNoiseValue(noiseX, noiseZ, 0.0625, (double) localX * 0.0625) * 15.0;
                
                mutable.set(x, -64, z);
                
                int preliminarySurfaceLevel = this.sampler.getPreliminarySurfaceLevel(mutable.getX(), mutable.getY(), mutable.getZ());
                int minSurfaceLevel = preliminarySurfaceLevel - 16;
                
                Biome biome = region.getBiome(mutable.set(mutable.getX(), preliminarySurfaceLevel, mutable.getZ()));
                ((BiomeExtension) biome).setPreliminarySurfaceLevel(minSurfaceLevel);
                
                biome.buildSurfaceAt(random, chunk, x, z, startHeight, noise, this.context.defaultBlock(), this.context.defaultFluid(), this.context.seaLevel(), region.getSeed());
            }
        }
    }
    
    public void applyCarvers(long seed, BiomeManager biomeManager, BiomeSource biomeSource, ChunkAccess chunk, GenerationStep.Carving type, WorldGenerationContext context) {
        BiomeManager diffBiomeManager = biomeManager.withDifferentSource(biomeSource);
        ChunkPos chunkPos = chunk.getPos();
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;
        BiomeGenerationSettings generationSettings = biomeSource.getNoiseBiome(chunkPos.x << 2, 0, chunkPos.z << 2).getGenerationSettings();
        BitSet carvingMask = ((ProtoChunkAccessor) chunk).getCarvingMasks().computeIfAbsent(type, carving -> {
            if (context.shouldGenerate()) {
                return new BitSet(98304);
            }
            
            return new BitSet(65536);
        });
        WorldgenRandom random = new WorldgenRandom();
        
        List<Supplier<ConfiguredWorldCarver<?>>> carvers = generationSettings.getCarvers(type);
        int size = carvers.size();
        
        int minX = chunkX - 8;
        int maxX = chunkX + 8;
        int minZ = chunkZ - 8;
        int maxZ = chunkZ + 8;
        
        for (int x = minX; x <= maxX; ++x) {
            for (int z = minZ; z <= maxZ; ++z) {
                for (int index = 0; index < size; index++) {
                    ConfiguredWorldCarver<?> carver = carvers.get(index).get();
                    random.setLargeFeatureSeed(seed + (long) index, x, z);
                    
                    ((WorldCarverExtension) ((ConfiguredWorldCarverAccessor) carver).getWorldCarver()).setAquifer(this.createAquifer(chunk));
                    ((WorldCarverExtension) ((ConfiguredWorldCarverAccessor) carver).getWorldCarver()).setContext(context);
                    
                    if (carver.isStartChunk(random, x, z)) {
                        carver.carve(chunk, diffBiomeManager::getBiome, random, this.context.seaLevel(), x, z, chunkX, chunkZ, carvingMask);
                    }
                }
            }
        }
    }
    
    public int iterateNoiseColumn(double[][] slices, int x, int z, double noiseX, double noiseZ, @Nullable BlockState[] states, @Nullable Predicate<BlockState> predicate) {
        int chunkX = SectionPos.blockToSectionCoord(x);
        int chunkZ = SectionPos.blockToSectionCoord(z);
        
        Aquifer aquifer = this.getAquifer(this.context.minY(), this.context.chunkCountY(), new ChunkPos(chunkX, chunkZ));
        
        for(int chunkY = this.context.chunkCountY() - 1; chunkY >= 0; --chunkY) {
            double noise000 = slices[0][chunkY];
            double noise001 = slices[1][chunkY];
            double noise100 = slices[2][chunkY];
            double noise101 = slices[3][chunkY];
            double noise010 = slices[0][chunkY + 1];
            double noise011 = slices[1][chunkY + 1];
            double noise110 = slices[2][chunkY + 1];
            double noise111 = slices[3][chunkY + 1];
            
            for(int i = this.context.chunkHeight() - 1; i >= 0; --i) {
                double noiseY = (double)i / (double)this.context.chunkHeight();
                double density = Mth.lerp3(noiseY, noiseX, noiseZ, noise000, noise010, noise100, noise110, noise001, noise011, noise101, noise111);
                int height = chunkY * this.context.chunkHeight() + i;
                int y = height + this.context.minY() * this.context.chunkHeight();
                BlockState state = this.updateNoiseAndGenerateBaseState(Beardifier.NO_BEARDS, aquifer, NoiseModifier.PASSTHROUGH, x, y, z, density);
                
                if (states != null) {
                    states[height] = state;
                }
                
                if (predicate != null && predicate.test(state)) {
                    return y + 1;
                }
            }
        }
        
        return 0;
    }
    
    public void fillNoiseColumn(double[] slices, int x, int z) {
        this.sampler.fillNoiseColumn(slices, x, z, this.settings.noiseSettings(), this.context.seaLevel(), this.context.minY(), this.context.chunkCountY());
    }
    
    public void fillFromNoise(StructureFeatureManager featureManager, ChunkAccess chunk, int minY, int chunkCountY) {
        ProtoChunk proto = (ProtoChunk) chunk;
        Heightmap oceanFloor = proto.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = proto.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        ChunkPos pos = chunk.getPos();
        
        int minBlockX = pos.getMinBlockX();
        int minBlockZ = pos.getMinBlockZ();
        
        Beardifier beardifier = new Beardifier(featureManager, chunk);
        Aquifer aquifer = this.getAquifer(Offlimits.INSTANCE.getMinBuildHeight(), this.context.chunkCountY(), pos);
        NoiseInterpolator interpolator = new NoiseInterpolator(this.context.chunkCountX(), chunkCountY, this.context.chunkCountZ(), pos, this::fillNoiseColumn);
        List<NoiseInterpolator> interpolators = Lists.newArrayList(interpolator);
        
        Consumer<NoiseInterpolator> consumer = interpolators::add;
        DoubleFunction<NoiseModifier> caveNoiseModifier = this.createCaveNoiseModifier(pos, consumer);
        
        interpolators.forEach(NoiseInterpolator::initializeForFirstCellX);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        
        for (int noiseX = 0; noiseX < this.context.chunkCountX(); noiseX++) {
            int cellX = noiseX;
            
            interpolators.forEach(interpolator_ -> interpolator_.advanceCellX(cellX));
            
            for (int noiseZ = 0; noiseZ < this.context.chunkCountZ(); noiseZ++) {
                LevelChunkSection section = proto.getOrCreateSection(Offlimits.INSTANCE.getSectionsCount() - 1);
                
                for(int noiseY = chunkCountY - 1; noiseY >= 0; --noiseY) {
                    int cellY = noiseY;
                    int cellZ = noiseZ;
                    
                    interpolators.forEach(interpolator_ -> interpolator_.selectCellYZ(cellY, cellZ));
                    
                    for(int pieceY = this.context.chunkHeight() - 1; pieceY >= 0; pieceY--) {
                        int realY = (minY + noiseY) * this.context.chunkHeight() + pieceY;
                        int localY = realY & 15;
                        int sectionY = Offlimits.INSTANCE.getSectionIndex(realY);
                        
                        if (Offlimits.INSTANCE.getSectionIndex(section.bottomBlockY()) != sectionY) {
                            section = proto.getOrCreateSection(sectionY);
                        }
                        
                        double factorY = (double) pieceY / (double) this.context.chunkHeight();
                        
                        interpolators.forEach(interpolator_ -> interpolator_.updateForY(factorY));
                        
                        for(int pieceX = 0; pieceX < this.context.chunkWidth(); pieceX++) {
                            int realX = minBlockX + noiseX * this.context.chunkWidth() + pieceX;
                            int localX = realX & 15;
                            double factorX = (double) pieceX / (double) this.context.chunkWidth();
                            
                            interpolators.forEach(interpolator_ -> interpolator_.updateForX(factorX));
                            
                            for(int pieceZ = 0; pieceZ < this.context.chunkWidth(); pieceZ++) {
                                int realZ = minBlockZ + noiseZ * this.context.chunkWidth() + pieceZ;
                                int localZ = realZ & 15;
                                double factorZ = (double) pieceZ / (double) this.context.chunkWidth();
                                double density = interpolator.calculateValue(factorZ);
                                
                                BlockState state = this.updateNoiseAndGenerateBaseState(beardifier, aquifer, caveNoiseModifier.apply(factorZ), realX, realY, realZ, density);
                                
                                if (!state.isAir()) {
                                    if (state.getLightEmission() != 0 && chunk instanceof ProtoChunk) {
                                        mutable.set(realX, realY, realZ);
                                        proto.addLight(mutable);
                                    }
                                    
                                    section.setBlockState(localX, localY, localZ, state, false);
                                    oceanFloor.update(localX, realY, localZ, state);
                                    worldSurface.update(localX, realY, localZ, state);
                                    
                                    if (aquifer.shouldScheduleFluidUpdate() && !state.getFluidState().isEmpty()) {
                                        mutable.set(realX, realY, realZ);
                                        chunk.getLiquidTicks().scheduleTick(mutable, state.getFluidState().getType(), 0);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            interpolators.forEach(NoiseInterpolator::swapSlices);
        }
        
    }
    
    protected BlockState updateNoiseAndGenerateBaseState(Beardifier beardifier, Aquifer aquifer, NoiseModifier modifier, int x, int y, int z, double density) {
        double updatedDensity = Mth.clamp(density / 200.0, -1.0, 1.0);
        updatedDensity = updatedDensity / 2.0 - updatedDensity * updatedDensity * updatedDensity / 24.0;
        updatedDensity = modifier.modifyNoise(updatedDensity, x, y, z);
        updatedDensity += beardifier.beardifyOrBury(x, y, z);
        return aquifer.computeState(new SimpleStoneSource(this.context.defaultBlock()), x, y, z, updatedDensity);
    }
    
    private DoubleFunction<NoiseModifier> createCaveNoiseModifier(ChunkPos pos, Consumer<NoiseInterpolator> consumer) {
        if (Offlimits.CONFIG.areNoodleCavesEnabled.get()) {
            NoodleCaveNoiseModifier modifier = new NoodleCaveNoiseModifier(pos, this.context.chunkCountX(), this.context.chunkCountY(), this.context.chunkCountZ(), this.noodleCavifier);
            modifier.listInterpolators(consumer);
            return modifier::prepare;
        }
        
        return value -> NoiseModifier.PASSTHROUGH;
    }
    
    private Aquifer getAquifer(int minChunkY, int chunkCountY, ChunkPos pos) {
        if (Offlimits.CONFIG.areAquifersEnabled.get()) {
            return Aquifer.create(pos, this.barrierNoise, this.waterLevelNoise, this.lavaNoise, this.settings, this.sampler, minChunkY * this.context.chunkHeight(), chunkCountY * this.context.chunkHeight());
        }
        
        return Aquifer.createDisabled(this.context.seaLevel(), this.context.defaultFluid());
    }
    
    private Aquifer createAquifer(ChunkAccess chunk) {
        ChunkPos pos = chunk.getPos();
        int minY = Mth.intFloorDiv(this.context.minY(), this.context.chunkHeight());
        return this.getAquifer(minY, this.context.chunkCountY(), pos);
    }
}
package com.blackgear.offlimits.common.level.levelgen;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.common.level.*;
import com.blackgear.offlimits.common.level.levelgen.stonesource.SimpleStoneSource;
import com.blackgear.offlimits.common.level.noise.BlendedNoise;
import com.blackgear.offlimits.common.level.noise.NoiseInterpolator;
import com.blackgear.offlimits.common.level.noise.NoiseModifier;
import com.blackgear.offlimits.common.level.noise.NoiseSampler;
import com.blackgear.offlimits.common.level.noiseModifiers.NoodleCaveNoiseModifier;
import com.blackgear.offlimits.common.level.surface.BiomeExtension;
import com.blackgear.offlimits.common.level.surface.WorldCarverExtension;
import com.blackgear.offlimits.common.utils.NoiseUtils;
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
import net.minecraft.world.level.block.Blocks;
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

import java.util.BitSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class OfflimitsChunkGenerator {
    private final WorldgenRandom random;
    private final ChunkGenContext context;
    private final NoiseGeneratorSettings settings;
    
    private NormalNoise barrierNoise, waterLevelNoise, lavaNoise;
    private NormalNoise fluidLevelFloodedNoise, fluidLevelSpreadNoise;
    
    private NoiseSampler sampler;
    private NoodleCavifier noodleCavifier;
    
    public OfflimitsChunkGenerator(WorldgenRandom random, ChunkGenContext context, NoiseGeneratorSettings settings) {
        this.random = random;
        this.context = context;
        this.settings = settings;
    }
    
    public void initialize(BiomeSource biomeSource, long seed, SimplexNoise islandNoise, PerlinNoise depthNoise) {
        BlendedNoise blendedNoise = new BlendedNoise(this.random);
        this.barrierNoise = NoiseUtils.normal(new WorldgenRandom(this.random.nextLong()), -3, 1.0);
        this.waterLevelNoise = NoiseUtils.normal(new WorldgenRandom(this.random.nextLong()), -3, 1.0, 0.0, 2.0);
        this.fluidLevelFloodedNoise = NoiseUtils.normal(new WorldgenRandom(this.random.nextLong()), -7, 1.0);
        this.lavaNoise = NoiseUtils.normal(new WorldgenRandom(this.random.nextLong()), -1, 1.0);
        this.fluidLevelSpreadNoise = NoiseUtils.normal(new WorldgenRandom(this.random.nextLong()), -5, 1.0);
//        this.lavaNoise = NoiseUtils.normal(new SimpleRandom(this.random.nextLong()), -1, 1.0, 0.0);
        
        NoiseModifier caveNoiseModifier;
        if (Offlimits.CONFIG.areNoiseCavesEnabled.get()) {
            caveNoiseModifier = new Cavifier(this.random, this.context.minY() / this.context.chunkHeight());
        } else {
            caveNoiseModifier = NoiseModifier.PASSTHROUGH;
        }
        
        this.sampler = new NoiseSampler(biomeSource, this.context.chunkWidth(), this.context.chunkHeight(), this.context.chunkCountY(), this.settings.noiseSettings(), blendedNoise, islandNoise, depthNoise, caveNoiseModifier);
        this.noodleCavifier = new NoodleCavifier(seed);
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
    
    public void applyCarvers(long seed, BiomeManager biomeManager, BiomeSource biomeSource, ChunkAccess chunk, GenerationStep.Carving carving) {
        BiomeManager diffBiomeManager = biomeManager.withDifferentSource(biomeSource);
        ChunkPos chunkPos = chunk.getPos();
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;
        BiomeGenerationSettings generationSettings = biomeSource.getNoiseBiome(chunkPos.x << 2, 0, chunkPos.z << 2).getGenerationSettings();
        BitSet carvingMask = ((ProtoChunk) chunk).getOrCreateCarvingMask(carving);
        WorldgenRandom random = new WorldgenRandom();
        
        List<Supplier<ConfiguredWorldCarver<?>>> carvers = generationSettings.getCarvers(carving);
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
                    
                    if (carver.isStartChunk(random, x, z)) {
                        carver.carve(chunk, diffBiomeManager::getBiome, random, this.context.seaLevel(), x, z, chunkX, chunkZ, carvingMask);
                    }
                }
            }
        }
    }
    
    public OptionalInt iterateNoiseColumn(int x, int z, BlockState[] states, Predicate<BlockState> predicate, int minY, int chunkCountY) {
        int chunkX = SectionPos.blockToSectionCoord(x);
        int chunkZ = SectionPos.blockToSectionCoord(z);
        int o = Math.floorDiv(x, this.context.chunkWidth());
        int p = Math.floorDiv(z, this.context.chunkWidth());
        int q = Math.floorMod(x, this.context.chunkWidth());
        int r = Math.floorMod(z, this.context.chunkWidth());
        double noiseX = (double)q / (double)this.context.chunkWidth();
        double noiseZ = (double)r / (double)this.context.chunkWidth();
        double[][] slices = new double[][] {
            this.makeAndFillNoiseColumn(o, p, minY, chunkCountY),
            this.makeAndFillNoiseColumn(o, p + 1, minY, chunkCountY),
            this.makeAndFillNoiseColumn(o + 1, p, minY, chunkCountY),
            this.makeAndFillNoiseColumn(o + 1, p + 1, minY, chunkCountY)
        };
        
        Aquifer aquifer = this.getAquifer(minY, chunkCountY, new ChunkPos(chunkX, chunkZ));
        
        for(int chunkY = chunkCountY - 1; chunkY >= 0; --chunkY) {
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
                int y = height + minY * this.context.chunkHeight();
                BlockState state = this.updateNoiseAndGenerateBaseState(Beardifier.NO_BEARDS, aquifer, NoiseModifier.PASSTHROUGH, x, y, z, density);
                
                if (states != null) {
                    states[height] = state;
                }
                
                if (predicate != null && predicate.test(state)) {
                    return OptionalInt.of(y + 1);
                }
            }
        }
        
        return OptionalInt.empty();
    }
    
    public double[] makeAndFillNoiseColumn(int x, int z, int minY, int chunkCountY) {
        double[] slices = new double[chunkCountY + 1];
        this.fillNoiseColumn(slices, x, z, minY, chunkCountY);
        return slices;
    }
    
    public void fillNoiseColumn(double[] slices, int x, int z, int minY, int chunkCountY) {
        this.sampler.fillNoiseColumn(slices, x, z, this.settings.noiseSettings(), this.context.seaLevel(), minY, chunkCountY);
    }
    
    public void fillFromNoise(StructureFeatureManager structureFeatureManager, ChunkAccess chunkAccess, int i, int j) {
        ProtoChunk protoChunk = (ProtoChunk) chunkAccess;
        Heightmap heightmap = chunkAccess.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap heightmap2 = chunkAccess.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        ChunkPos chunkPos = chunkAccess.getPos();
        int k = chunkPos.getMinBlockX();
        int l = chunkPos.getMinBlockZ();
        Beardifier beardifier = new Beardifier(structureFeatureManager, chunkAccess);
        Aquifer aquifer = this.getAquifer(i, j, chunkPos);
        NoiseInterpolator noiseInterpolator = new NoiseInterpolator(this.context.chunkCountX(), j, this.context.chunkCountZ(), chunkPos, i, this::fillNoiseColumn);
        List<NoiseInterpolator> list = Lists.newArrayList(noiseInterpolator);
        Consumer<NoiseInterpolator> consumer = list::add;
        DoubleFunction<NoiseModifier> caveNoiseModifier = this.createCaveNoiseModifier(i, chunkPos, consumer);
        list.forEach(NoiseInterpolator::initializeForFirstCellX);
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        
        for (int m = 0; m < this.context.chunkCountX(); m++) {
            int n = m;
            list.forEach(interpolator_ -> interpolator_.advanceCellX(n));
            
            for (int o = 0; o < this.context.chunkCountZ(); o++) {
                LevelChunkSection section = protoChunk.getOrCreateSection(Offlimits.LEVEL.getSectionsCount() - 1);
                
                for(int p = j - 1; p >= 0; --p) {
                    int q = o;
                    int r = p;
                    list.forEach(interpolator_ -> interpolator_.selectCellYZ(r, q));
                    
                    for(int s = this.context.chunkHeight() - 1; s >= 0; s--) {
                        int t = (i + p) * this.context.chunkHeight() + s;
                        int u = t & 15;
                        int v = Offlimits.LEVEL.getSectionIndex(t);
                        if (Offlimits.LEVEL.getSectionIndex(section.bottomBlockY()) != v) {
                            section = protoChunk.getOrCreateSection(v);
                        }
                        
                        double d = (double) s / (double) this.context.chunkHeight();
                        list.forEach(interpolator_ -> interpolator_.updateForY(d));
                        
                        for(int w = 0; w < this.context.chunkWidth(); w++) {
                            int x = k + m * this.context.chunkWidth() + w;
                            int y = x & 15;
                            double e = (double) w / (double) this.context.chunkWidth();
                            list.forEach(interpolator_ -> interpolator_.updateForX(e));
                            
                            for(int z = 0; z < this.context.chunkWidth(); z++) {
                                int aa = l + o * this.context.chunkWidth() + z;
                                int ab = aa & 15;
                                double f = (double) z / (double) this.context.chunkWidth();
                                double g = noiseInterpolator.calculateValue(f);
                                BlockState state = this.updateNoiseAndGenerateBaseState(beardifier, aquifer, caveNoiseModifier.apply(f), x, t, aa, g);
                                
                                if (!state.isAir()) {
                                    if (state.getLightEmission() != 0 && chunkAccess instanceof ProtoChunk) {
                                        mutableBlockPos.set(x, t, aa);
                                        protoChunk.addLight(mutableBlockPos);
                                    }
                                    
                                    section.setBlockState(y, u, ab, state, false);
                                    heightmap.update(y, t, ab, state);
                                    heightmap2.update(y, t, ab, state);
                                    
                                    if (aquifer.shouldScheduleFluidUpdate() && !state.getFluidState().isEmpty()) {
                                        mutableBlockPos.set(x, t, aa);
                                        chunkAccess.getLiquidTicks().scheduleTick(mutableBlockPos, state.getFluidState().getType(), 0);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            list.forEach(NoiseInterpolator::swapSlices);
        }
    }
    
    private DoubleFunction<NoiseModifier> createCaveNoiseModifier(int minY, ChunkPos pos, Consumer<NoiseInterpolator> consumer) {
        if (Offlimits.CONFIG.areNoodleCavesEnabled.get()) {
            NoodleCaveNoiseModifier modifier = new NoodleCaveNoiseModifier(pos, this.context.chunkCountX(), this.context.chunkCountY(), this.context.chunkCountZ(), this.noodleCavifier, minY);
            modifier.listInterpolators(consumer);
            return modifier::prepare;
        }
        
        return value -> NoiseModifier.PASSTHROUGH;
    }
    
    protected BlockState updateNoiseAndGenerateBaseState(Beardifier beardifier, Aquifer aquifer, NoiseModifier modifier, int x, int y, int z, double density) {
        double updatedDensity = Mth.clamp(density / 200.0, -1.0, 1.0);
        updatedDensity = updatedDensity / 2.0 - updatedDensity * updatedDensity * updatedDensity / 24.0;
        updatedDensity = modifier.modifyNoise(updatedDensity, x, y, z);
        updatedDensity += beardifier.beardifyOrBury(x, y, z);
        return aquifer.computeState(new SimpleStoneSource(this.context.defaultBlock()), x, y, z, updatedDensity);
    }
    
    private Aquifer getAquifer(int minY, int height, ChunkPos pos) {
        if (Offlimits.CONFIG.areAquifersEnabled.get()) {
//            return Aquifer.create(
//                pos,
//                this.barrierNoise,
//                this.waterLevelNoise,
//                this.lavaNoise,
//                this.settings,
//                this.sampler,
//                minY * this.context.chunkHeight(),
//                height * this.context.chunkHeight()
//            );
            Aquifer.FluidStatus fluidStatus = new Aquifer.FluidStatus(this.context.minY() + 10, Blocks.LAVA.defaultBlockState());
            Aquifer.FluidStatus fluidStatus2 = new Aquifer.FluidStatus(this.context.seaLevel(), this.context.defaultFluid());

            return new NoiseBasedAquifer(
                this.sampler,
                pos,
                this.barrierNoise,
                this.fluidLevelFloodedNoise,
                this.fluidLevelSpreadNoise,
                this.lavaNoise,
                this.random,
                minY * this.context.chunkHeight(),
                height * this.context.chunkHeight(),
                (x, y, z) -> y < Math.min(this.context.minY() + 10, this.context.seaLevel()) ? fluidStatus : fluidStatus2
            );
        }
        
        return Aquifer.createDisabled(this.context.seaLevel(), this.context.defaultFluid());
    }
    
    private Aquifer createAquifer(ChunkAccess chunk) {
        ChunkPos pos = chunk.getPos();
        int minY = Mth.intFloorDiv(this.context.minY(), this.context.chunkHeight());
        return this.getAquifer(minY, this.context.chunkCountY(), pos);
    }
}
package com.blackgear.offlimits.common.level.levelgen.chunk;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.common.level.Aquifer;
import com.blackgear.offlimits.common.level.Beardifier;
import com.blackgear.offlimits.common.level.levelgen.noisemodifiers.NoodleCavifier;
import com.blackgear.offlimits.common.level.Cavifier;
import com.blackgear.offlimits.common.level.levelgen.TerrainContext;
import com.blackgear.offlimits.common.level.levelgen.stonesource.BaseStoneSource;
import com.blackgear.offlimits.common.level.levelgen.stonesource.SimpleStoneSource;
import com.blackgear.offlimits.common.level.noise.BlendedNoise;
import com.blackgear.offlimits.common.level.noise.BlockNoiseColumn;
import com.blackgear.offlimits.common.level.noise.NoiseInterpolator;
import com.blackgear.offlimits.common.level.levelgen.noisemodifiers.NoiseModifier;
import com.blackgear.offlimits.common.level.levelgen.noisemodifiers.NoodleCaveNoiseModifier;
import com.blackgear.offlimits.common.utils.HeightLimitAccess;
import com.blackgear.offlimits.common.utils.NoiseUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Predicate;

public class OfflimitsNoiseChunk extends NoiseChunk {
    private NormalNoise barrierNoise, waterLevelNoise, lavaNoise;
    private NoodleCavifier noodleCavifier;
    private BaseStoneSource stoneSource;
    
    public OfflimitsNoiseChunk(TerrainContext context, NoiseGeneratorSettings settings, WorldgenRandom random) {
        super(context, settings, random);
    }
    
    @Override
    public void initialize(BiomeSource source, long seed, @Nullable SimplexNoise islandNoise, PerlinNoise depthNoise) {
        BlendedNoise blendedNoise = new BlendedNoise(this.random);
        NoiseModifier caveNoiseModifier = this.context.areNoiseCavesEnabled()
            ? new Cavifier(this.random, this.context.minY() / this.context.chunkHeight())
            : NoiseModifier.PASSTHROUGH;
        
        this.barrierNoise = NoiseUtils.normal(this.random.nextLong(), -3, 1.0);
        this.waterLevelNoise = NoiseUtils.normal(this.random.nextLong(), -3, 0.2, 2.0, 1.0);
        this.lavaNoise = NoiseUtils.normal(this.random.nextLong(), -1, 1.0, 0.0);
        
        this.sampler = this.createNoiseSampler(source, blendedNoise, islandNoise, depthNoise, caveNoiseModifier);
        this.noodleCavifier = new NoodleCavifier(seed);
        this.stoneSource = new SimpleStoneSource(this.context.defaultBlock());
    }
    
    public OptionalInt iterateNoiseColumn(int x, int z, BlockState[] states, Predicate<BlockState> statePredicate, int minY, int chunkCountY) {
        int chunkX = SectionPos.blockToSectionCoord(x);
        int chunkZ = SectionPos.blockToSectionCoord(z);
        int xStart = Math.floorDiv(x, this.context.chunkWidth());
        int zStart = Math.floorDiv(z, this.context.chunkWidth());
        int xProgress = Math.floorMod(x, this.context.chunkWidth());
        int zProgress = Math.floorMod(z, this.context.chunkWidth());
        double lerpX = (double) xProgress / (double) this.context.chunkWidth();
        double lerpZ = (double) zProgress / (double) this.context.chunkWidth();
        double[][] buffer = new double[][] {
            this.makeAndFillNoiseColumn(xStart, zStart, minY, chunkCountY),
            this.makeAndFillNoiseColumn(xStart, zStart + 1, minY, chunkCountY),
            this.makeAndFillNoiseColumn(xStart + 1, zStart, minY, chunkCountY),
            this.makeAndFillNoiseColumn(xStart + 1, zStart + 1, minY, chunkCountY)
        };
        Aquifer aquifer = this.getAquifer(minY, chunkCountY, new ChunkPos(chunkX, chunkZ));
        
        for (int noiseY = chunkCountY - 1; noiseY >= 0; noiseY--) {
            double x0z0yo = buffer[0][noiseY];
            double x0z1y0 = buffer[1][noiseY];
            double x1z0y0 = buffer[2][noiseY];
            double x1z1y0 = buffer[3][noiseY];
            double x0z0y1 = buffer[0][noiseY + 1];
            double x0z1y1 = buffer[1][noiseY + 1];
            double x1z0y1 = buffer[2][noiseY + 1];
            double x1z1y1 = buffer[3][noiseY + 1];
            
            for (int pieceY = this.context.chunkHeight() - 1; pieceY >= 0; pieceY--) {
                double lerpY = (double) pieceY / (double) this.context.chunkHeight();
                double density = Mth.lerp3(lerpY, lerpX, lerpZ, x0z0yo, x0z0y1, x1z0y0, x1z0y1, x0z1y0, x0z1y1, x1z1y0, x1z1y1);
                
                int baseY = noiseY * this.context.chunkHeight() + pieceY;
                int y = baseY + minY * this.context.chunkHeight();
                
                BlockState state = this.updateNoiseAndGenerateBaseState(Beardifier.NO_BEARDS, aquifer, (x1, y1, z1) -> this.context.defaultBlock(), NoiseModifier.PASSTHROUGH, x, y, z, density);
                
                if (states != null) {
                    states[baseY] = state;
                }
                
                if (statePredicate != null && statePredicate.test(state)) {
                    return OptionalInt.of(y + 1);
                }
            }
        }
        
        return OptionalInt.empty();
    }
    
    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types types) {
        int genDepth = Math.max(this.context.minY(), this.context.minBuildHeight());
        int genHeight = Math.min(this.context.minY() + this.context.height(), this.context.maxBuildHeight());
        int minY = Mth.intFloorDiv(genDepth, this.context.chunkHeight());
        int chunkCountY = Mth.intFloorDiv(genHeight - genDepth, this.context.chunkHeight());
        
        return chunkCountY <= 0
            ? this.context.minBuildHeight()
            : this.iterateNoiseColumn(x, z, null, types.isOpaque(), minY, chunkCountY).orElse(this.context.minBuildHeight());
    }
    
    @Override
    public BlockGetter getBaseColumn(int x, int z) {
        int genDepth = Math.max(this.context.minY(), this.context.minBuildHeight());
        int genHeight = Math.min(this.context.minY() + this.context.height(), this.context.maxBuildHeight());
        int minY = Mth.intFloorDiv(genDepth, this.context.chunkHeight());
        int chunkCountY = Mth.intFloorDiv(genHeight - genDepth, this.context.chunkHeight());
        
        if (chunkCountY <= 0) {
            return new BlockNoiseColumn(genDepth, new BlockState[0]);
        } else {
            BlockState[] states = new BlockState[chunkCountY * this.context.chunkHeight()];
            this.iterateNoiseColumn(x, z, states, null, minY, chunkCountY);
            return new BlockNoiseColumn(genDepth, states);
        }
    }
    
    @Override
    public void fillFromNoise(LevelAccessor level, StructureFeatureManager structureManager, ChunkAccess chunk, int genDepth, int genHeight) {
        int minY = Mth.intFloorDiv(genDepth, this.context.chunkHeight());
        int chunkCountY = Mth.intFloorDiv(genHeight - genDepth, this.context.chunkHeight());
        
        if (chunkCountY > 0) {
            int maxSectionY = Offlimits.LEVEL.getSectionIndex(chunkCountY * this.context.chunkHeight() - 1 + genDepth);
            int minSectionY = Offlimits.LEVEL.getSectionIndex(genDepth);
            
            Set<LevelChunkSection> sections = Sets.newHashSet();
            
            try {
                for (int sectionY = maxSectionY; sectionY >= minSectionY; sectionY--) {
                    LevelChunkSection section = ((ProtoChunk) chunk).getOrCreateSection(sectionY);
                    section.acquire();
                    sections.add(section);
                }
                
                this.doFill(structureManager, chunk, minY, chunkCountY);
            } finally {
                for (LevelChunkSection section : sections) {
                    section.release();
                }
            }
        }
    }
    
    private void doFill(StructureFeatureManager structureManager, ChunkAccess chunk, int minY, int chunkCountY) {
        HeightLimitAccess level = Offlimits.LEVEL;
        TerrainContext context = this.context;
        
        ProtoChunk protoChunk = (ProtoChunk) chunk;
        
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        ChunkPos chunkPos = chunk.getPos();
        
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();
        
        Beardifier beardifier = new Beardifier(structureManager, chunk);
        Aquifer aquifer = this.getAquifer(minY, chunkCountY, chunkPos);
        
        NoiseInterpolator interpolator = new NoiseInterpolator(this.context.chunkCountX(), chunkCountY, this.context.chunkCountZ(), chunkPos, minY, this::fillNoiseColumn);
        List<NoiseInterpolator> interpolators = Lists.newArrayList(interpolator);
        
        DoubleFunction<BaseStoneSource> stoneSource = this.createBaseStoneSource(chunkCountY, chunkPos, interpolators::add);
        DoubleFunction<NoiseModifier> noiseModifier = this.createCaveNoiseModifier(chunkCountY, chunkPos, interpolators::add);
        
        interpolators.forEach(NoiseInterpolator::initializeForFirstCellX);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        
        for (int noiseX = 0; noiseX < context.chunkCountX(); noiseX++) {
            int finalX = noiseX;
            interpolators.forEach(lerp -> lerp.advanceCellX(finalX));
            
            for (int noiseZ = 0; noiseZ < context.chunkCountZ(); noiseZ++) {
                LevelChunkSection section = protoChunk.getOrCreateSection(level.getSectionsCount() - 1);
                
                for (int noiseY = chunkCountY - 1; noiseY >= 0; noiseY--) {
                    int finalZ = noiseZ;
                    int finalY = noiseY;
                    interpolators.forEach(lerp -> lerp.selectCellYZ(finalY, finalZ));
                    
                    for (int pieceY = context.chunkHeight() - 1; pieceY >= 0; pieceY--) {
                        int realY = (minY + noiseY) * context.chunkHeight() + pieceY;
                        int localY = realY & 15;
                        
                        int sectionY = level.getSectionIndex(realY);
                        if (level.getSectionIndex(section.bottomBlockY()) != sectionY) {
                            section = protoChunk.getOrCreateSection(sectionY);
                        }
                        
                        double factorY = (double) pieceY / (double) context.chunkHeight();
                        interpolators.forEach(lerp -> lerp.updateForY(factorY));
                        
                        for (int pieceX = 0; pieceX < context.chunkWidth(); pieceX++) {
                            int realX = startX + noiseX * context.chunkWidth() + pieceX;
                            int localX = realX & 15;
                            double factorX = (double) pieceX / (double) context.chunkWidth();
                            interpolators.forEach(lerp -> lerp.updateForX(factorX));
                            
                            for (int pieceZ = 0; pieceZ < context.chunkWidth(); pieceZ++) {
                                int realZ = startZ + noiseZ * context.chunkWidth() + pieceZ;
                                int localZ = realZ & 15;
                                double factorZ = (double) pieceZ / (double) context.chunkWidth();
                                
                                double density = interpolator.calculateValue(factorZ);
                                BlockState state = this.updateNoiseAndGenerateBaseState(beardifier, aquifer, stoneSource.apply(factorZ), noiseModifier.apply(factorZ), realX, realY, realZ, density);
                                
                                if (!state.isAir()) {
                                    if (state.getLightEmission() != 0 && chunk instanceof ProtoChunk) {
                                        mutable.set(realX, realY, realZ);
                                        protoChunk.addLight(mutable);
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
    
    private BlockState updateNoiseAndGenerateBaseState(Beardifier beardifier, Aquifer aquifer, BaseStoneSource stoneSource, NoiseModifier noiseModifier, int x, int y, int z, double baseDensity) {
        double density = Mth.clamp(baseDensity / 200.0, -1.0, 1.0);
        density = density / 2.0 - density * density * density / 24.0;
        density = noiseModifier.modifyNoise(density, x, y, z);
        density += beardifier.beardifyOrBury(x, y, z);
        return aquifer.computeState(stoneSource, x, y, z, density);
    }
    
    private Aquifer getAquifer(int minY, int chunkCountY, ChunkPos pos) {
        if (this.context.areAquifersEnabled()) {
            return Aquifer.create(
                pos,
                this.barrierNoise,
                this.waterLevelNoise,
                this.lavaNoise,
                this.settings,
                this.sampler,
                minY * this.context.chunkHeight(),
                chunkCountY * this.context.chunkHeight()
            );
        }
        
        return Aquifer.createDisabled(this.context.seaLevel(), this.context.defaultFluid());
    }
    
    @Override
    public Aquifer createAquifer(ChunkAccess chunk) {
        int minY = Mth.intFloorDiv(this.context.minY(), this.context.chunkHeight());
        return this.getAquifer(minY, this.context.chunkCountY(), chunk.getPos());
    }
    
    private DoubleFunction<BaseStoneSource> createBaseStoneSource(int minY, ChunkPos pos, Consumer<NoiseInterpolator> interpolator) {
        return value -> this.stoneSource;
    }
    
    private DoubleFunction<NoiseModifier> createCaveNoiseModifier(int minY, ChunkPos pos, Consumer<NoiseInterpolator> interpolator) {
        if (this.context.areNoodleCavesEnabled()) {
            NoodleCaveNoiseModifier noiseModifier = new NoodleCaveNoiseModifier(pos, this.context, this.noodleCavifier, minY);
            noiseModifier.listInterpolators(interpolator);
            return noiseModifier::prepare;
        }
        
        return value -> NoiseModifier.PASSTHROUGH;
    }
}
package com.blackgear.offlimits.core.mixin.common.level.chunk;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.common.level.levelgen.ChunkGenContext;
import com.blackgear.offlimits.common.level.levelgen.OfflimitsChunkGenerator;
import com.blackgear.offlimits.common.level.noise.BlockNoiseColumn;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.structures.JigsawJunction;
import net.minecraft.world.level.levelgen.feature.structures.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
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
    
    @Shadow protected abstract void setBedrock(ChunkAccess chunk, Random random);
    @Shadow public abstract int getSeaLevel();
    
    @Shadow @Final private SurfaceNoise surfaceNoise;
    
    @Shadow @Final private int height;
    
    @Shadow protected abstract void fillNoiseColumn(double[] ds, int i, int j);
    
    @Shadow private static double getContribution(int i, int j, int k) { throw new AssertionError(); }
    
    @Shadow protected abstract BlockState generateBaseState(double d, int i);
    
    @Shadow @Final private static BlockState AIR;
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
        if (!this.context.allowTerrainModifications()) {
//            if (Offlimits.INSTANCE.getMinBuildHeight() < 0) {
//                return;
//            }
            
            super.applyCarvers(seed, biomeManager, chunk, carving);
        }

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
        int k = Math.max(this.context.minY(), Offlimits.LEVEL.getMinBuildHeight());
        int l = Math.min(k + settings.height(), Offlimits.LEVEL.getMaxBuildHeight());
        int m = Mth.intFloorDiv(k, this.chunkHeight);
        int n = Mth.intFloorDiv(l - k, this.chunkHeight);
        cir.setReturnValue(
            n <= 0
                ? Offlimits.LEVEL.getMinBuildHeight()
                : this.generator.iterateNoiseColumn(i, j, null, types.isOpaque(), m, n).orElse(Offlimits.LEVEL.getMinBuildHeight())
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
        int k = Math.max(this.context.minY(), Offlimits.LEVEL.getMinBuildHeight());
        int l = Math.min(k + settings.height(), Offlimits.LEVEL.getMaxBuildHeight());
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
        int k = this.context.minY();
        int l = k - noiseGeneratorSettings.getBedrockFloorPosition();
        int m = this.height - 1 + k - noiseGeneratorSettings.getBedrockRoofPosition();
        int n = 5;
        int o = Offlimits.LEVEL.getMinBuildHeight();
        int p = Offlimits.LEVEL.getMaxBuildHeight();
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
        NoiseSettings settings = this.settings.get().noiseSettings();
        int i = Math.max(this.context.minY(), Offlimits.LEVEL.getMinBuildHeight());
        int j = Math.min(this.context.minY() + settings.height(), chunk.getMaxBuildHeight());
        int k = Mth.intFloorDiv(i, this.chunkHeight);
        int l = Mth.intFloorDiv(j - i, this.chunkHeight);
        
        
        if (!this.context.allowsTerrainModifications(level)) {
//            if (Offlimits.CONFIG.minBuildHeight.get() < 0) {
                this.vanilla$fillFromNoise(level, featureManager, chunk, k, l);
                ci.cancel();
//            }
//
//            return;
        }

        if (l <= 0) {
            ci.cancel();
        }

        int m = Offlimits.LEVEL.getSectionIndex(l * this.chunkHeight - 1 + i);
        int n = Offlimits.LEVEL.getSectionIndex(i);

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
    
    @Unique
    public void vanilla$fillFromNoise(LevelAccessor levelAccessor, StructureFeatureManager structureFeatureManager, ChunkAccess chunkAccess, int minY, int height) {
        ObjectList<StructurePiece> objectList = new ObjectArrayList<>(10);
        ObjectList<JigsawJunction> objectList2 = new ObjectArrayList<>(32);
        ChunkPos chunkPos = chunkAccess.getPos();
        int i = chunkPos.x;
        int j = chunkPos.z;
        int k = i << 4;
        int l = j << 4;
        
        for (StructureFeature<?> feature : StructureFeature.NOISE_AFFECTING_FEATURES) {
            structureFeatureManager.startsForFeature(SectionPos.of(chunkPos, Offlimits.CONFIG.worldGenMinY.get()), feature).forEach((structureStart) -> {
                Iterator<StructurePiece> var6 = structureStart.getPieces().iterator();
                
                while (true) {
                    while (true) {
                        StructurePiece structurePiece;
                        do {
                            if (!var6.hasNext()) {
                                return;
                            }
                            
                            structurePiece = var6.next();
                        } while (!structurePiece.isCloseToChunk(chunkPos, 12));
                        
                        if (structurePiece instanceof PoolElementStructurePiece) {
                            PoolElementStructurePiece poolElementStructurePiece = (PoolElementStructurePiece) structurePiece;
                            StructureTemplatePool.Projection projection = poolElementStructurePiece.getElement().getProjection();
                            if (projection == StructureTemplatePool.Projection.RIGID) {
                                objectList.add(poolElementStructurePiece);
                            }
                            
                            for (JigsawJunction jigsawJunction : poolElementStructurePiece.getJunctions()) {
                                int kx = jigsawJunction.getSourceX();
                                int lx = jigsawJunction.getSourceZ();
                                if (kx > k - 12 && lx > l - 12 && kx < k + 15 + 12 && lx < l + 15 + 12) {
                                    objectList2.add(jigsawJunction);
                                }
                            }
                        } else {
                            objectList.add(structurePiece);
                        }
                    }
                }
            });
        }
        
        double[][][] ds = new double[2][this.chunkCountZ + 1][this.chunkCountY + 1];
        
        for(int m = 0; m < this.chunkCountZ + 1; ++m) {
            ds[0][m] = new double[this.chunkCountY + 1];
            this.fillNoiseColumn(ds[0][m], i * this.chunkCountX, j * this.chunkCountZ + m);
            ds[1][m] = new double[this.chunkCountY + 1];
        }
        
        ProtoChunk protoChunk = (ProtoChunk)chunkAccess;
        Heightmap heightmap = protoChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap heightmap2 = protoChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        ObjectListIterator<StructurePiece> objectListIterator = objectList.iterator();
        ObjectListIterator<JigsawJunction> objectListIterator2 = objectList2.iterator();
        
        for(int n = 0; n < this.chunkCountX; ++n) {
            int o;
            for(o = 0; o < this.chunkCountZ + 1; ++o) {
                this.fillNoiseColumn(ds[1][o], i * this.chunkCountX + n + 1, j * this.chunkCountZ + o);
            }
            
            for(o = 0; o < this.chunkCountZ; ++o) {
                LevelChunkSection levelChunkSection = protoChunk.getOrCreateSection(Offlimits.LEVEL.getSectionsCount() - 1);
                levelChunkSection.acquire();
                
                for(int p = height - 1; p >= 0; --p) {
                    double d = ds[0][o][p];
                    double e = ds[0][o + 1][p];
                    double f = ds[1][o][p];
                    double g = ds[1][o + 1][p];
                    double h = ds[0][o][p + 1];
                    double q = ds[0][o + 1][p + 1];
                    double r = ds[1][o][p + 1];
                    double s = ds[1][o + 1][p + 1];
                    
                    for(int t = this.chunkHeight - 1; t >= 0; --t) {
                        int u = (minY + p) * this.chunkHeight + t;
                        int v = u & 15;
                        int w = Offlimits.LEVEL.getSectionIndex(u);
                        if (Offlimits.LEVEL.getSectionIndex(levelChunkSection.bottomBlockY()) != w) {
                            levelChunkSection.release();
                            levelChunkSection = protoChunk.getOrCreateSection(w);
                            levelChunkSection.acquire();
                        }
                        
                        double x = (double)t / (double)this.chunkHeight;
                        double y = Mth.lerp(x, d, h);
                        double z = Mth.lerp(x, f, r);
                        double aa = Mth.lerp(x, e, q);
                        double ab = Mth.lerp(x, g, s);
                        
                        for(int ac = 0; ac < this.chunkWidth; ++ac) {
                            int ad = k + n * this.chunkWidth + ac;
                            int ae = ad & 15;
                            double af = (double)ac / (double)this.chunkWidth;
                            double ag = Mth.lerp(af, y, z);
                            double ah = Mth.lerp(af, aa, ab);
                            
                            for(int ai = 0; ai < this.chunkWidth; ++ai) {
                                int aj = l + o * this.chunkWidth + ai;
                                int ak = aj & 15;
                                double al = (double)ai / (double)this.chunkWidth;
                                double am = Mth.lerp(al, ag, ah);
                                double an = Mth.clamp(am / 200.0, -1.0, 1.0);
                                
                                int ao;
                                int ap;
                                int aq;
                                for(an = an / 2.0 - an * an * an / 24.0; objectListIterator.hasNext(); an += getContribution(ao, ap, aq) * 0.8) {
                                    StructurePiece structurePiece = objectListIterator.next();
                                    BoundingBox boundingBox = structurePiece.getBoundingBox();
                                    ao = Math.max(0, Math.max(boundingBox.x0 - ad, ad - boundingBox.x1));
                                    ap = u - (boundingBox.y0 + (structurePiece instanceof PoolElementStructurePiece ? ((PoolElementStructurePiece)structurePiece).getGroundLevelDelta() : 0));
                                    aq = Math.max(0, Math.max(boundingBox.z0 - aj, aj - boundingBox.z1));
                                }
                                
                                objectListIterator.back(objectList.size());
                                
                                while(objectListIterator2.hasNext()) {
                                    JigsawJunction jigsawJunction = objectListIterator2.next();
                                    int ar = ad - jigsawJunction.getSourceX();
                                    ao = u - jigsawJunction.getSourceGroundY();
                                    ap = aj - jigsawJunction.getSourceZ();
                                    an += getContribution(ar, ao, ap) * 0.4;
                                }
                                
                                objectListIterator2.back(objectList2.size());
                                BlockState blockState = this.generateBaseState(an, u);
                                if (blockState != AIR) {
                                    if (blockState.getLightEmission() != 0) {
                                        mutableBlockPos.set(ad, u, aj);
                                        protoChunk.addLight(mutableBlockPos);
                                    }
                                    
                                    levelChunkSection.setBlockState(ae, v, ak, blockState, false);
                                    heightmap.update(ae, u, ak, blockState);
                                    heightmap2.update(ae, u, ak, blockState);
                                }
                            }
                        }
                    }
                }
                
                levelChunkSection.release();
            }
            
            double[][] es = ds[0];
            ds[0] = ds[1];
            ds[1] = es;
        }
    }
}
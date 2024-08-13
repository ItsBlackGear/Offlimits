package com.blackgear.offlimits.common.level;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.Util;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.structures.JigsawJunction;
import net.minecraft.world.level.levelgen.feature.structures.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

public class Beardifier {
    public static final Beardifier NO_BEARDS = new Beardifier();
    public static final int BEARD_KERNEL_RADIUS = 12;
    private static final int BEARD_KERNEL_SIZE = 24;
    private static final float[] BEARD_KERNEL = Util.make(new float[13824], kernels -> {
        for(int x = 0; x < BEARD_KERNEL_SIZE; ++x) {
            for(int y = 0; y < BEARD_KERNEL_SIZE; ++y) {
                for(int z = 0; z < BEARD_KERNEL_SIZE; ++z) {
                    kernels[x * BEARD_KERNEL_SIZE * BEARD_KERNEL_SIZE + y * BEARD_KERNEL_SIZE + z] = (float)computeBeardContribution(y - BEARD_KERNEL_RADIUS, z - BEARD_KERNEL_RADIUS, x - BEARD_KERNEL_RADIUS);
                }
            }
        }
    });
    
    private final ObjectList<StructurePiece> rigids;
    private final ObjectList<JigsawJunction> junctions;
    private final ObjectListIterator<StructurePiece> pieceIterator;
    private final ObjectListIterator<JigsawJunction> junctionIterator;
    
    public Beardifier(StructureFeatureManager featureManager, ChunkAccess chunk) {
        ChunkPos pos = chunk.getPos();
        int minChunkX = pos.getMinBlockX();
        int minChunkZ = pos.getMinBlockZ();
        
        this.junctions = new ObjectArrayList<>(32);
        this.rigids = new ObjectArrayList<>(10);
        
        for(StructureFeature<?> structureFeature : StructureFeature.NOISE_AFFECTING_FEATURES) {
            featureManager.startsForFeature(SectionPos.of(pos, 0), structureFeature).forEach(structureStart -> {
                for(StructurePiece structurePiece : structureStart.getPieces()) {
                    if (structurePiece.isCloseToChunk(pos, 12)) {
                        if (structurePiece instanceof PoolElementStructurePiece) {
                            PoolElementStructurePiece poolElementStructurePiece = (PoolElementStructurePiece)structurePiece;
                            StructureTemplatePool.Projection projection = poolElementStructurePiece.getElement().getProjection();
                            if (projection == StructureTemplatePool.Projection.RIGID) {
                                this.rigids.add(poolElementStructurePiece);
                            }
                            
                            for(JigsawJunction jigsawJunction : poolElementStructurePiece.getJunctions()) {
                                int k = jigsawJunction.getSourceX();
                                int l = jigsawJunction.getSourceZ();
                                if (k > minChunkX - 12 && l > minChunkZ - 12 && k < minChunkX + 15 + 12 && l < minChunkZ + 15 + 12) {
                                    this.junctions.add(jigsawJunction);
                                }
                            }
                        } else {
                            this.rigids.add(structurePiece);
                        }
                    }
                }
            });
        }
        
        this.pieceIterator = this.rigids.iterator();
        this.junctionIterator = this.junctions.iterator();
    }
    
    private Beardifier() {
        this.junctions = new ObjectArrayList<>();
        this.rigids = new ObjectArrayList<>();
        this.pieceIterator = this.rigids.iterator();
        this.junctionIterator = this.junctions.iterator();
    }
    
    public double beardifyOrBury(int x, int y, int z) {
        double contribution = 0.0;
        
        while (this.pieceIterator.hasNext()) {
            StructurePiece piece = this.pieceIterator.next();
            BoundingBox box = piece.getBoundingBox();
            
            int pieceX = Math.max(0, Math.max(box.x0 - x, x - box.x1));
            int pieceY = y - (box.y0 + (piece instanceof PoolElementStructurePiece ? ((PoolElementStructurePiece) piece).getGroundLevelDelta() : 0));
            int pieceZ = Math.max(0, Math.max(box.z0 - z, z - box.z1));
            
            contribution += getBeardContribution(pieceX, pieceY, pieceZ) * 0.8;
        }
        
        this.pieceIterator.back(this.rigids.size());
        
        while (this.junctionIterator.hasNext()) {
            JigsawJunction function = this.junctionIterator.next();
            
            int junctionX = x - function.getSourceX();
            int junctionY = y - function.getSourceGroundY();
            int junctionZ = z - function.getSourceZ();
            
            contribution += getBeardContribution(junctionX, junctionY, junctionZ) * 0.4;
        }
        
        this.junctionIterator.back(this.junctions.size());
        return contribution;
    }
    
    private static double getBeardContribution(int x, int y, int z) {
        int kernelX = x + BEARD_KERNEL_RADIUS;
        int kernelY = y + BEARD_KERNEL_RADIUS;
        int kernelZ = z + BEARD_KERNEL_RADIUS;
        
        if (kernelX < 0 || kernelX >= BEARD_KERNEL_SIZE) {
            return 0.0;
        } else if (kernelY < 0 || kernelY >= BEARD_KERNEL_SIZE) {
            return 0.0;
        } else {
            return kernelZ >= 0 && kernelZ < BEARD_KERNEL_SIZE
                ? (double)BEARD_KERNEL[kernelZ * BEARD_KERNEL_SIZE * BEARD_KERNEL_SIZE + kernelX * BEARD_KERNEL_SIZE + kernelY]
                : 0.0;
        }
    }
    
    private static double computeBeardContribution(int x, int y, int z) {
        double xzRadius = x * x + z * z;
        double yOffset = (double) y + 0.5;
        double yRadius = yOffset * yOffset;
        double factor = Math.pow(Math.E, -(yRadius / 16.0 + xzRadius / 16.0));
        double contribution = -yOffset * Mth.fastInvSqrt(yRadius / 2.0 + xzRadius / 2.0) / 2.0;
        return contribution * factor;
    }
}
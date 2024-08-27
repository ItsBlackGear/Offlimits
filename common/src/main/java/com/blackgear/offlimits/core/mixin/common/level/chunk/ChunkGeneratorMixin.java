package com.blackgear.offlimits.core.mixin.common.level.chunk;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.CrashReportDetail;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.StructureSettings;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.StructureFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin {
    @Shadow @Final private StructureSettings settings;
    
    @Shadow @Final protected BiomeSource biomeSource;
    
    @Inject(
        method = "applyBiomeDecoration",
        at = @At("HEAD"),
        cancellable = true
    )
    public void off$applyBiomeDecoration(WorldGenRegion region, StructureFeatureManager structureManager, CallbackInfo ci) {
        int i = region.getCenterX();
        int j = region.getCenterZ();
        int k = i * 16;
        int l = j * 16;
        BlockPos blockPos = new BlockPos(k, Offlimits.INSTANCE.getMinSection(), l);
        Biome biome = this.biomeSource.getNoiseBiome((i << 2) + 2, 2, (j << 2) + 2);
        WorldgenRandom worldgenRandom = new WorldgenRandom();
        long m = worldgenRandom.setDecorationSeed(region.getSeed(), k, l);
        
        try {
            biome.generate(structureManager, ChunkGenerator.class.cast(this), region, m, worldgenRandom, blockPos);
        } catch (Exception var14) {
            CrashReport crashReport = CrashReport.forThrowable(var14, "Biome decoration");
            crashReport.addCategory("Generation").setDetail("CenterX", i).setDetail("CenterZ", j).setDetail("Seed", m).setDetail("Biome", biome);
            throw new ReportedException(crashReport);
        }
        
        ci.cancel();
    }
    
    @Inject(
        method = "createStructure",
        at = @At("HEAD"),
        cancellable = true
    )
    private void off$createStructure(
        ConfiguredStructureFeature<?, ?> config,
        RegistryAccess registry,
        StructureFeatureManager featureManager,
        ChunkAccess chunk,
        StructureManager structureManager,
        long seed,
        ChunkPos chunkPos,
        Biome biome,
        CallbackInfo ci
    ) {
        StructureStart<?> structureStart = featureManager.getStartForFeature(
            SectionPos.of(chunk.getPos(), Offlimits.INSTANCE.getMinSection()),
            config.feature,
            chunk
        );
        int i = structureStart != null ? structureStart.getReferences() : 0;
        StructureFeatureConfiguration structureFeatureConfiguration = this.settings.getConfig(config.feature);
        if (structureFeatureConfiguration != null) {
            StructureStart<?> structureStart2 = config.generate(
                registry, (ChunkGenerator)(Object)this, this.biomeSource, structureManager, seed, chunkPos, biome, i, structureFeatureConfiguration
            );
            featureManager.setStartForFeature(SectionPos.of(chunk.getPos(), 0), config.feature, structureStart2, chunk);
        }
        
        ci.cancel();
    }
    
    @Inject(
        method = "createReferences",
        at = @At("HEAD"),
        cancellable = true
    )
    public void off$createReferences(
        WorldGenLevel level,
        StructureFeatureManager structureManager,
        ChunkAccess chunk,
        CallbackInfo ci
    ) {
        int j = chunk.getPos().x;
        int k = chunk.getPos().z;
        int l = j << 4;
        int m = k << 4;
        SectionPos sectionPos = SectionPos.of(chunk.getPos(), Offlimits.INSTANCE.getMinSection());
        
        for(int n = j - 8; n <= j + 8; ++n) {
            for(int o = k - 8; o <= k + 8; ++o) {
                long p = ChunkPos.asLong(n, o);
                
                for(StructureStart<?> structureStart : level.getChunk(n, o).getAllStarts().values()) {
                    try {
                        if (structureStart != StructureStart.INVALID_START && structureStart.getBoundingBox().intersects(l, m, l + 15, m + 15)) {
                            structureManager.addReferenceForFeature(sectionPos, structureStart.getFeature(), p, chunk);
                            DebugPackets.sendStructurePacket(level, structureStart);
                        }
                    } catch (Exception var19) {
                        CrashReport crashReport = CrashReport.forThrowable(var19, "Generating structure reference");
                        CrashReportCategory crashReportCategory = crashReport.addCategory("Structure");
                        crashReportCategory.setDetail("Id", () -> Registry.STRUCTURE_FEATURE.getKey(structureStart.getFeature()).toString());
                        crashReportCategory.setDetail("Name", () -> structureStart.getFeature().getFeatureName());
                        crashReportCategory.setDetail("Class", () -> structureStart.getFeature().getClass().getCanonicalName());
                        throw new ReportedException(crashReport);
                    }
                }
            }
        }
        
        ci.cancel();
    }
}
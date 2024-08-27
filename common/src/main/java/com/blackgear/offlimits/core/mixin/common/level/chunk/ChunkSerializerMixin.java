package com.blackgear.offlimits.core.mixin.common.level.chunk;

import com.blackgear.offlimits.Offlimits;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ChunkTickList;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(ChunkSerializer.class)
public abstract class ChunkSerializerMixin {
    @Shadow @Final private static Logger LOGGER;
    
    @Shadow public static ChunkStatus.ChunkType getChunkTypeFromTag(@Nullable CompoundTag chunkNBT) { return null; }
    @Shadow private static Map<StructureFeature<?>, StructureStart<?>> unpackStructureStart(StructureManager structureManager, CompoundTag compoundTag, long l) { return null; }
    @Shadow private static Map<StructureFeature<?>, LongSet> unpackStructureReferences(ChunkPos pos, CompoundTag tag) { return null; }
    @Shadow private static void postLoadChunk(CompoundTag compoundTag, LevelChunk levelChunk) {}
    @Shadow public static ListTag packOffsets(ShortList[] list) { return null; }
    @Shadow private static CompoundTag packStructureData(ChunkPos chunkPos, Map<StructureFeature<?>, StructureStart<?>> map, Map<StructureFeature<?>, LongSet> map2) { return null; }
    
    @Inject(
        method = "read",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void read(ServerLevel level, StructureManager templateManager, PoiManager poiManager, ChunkPos pos, CompoundTag compound, CallbackInfoReturnable<ProtoChunk> cir) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        BiomeSource biomeSource = generator.getBiomeSource();
        CompoundTag levelTag = compound.getCompound("Level");
        ChunkPos chunkPos = new ChunkPos(levelTag.getInt("xPos"), levelTag.getInt("zPos"));
        
        if (!Objects.equals(pos, chunkPos)) {
            LOGGER.error("Chunk file at {} is in the wrong location; relocating. (Expected {}, got {})", pos, pos, chunkPos);
        }
        
        ChunkBiomeContainer biomeContainer = new ChunkBiomeContainer(level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), pos, biomeSource, levelTag.contains("Biomes", 11) ? levelTag.getIntArray("Biomes") : null);
        UpgradeData upgradeData = levelTag.contains("UpgradeData", 10) ? new UpgradeData(levelTag.getCompound("UpgradeData")) : UpgradeData.EMPTY;
        ProtoTickList<Block> blocksToBeTicked = new ProtoTickList<>(block -> block == null || block.defaultBlockState().isAir(), pos, levelTag.getList("ToBeTicked", 9));
        ProtoTickList<Fluid> liquidsToBeTicked = new ProtoTickList<>(fluid -> fluid == null || fluid == Fluids.EMPTY, pos, levelTag.getList("LiquidsToBeTicked", 9));
        boolean isLightOn = levelTag.getBoolean("isLightOn");
        ListTag sections = levelTag.getList("Sections", 10);
        int j = Offlimits.INSTANCE.getSectionsCount();
        LevelChunkSection[] section = new LevelChunkSection[j];
        boolean hasSkyLight = level.dimensionType().hasSkyLight();
        ChunkSource chunkSource = level.getChunkSource();
        LevelLightEngine lightEngine = chunkSource.getLightEngine();
        
        if (isLightOn) {
            lightEngine.retainData(pos, true);
        }
        
        for(int index = 0; index < sections.size(); ++index) {
            CompoundTag sectionTag = sections.getCompound(index);
            int y = sectionTag.getByte("Y");
            if (sectionTag.contains("Palette", 9) && sectionTag.contains("BlockStates", 12)) {
                LevelChunkSection levelChunkSection = new LevelChunkSection(y << 4);
                levelChunkSection.getStates().read(sectionTag.getList("Palette", 10), sectionTag.getLongArray("BlockStates"));
                levelChunkSection.recalcBlockCounts();
                if (!levelChunkSection.isEmpty()) {
                    section[Offlimits.INSTANCE.getSectionIndexFromSectionY(y)] = levelChunkSection;
                }
                
                poiManager.checkConsistencyWithBlocks(pos, levelChunkSection);
            }
            
            if (isLightOn) {
                if (sectionTag.contains("BlockLight", 7)) {
                    lightEngine.queueSectionData(LightLayer.BLOCK, SectionPos.of(pos, y), new DataLayer(sectionTag.getByteArray("BlockLight")), true);
                }
                
                if (hasSkyLight && sectionTag.contains("SkyLight", 7)) {
                    lightEngine.queueSectionData(LightLayer.SKY, SectionPos.of(pos, y), new DataLayer(sectionTag.getByteArray("SkyLight")), true);
                }
            }
        }
        
        long inhabitedTime = levelTag.getLong("InhabitedTime");
        ChunkStatus.ChunkType chunkType = getChunkTypeFromTag(compound);
        
        ChunkAccess chunk;
        if (chunkType == ChunkStatus.ChunkType.LEVELCHUNK) {
            TickList<Block> tileTicks;
            if (levelTag.contains("TileTicks", 9)) {
                tileTicks = ChunkTickList.create(levelTag.getList("TileTicks", 10), Registry.BLOCK::getKey, Registry.BLOCK::get);
            } else {
                tileTicks = blocksToBeTicked;
            }
            
            TickList<Fluid> liquidTicks;
            if (levelTag.contains("LiquidTicks", 9)) {
                liquidTicks = ChunkTickList.create(levelTag.getList("LiquidTicks", 10), Registry.FLUID::getKey, Registry.FLUID::get);
            } else {
                liquidTicks = liquidsToBeTicked;
            }
            
            chunk = new LevelChunk(level.getLevel(), pos, biomeContainer, upgradeData, tileTicks, liquidTicks, inhabitedTime, section, levelChunk -> postLoadChunk(levelTag, levelChunk));
        } else {
            ProtoChunk protoChunk = new ProtoChunk(pos, upgradeData, section, blocksToBeTicked, liquidsToBeTicked);
            protoChunk.setBiomes(biomeContainer);
            chunk = protoChunk;
            protoChunk.setInhabitedTime(inhabitedTime);
            protoChunk.setStatus(ChunkStatus.byName(levelTag.getString("Status")));
            if (protoChunk.getStatus().isOrAfter(ChunkStatus.FEATURES)) {
                protoChunk.setLightEngine(lightEngine);
            }
            
            if (!isLightOn && protoChunk.getStatus().isOrAfter(ChunkStatus.LIGHT)) {
                for(BlockPos blockPos : BlockPos.betweenClosed(
                    pos.getMinBlockX(),
                    Offlimits.INSTANCE.getMinBuildHeight(),
                    pos.getMinBlockZ(),
                    pos.getMaxBlockX(),
                    Offlimits.INSTANCE.getMaxBuildHeight() - 1,
                    pos.getMaxBlockZ())
                ) {
                    if (chunk.getBlockState(blockPos).getLightEmission() != 0) {
                        protoChunk.addLight(blockPos);
                    }
                }
            }
        }
        
        chunk.setLightCorrect(isLightOn);
        CompoundTag heightmaps = levelTag.getCompound("Heightmaps");
        EnumSet<Heightmap.Types> heightmapTypes = EnumSet.noneOf(Heightmap.Types.class);
        
        for(Heightmap.Types types : chunk.getStatus().heightmapsAfter()) {
            String serializationKey = types.getSerializationKey();
            if (heightmaps.contains(serializationKey, 12)) {
                chunk.setHeightmap(types, heightmaps.getLongArray(serializationKey));
            } else {
                heightmapTypes.add(types);
            }
        }
        
        Heightmap.primeHeightmaps(chunk, heightmapTypes);
        CompoundTag structures = levelTag.getCompound("Structures");
        chunk.setAllStarts(unpackStructureStart(templateManager, structures, level.getSeed()));
        chunk.setAllReferences(unpackStructureReferences(pos, structures));
        if (levelTag.getBoolean("shouldSave")) {
            chunk.setUnsaved(true);
        }
        
        ListTag postProcessing = levelTag.getList("PostProcessing", 9);
        
        for(int i = 0; i < postProcessing.size(); ++i) {
            ListTag listTag3 = postProcessing.getList(i);
            
            for(int n = 0; n < listTag3.size(); ++n) {
                chunk.addPackedPostProcess(listTag3.getShort(n), i);
            }
        }
        
        if (chunkType == ChunkStatus.ChunkType.LEVELCHUNK) {
            cir.setReturnValue(new ImposterProtoChunk((LevelChunk) chunk));
        } else {
            ProtoChunk protoChunk2 = (ProtoChunk)chunk;
            ListTag listTag3 = levelTag.getList("Entities", 10);
            
            for(int n = 0; n < listTag3.size(); ++n) {
                protoChunk2.addEntity(listTag3.getCompound(n));
            }
            
            ListTag listTag4 = levelTag.getList("TileEntities", 10);
            
            for(int o = 0; o < listTag4.size(); ++o) {
                CompoundTag compoundTag5 = listTag4.getCompound(o);
                chunk.setBlockEntityNbt(compoundTag5);
            }
            
            ListTag listTag5 = levelTag.getList("Lights", 9);
            
            for(int p = 0; p < listTag5.size(); ++p) {
                ListTag listTag6 = listTag5.getList(p);
                
                for(int q = 0; q < listTag6.size(); ++q) {
                    protoChunk2.addLight(listTag6.getShort(q), p);
                }
            }
            
            CompoundTag compoundTag5 = levelTag.getCompound("CarvingMasks");
            
            for(String string2 : compoundTag5.getAllKeys()) {
                GenerationStep.Carving carving = GenerationStep.Carving.valueOf(string2);
                protoChunk2.setCarvingMask(carving, BitSet.valueOf(compoundTag5.getByteArray(string2)));
            }
            
            cir.setReturnValue(protoChunk2);
        }
    }
    
    @Inject(
        method = "write",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void write(ServerLevel level, ChunkAccess chunk, CallbackInfoReturnable<CompoundTag> cir) {
        ChunkPos chunkPos = chunk.getPos();
        CompoundTag compoundTag = new CompoundTag();
        CompoundTag compoundTag2 = new CompoundTag();
        compoundTag.putInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());
        compoundTag.put("Level", compoundTag2);
        compoundTag2.putInt("xPos", chunkPos.x);
        compoundTag2.putInt("zPos", chunkPos.z);
        compoundTag2.putLong("LastUpdate", level.getGameTime());
        compoundTag2.putLong("InhabitedTime", chunk.getInhabitedTime());
        compoundTag2.putString("Status", chunk.getStatus().getName());
        UpgradeData upgradeData = chunk.getUpgradeData();
        if (!upgradeData.isEmpty()) {
            compoundTag2.put("UpgradeData", upgradeData.write());
        }
        
        LevelChunkSection[] levelChunkSections = chunk.getSections();
        ListTag listTag = new ListTag();
        LevelLightEngine levelLightEngine = level.getChunkSource().getLightEngine();
        boolean isLightCorrect = chunk.isLightCorrect();
        
        for(int i = Offlimits.LIGHT.getMinLightSection(); i < Offlimits.LIGHT.getMaxLightSection(); ++i) {
            int index = i;
            LevelChunkSection levelChunkSection = Arrays.stream(levelChunkSections).filter(section -> {
                return section != null && SectionPos.blockToSectionCoord(section.bottomBlockY()) == index;
            }).findFirst().orElse(LevelChunk.EMPTY_SECTION);
            DataLayer dataLayer = levelLightEngine.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(chunkPos, index));
            DataLayer dataLayer2 = levelLightEngine.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(chunkPos, index));
            
            if (levelChunkSection != LevelChunk.EMPTY_SECTION || dataLayer != null || dataLayer2 != null) {
                CompoundTag compoundTag3 = new CompoundTag();
                compoundTag3.putByte("Y", (byte)(index & 255));
                if (levelChunkSection != LevelChunk.EMPTY_SECTION) {
                    levelChunkSection.getStates().write(compoundTag3, "Palette", "BlockStates");
                }
                
                if (dataLayer != null && !dataLayer.isEmpty()) {
                    compoundTag3.putByteArray("BlockLight", dataLayer.getData());
                }
                
                if (dataLayer2 != null && !dataLayer2.isEmpty()) {
                    compoundTag3.putByteArray("SkyLight", dataLayer2.getData());
                }
                
                listTag.add(compoundTag3);
            }
        }
        
        compoundTag2.put("Sections", listTag);
        if (isLightCorrect) {
            compoundTag2.putBoolean("isLightOn", true);
        }
        
        ChunkBiomeContainer chunkBiomeContainer = chunk.getBiomes();
        if (chunkBiomeContainer != null) {
            compoundTag2.putIntArray("Biomes", chunkBiomeContainer.writeBiomes());
        }
        
        ListTag listTag2 = new ListTag();
        
        for(BlockPos blockPos : chunk.getBlockEntitiesPos()) {
            CompoundTag compoundTag4 = chunk.getBlockEntityNbtForSaving(blockPos);
            if (compoundTag4 != null) {
                listTag2.add(compoundTag4);
            }
        }
        
        compoundTag2.put("TileEntities", listTag2);
        ListTag listTag3 = new ListTag();
        if (chunk.getStatus().getChunkType() == ChunkStatus.ChunkType.LEVELCHUNK) {
            LevelChunk levelChunk = (LevelChunk)chunk;
            levelChunk.setLastSaveHadEntities(false);
            
            for(int k = 0; k < levelChunk.getEntitySections().length; ++k) {
                for(Entity entity : levelChunk.getEntitySections()[k]) {
                    CompoundTag compoundTag5 = new CompoundTag();
                    if (entity.save(compoundTag5)) {
                        levelChunk.setLastSaveHadEntities(true);
                        listTag3.add(compoundTag5);
                    }
                }
            }
        } else {
            ProtoChunk protoChunk = (ProtoChunk)chunk;
            listTag3.addAll(protoChunk.getEntities());
            compoundTag2.put("Lights", packOffsets(protoChunk.getPackedLights()));
            CompoundTag compoundTag4 = new CompoundTag();
            
            for(GenerationStep.Carving carving : GenerationStep.Carving.values()) {
                BitSet bitSet = protoChunk.getCarvingMask(carving);
                if (bitSet != null) {
                    compoundTag4.putByteArray(carving.toString(), bitSet.toByteArray());
                }
            }
            
            compoundTag2.put("CarvingMasks", compoundTag4);
        }
        
        compoundTag2.put("Entities", listTag3);
        TickList<Block> blockTicks = chunk.getBlockTicks();
        if (blockTicks instanceof ProtoTickList) {
            compoundTag2.put("ToBeTicked", ((ProtoTickList<Block>) blockTicks).save());
        } else if (blockTicks instanceof ChunkTickList) {
            compoundTag2.put("TileTicks", ((ChunkTickList<Block>) blockTicks).save());
        } else {
            compoundTag2.put("TileTicks", level.getBlockTicks().save(chunkPos));
        }
        
        TickList<Fluid> liquidTicks = chunk.getLiquidTicks();
        if (liquidTicks instanceof ProtoTickList) {
            compoundTag2.put("LiquidsToBeTicked", ((ProtoTickList<Fluid>) liquidTicks).save());
        } else if (liquidTicks instanceof ChunkTickList) {
            compoundTag2.put("LiquidTicks", ((ChunkTickList<Fluid>) liquidTicks).save());
        } else {
            compoundTag2.put("LiquidTicks", level.getLiquidTicks().save(chunkPos));
        }
        
        compoundTag2.put("PostProcessing", packOffsets(chunk.getPostProcessing()));
        CompoundTag compoundTag3 = new CompoundTag();
        
        for(Map.Entry<Heightmap.Types, Heightmap> entry : chunk.getHeightmaps()) {
            if (chunk.getStatus().heightmapsAfter().contains(entry.getKey())) {
                compoundTag3.put(entry.getKey().getSerializationKey(), new LongArrayTag(entry.getValue().getRawData()));
            }
        }
        
        compoundTag2.put("Heightmaps", compoundTag3);
        compoundTag2.put("Structures", packStructureData(chunkPos, chunk.getAllStarts(), chunk.getAllReferences()));
        cir.setReturnValue(compoundTag);
    }
}
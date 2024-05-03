package com.blackgear.offlimits.mixin.common.level.chunk;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {
    // ========== READ ==========
    
    @ModifyConstant(
        method = "read",
        constant = @Constant(intValue = 16)
    )
    private static int offlimits$read(int original) {
        return Offlimits.INSTANCE.getSectionsCount();
    }
    
    /**
     * There must be a better way to do this, I am unsure but there should be.
     */
    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(
        method = "read",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;isEmpty()Z"
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void offlimits$readVerticalSections(
        ServerLevel level,
        StructureManager templateManager,
        PoiManager poiManager,
        ChunkPos pos,
        CompoundTag compound,
        CallbackInfoReturnable<ProtoChunk> cir,
        ChunkGenerator chunkGenerator,
        BiomeSource biomeSource,
        CompoundTag levelTag,
        ChunkBiomeContainer chunkBiomeContainer,
        UpgradeData upgradeData,
        ProtoTickList<Block> blockTicks,
        ProtoTickList<Fluid> fluidTicks,
        boolean isLightOn,
        ListTag sections,
        int sectionCount,
        LevelChunkSection[] levelChunkSections,
        boolean hasSkyLight,
        ChunkSource chunkSource,
        LevelLightEngine levelLightEngine,
        int section,
        CompoundTag sectionTag,
        int y,
        LevelChunkSection levelChunkSection
    ) {
        if (!levelChunkSection.isEmpty()) {
            levelChunkSections[Offlimits.INSTANCE.getSectionIndexFromSectionY(y)] = levelChunkSection;
        }
    }
    
    /**
     * Ditto...
     */
    @Redirect(
        method = "read",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;isEmpty()Z"
        )
    )
    private static boolean offlimits$ignoreVanillaSections(LevelChunkSection instance) {
        return false;
    }
    
    // ========== WRITE ==========
    
    @ModifyConstant(
        method = "write",
        constant = @Constant(intValue = 17)
    )
    private static int offlimits$write(int original) {
        return Offlimits.LIGHT.getLightSectionCount();
    }
}
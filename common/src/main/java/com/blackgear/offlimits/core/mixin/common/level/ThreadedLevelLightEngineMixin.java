package com.blackgear.offlimits.core.mixin.common.level;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.Util;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.function.IntSupplier;

@Mixin(ChunkMap.class)
interface ChunkMapAccessor {
    @Invoker
    void callReleaseLightTicket(ChunkPos chunkPos);
}

@Mixin(ThreadedLevelLightEngine.class)
public abstract class ThreadedLevelLightEngineMixin extends LevelLightEngine {
    
    @Shadow protected abstract void addTask(int i, int j, IntSupplier supplier, ThreadedLevelLightEngine.TaskType taskType, Runnable runnable);
    @Shadow protected abstract void addTask(int i, int j, ThreadedLevelLightEngine.TaskType taskType, Runnable runnable);
    
    @Shadow @Final private ChunkMap chunkMap;
    
    public ThreadedLevelLightEngineMixin(LightChunkGetter lightChunkGetter, boolean bl, boolean bl2) {
        super(lightChunkGetter, bl, bl2);
    }
    
    @Inject(
        method = "updateChunkStatus",
        at = @At("HEAD"),
        cancellable = true
    )
    private void off$updateChunkStatus(ChunkPos pos, CallbackInfo ci) {
        this.addTask(pos.x, pos.z, () -> 0, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, Util.name(() -> {
            super.retainData(pos, false);
            super.enableLightSources(pos, false);
            
            for (int i = Offlimits.LIGHT.getMinLightSection(); i < Offlimits.LIGHT.getMaxLightSection(); i++) {
                super.queueSectionData(LightLayer.BLOCK, SectionPos.of(pos, i), null, true);
                super.queueSectionData(LightLayer.SKY, SectionPos.of(pos, i), null, true);
            }
            
            for (int i = Offlimits.LEVEL.getMinSection(); i < Offlimits.LEVEL.getMaxSection(); i++) {
                super.updateSectionStatus(SectionPos.of(pos, i), true);
            }
        }, () -> "updateChunkStatus " + pos + " " + true));
        ci.cancel();
    }
    
    @Inject(
        method = "lightChunk",
        at = @At("HEAD"),
        cancellable = true
    )
    private void off$updateChunkStatus(ChunkAccess chunkAccess, boolean bl, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
        ChunkPos chunkPos = chunkAccess.getPos();
        chunkAccess.setLightCorrect(false);
        this.addTask(chunkPos.x, chunkPos.z, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, Util.name(() -> {
            LevelChunkSection[] levelChunkSections = chunkAccess.getSections();
            
            for(int i = 0; i < Offlimits.LEVEL.getSectionsCount(); ++i) {
                LevelChunkSection levelChunkSection = levelChunkSections[i];
                if (!LevelChunkSection.isEmpty(levelChunkSection)) {
                    int j = Offlimits.LEVEL.getSectionYFromSectionIndex(i);
                    super.updateSectionStatus(SectionPos.of(chunkPos, j), false);
                }
            }
            
            super.enableLightSources(chunkPos, true);
            if (!bl) {
                chunkAccess.getLights().forEach((blockPos) -> {
                    super.onBlockEmissionIncrease(blockPos, chunkAccess.getLightEmission(blockPos));
                });
            }
            
            ((ChunkMapAccessor) this.chunkMap).callReleaseLightTicket(chunkPos);
        }, () -> {
            return "lightChunk " + chunkPos + " " + bl;
        }));
        
        cir.setReturnValue(
            CompletableFuture.supplyAsync(() -> {
                chunkAccess.setLightCorrect(true);
                super.retainData(chunkPos, false);
                return chunkAccess;
            }, (runnable) -> {
                this.addTask(chunkPos.x, chunkPos.z, ThreadedLevelLightEngine.TaskType.POST_UPDATE, runnable);
            })
        );
    }
}
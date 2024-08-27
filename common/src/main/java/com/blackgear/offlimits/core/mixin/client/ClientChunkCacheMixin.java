package com.blackgear.offlimits.core.mixin.client;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicReferenceArray;

@Mixin(ClientChunkCache.Storage.class)
interface StorageAccessor {
    @Invoker
    boolean callInRange(int x, int z);
    
    @Accessor
    AtomicReferenceArray<LevelChunk> getChunks();
    
    @Invoker
    int callGetIndex(int x, int z);
    
    @Invoker
    void callReplace(int chunkIndex, @Nullable LevelChunk chunk);
}

@Mixin(ClientChunkCache.class)
public abstract class ClientChunkCacheMixin extends ChunkSource {
    @Shadow @Final private static Logger LOGGER;
    
    @Shadow private static boolean isValidChunk(@Nullable LevelChunk chunk, int x, int z) { return false; }
    
    @Shadow private volatile ClientChunkCache.Storage storage;
    @Shadow @Final private ClientLevel level;
    
    @Inject(
        method = "replaceWithPacketData",
        at = @At("HEAD"),
        cancellable = true
    )
    public void replaceWithPacketData(
        int i,
        int j,
        ChunkBiomeContainer chunkBiomeContainer,
        FriendlyByteBuf friendlyByteBuf,
        CompoundTag compoundTag,
        int k,
        boolean bl,
        CallbackInfoReturnable<LevelChunk> cir
    ) {
        StorageAccessor storage = (StorageAccessor) this.storage;
        
        if (!storage.callInRange(i, j)) {
            LOGGER.warn("Ignoring chunk since it's not in the view range: {}, {}", i, j);
            cir.setReturnValue(null);
        } else {
            int l = storage.callGetIndex(i, j);
            LevelChunk levelChunk = storage.getChunks().get(l);
            if (!bl && isValidChunk(levelChunk, i, j)) {
                levelChunk.replaceWithPacketData(chunkBiomeContainer, friendlyByteBuf, compoundTag, k);
            } else {
                if (chunkBiomeContainer == null) {
                    LOGGER.warn("Ignoring chunk since we don't have complete data: {}, {}", i, j);
                    cir.setReturnValue(null);
                }
                
                levelChunk = new LevelChunk(this.level, new ChunkPos(i, j), chunkBiomeContainer);
                levelChunk.replaceWithPacketData(chunkBiomeContainer, friendlyByteBuf, compoundTag, k);
                storage.callReplace(l, levelChunk);
            }
            
            LevelChunkSection[] levelChunkSections = levelChunk.getSections();
            LevelLightEngine levelLightEngine = this.getLightEngine();
            levelLightEngine.enableLightSources(new ChunkPos(i, j), true);
            
            for(int m = 0; m < levelChunkSections.length; ++m) {
                LevelChunkSection levelChunkSection = levelChunkSections[m];
                int n = Offlimits.INSTANCE.getSectionYFromSectionIndex(m);
                levelLightEngine.updateSectionStatus(SectionPos.of(i, n, j), LevelChunkSection.isEmpty(levelChunkSection));
            }
            
            this.level.onChunkLoaded(i, j);
            cir.setReturnValue(levelChunk);
        }
    }
}
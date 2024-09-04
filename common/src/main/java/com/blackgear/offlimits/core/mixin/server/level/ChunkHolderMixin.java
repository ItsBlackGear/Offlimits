package com.blackgear.offlimits.core.mixin.server.level;

import com.blackgear.offlimits.Offlimits;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkHolder.class)
public abstract class ChunkHolderMixin {
    @Mutable @Shadow @Final private ShortSet[] changedBlocksPerSection;
    
    @Shadow private boolean hasChangedSections;
    
    @Shadow private int skyChangedLightSectionFilter;
    
    @Shadow private int blockChangedLightSectionFilter;
    
    @Shadow private boolean resendLight;
    
    @Shadow protected abstract void broadcast(Packet<?> packet, boolean boundaryOnly);
    
    @Shadow @Final private LevelLightEngine lightEngine;
    
    @Shadow protected abstract void broadcastBlockEntityIfNeeded(Level level, BlockPos blockPos, BlockState blockState);
    
    @Shadow @Nullable public abstract LevelChunk getTickingChunk();
    
    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    public void offlimits$init(
        ChunkPos chunkPos,
        int ticketLevel,
        LevelLightEngine levelLightEngine,
        ChunkHolder.LevelChangeListener levelChangeListener,
        ChunkHolder.PlayerProvider playerProvider,
        CallbackInfo ci
    ) {
        this.changedBlocksPerSection = new ShortSet[Offlimits.INSTANCE.getSectionsCount()];
    }
    
    @Inject(
        method = "blockChanged",
        at = @At("HEAD"),
        cancellable = true
    )
    public void blockChanged(BlockPos pos, CallbackInfo ci) {
        LevelChunk chunk = this.getTickingChunk();
        if (chunk != null) {
            int i = Offlimits.INSTANCE.getSectionIndex(pos.getY());
            if (this.changedBlocksPerSection[i] == null) {
                this.hasChangedSections = true;
                this.changedBlocksPerSection[i] = new ShortOpenHashSet();
            }
            
            this.changedBlocksPerSection[i].add(SectionPos.sectionRelativePos(pos));
        }
        
        ci.cancel();
    }
    
    @Inject(
        method = "sectionLightChanged",
        at = @At("HEAD"),
        cancellable = true
    )
    public void sectionLightChanged(LightLayer type, int sectionY, CallbackInfo ci) {
        LevelChunk chunk = this.getTickingChunk();
        if (chunk != null) {
            chunk.setUnsaved(true);
            int minSection = Offlimits.LIGHT.getMinLightSection();
            int maxSection = Offlimits.LIGHT.getMaxLightSection();
            
            if (sectionY >= minSection && sectionY <= maxSection) {
                int y = sectionY - minSection;
                if (type == LightLayer.SKY) {
                    this.skyChangedLightSectionFilter |= 1 << y;
                } else {
                    this.blockChangedLightSectionFilter |= 1 << y;
                }
            }
        }
        
        ci.cancel();
    }
    
    @Inject(
        method = "broadcastChanges",
        at = @At("HEAD"),
        cancellable = true
    )
    public void broadcastChanges(LevelChunk chunk, CallbackInfo ci) {
        if (this.hasChangedSections || this.skyChangedLightSectionFilter != 0 || this.blockChangedLightSectionFilter != 0) {
            Level level = chunk.getLevel();
            int i = 0;
            
            for (ShortSet shorts : this.changedBlocksPerSection) {
                i += shorts != null ? shorts.size() : 0;
            }
            
            this.resendLight |= i >= 64;
            if (this.skyChangedLightSectionFilter != 0 || this.blockChangedLightSectionFilter != 0) {
                this.broadcast(new ClientboundLightUpdatePacket(chunk.getPos(), this.lightEngine, this.skyChangedLightSectionFilter, this.blockChangedLightSectionFilter, true), !this.resendLight);
                this.skyChangedLightSectionFilter = 0;
                this.blockChangedLightSectionFilter = 0;
            }
            
            for(int j = 0; j < this.changedBlocksPerSection.length; ++j) {
                ShortSet shortSet = this.changedBlocksPerSection[j];
                if (shortSet != null) {
                    int k = Offlimits.INSTANCE.getSectionYFromSectionIndex(j);
                    SectionPos sectionPos = SectionPos.of(chunk.getPos(), k);
                    if (shortSet.size() == 1) {
                        BlockPos blockPos = sectionPos.relativeToBlockPos(shortSet.iterator().nextShort());
                        BlockState blockState = level.getBlockState(blockPos);
                        this.broadcast(new ClientboundBlockUpdatePacket(blockPos, blockState), false);
                        this.broadcastBlockEntityIfNeeded(level, blockPos, blockState);
                    } else {
                        LevelChunkSection section = chunk.getSections()[j];
                        ClientboundSectionBlocksUpdatePacket sectionBlockUpdate = new ClientboundSectionBlocksUpdatePacket(sectionPos, shortSet, section, this.resendLight);
                        this.broadcast(sectionBlockUpdate, false);
                        sectionBlockUpdate.runUpdates((blockPosx, blockStatex) -> {
                            this.broadcastBlockEntityIfNeeded(level, blockPosx, blockStatex);
                        });
                    }
                    
                    this.changedBlocksPerSection[j] = null;
                }
            }
            
            this.hasChangedSections = false;
        }
        
        ci.cancel();
    }
}
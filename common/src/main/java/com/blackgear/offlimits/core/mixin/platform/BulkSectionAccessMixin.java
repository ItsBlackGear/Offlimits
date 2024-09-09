package com.blackgear.offlimits.core.mixin.platform;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.platform.common.worldgen.BulkSectionAccess;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BulkSectionAccess.class)
public class BulkSectionAccessMixin {
    @Shadow @Final private LevelAccessor level;
    @Shadow @Final private Long2ObjectMap<LevelChunkSection> acquiredSections;
    @Shadow private @Nullable LevelChunkSection lastSection;
    @Shadow private long lastSectionKey;
    
    @Inject(
        method = "getSection",
        at = @At("HEAD"),
        cancellable = true
    )
    private void off$getSection(BlockPos pos, CallbackInfoReturnable<LevelChunkSection> cir) {
        int sectionY = Offlimits.LEVEL.getSectionIndex(pos.getY());
        if (sectionY >= 0 && sectionY < Offlimits.LEVEL.getSectionsCount()) {
            long sectionKey = SectionPos.asLong(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(sectionY), SectionPos.blockToSectionCoord(pos.getZ()));
            if (this.lastSection == null || this.lastSectionKey != sectionKey) {
                this.lastSection = this.acquiredSections.computeIfAbsent(sectionKey, lx -> {
                    ChunkAccess chunk = this.level.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
                    LevelChunkSection levelChunkSection = ((ProtoChunk) chunk).getOrCreateSection(sectionY);
                    levelChunkSection.acquire();
                    return levelChunkSection;
                });
                this.lastSectionKey = sectionKey;
            }
            
            cir.setReturnValue(this.lastSection);
        } else {
            cir.setReturnValue(null);
        }
    }
}
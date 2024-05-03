package com.blackgear.offlimits.fabric.mixin.sodium;

import com.blackgear.offlimits.Offlimits;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSection;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo @Mixin(ClonedChunkSection.class)
public class ClonedChunkSectionMixin {
    /**
     * @author ItsBlackGear
     * @reason TODO; too lazy to figure this out right now...
     */
    @Overwrite
    private static LevelChunkSection getChunkSection(ChunkAccess chunk, SectionPos pos) {
        LevelChunkSection section = null;
        if (!Level.isOutsideBuildHeight(SectionPos.sectionToBlockCoord(pos.getY()))) {
            section = chunk.getSections()[Offlimits.INSTANCE.getSectionIndexFromSectionY(pos.getY())];
        }
        
        return section;
    }
}
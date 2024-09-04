package com.blackgear.offlimits.core.mixin.common.level.chunk;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkAccess.class)
public interface ChunkAccessMixin {
    @Shadow LevelChunkSection[] getSections();
    
    @Shadow @Nullable LevelChunkSection getHighestSection();
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    default int getHighestSectionPosition() {
        LevelChunkSection levelChunkSection = this.getHighestSection();
        return levelChunkSection == null ? Offlimits.LEVEL.getMinBuildHeight() : levelChunkSection.bottomBlockY();
    }
    
    /**
     * @author ItsBlackGear
     * @reason we cannot modify the constant in an interface.
     */
    @Overwrite
    default boolean isYSpaceEmpty(int minY, int maxY) {
        if (minY < Offlimits.LEVEL.getMinBuildHeight()) {
            minY = Offlimits.LEVEL.getMinBuildHeight();
        }
        
        if (maxY >= Offlimits.LEVEL.getMaxBuildHeight()) {
            maxY = Offlimits.LEVEL.getMaxBuildHeight() - 1;
        }
        
        for (int y = minY; y <= maxY; y+= 16) {
            if (!LevelChunkSection.isEmpty(this.getSections()[Offlimits.LEVEL.getSectionIndex(y)])) {
                return false;
            }
        }
        
        return true;
    }
}
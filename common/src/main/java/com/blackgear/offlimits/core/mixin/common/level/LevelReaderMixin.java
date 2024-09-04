package com.blackgear.offlimits.core.mixin.common.level;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LevelReader.class)
public interface LevelReaderMixin {
    @Shadow @Deprecated boolean hasChunk(int x, int z);
    
    /**
     * @author ItsBlackGear
     * @reason we cannot modify the constant in an interface.
     */
    @Overwrite @Deprecated
    default boolean hasChunksAt(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        if (toY >= Offlimits.LEVEL.getMinBuildHeight() && fromY < Offlimits.LEVEL.getMaxBuildHeight()) {
            int minX = SectionPos.blockToSectionCoord(fromX);
            int minZ = SectionPos.blockToSectionCoord(fromZ);
            int maxX = SectionPos.blockToSectionCoord(toX);
            int maxZ = SectionPos.blockToSectionCoord(toZ);
            
            for(int x = minX; x <= maxX; ++x) {
                for(int z = minZ; z <= maxZ; ++z) {
                    if (!hasChunk(x, z)) {
                        return false;
                    }
                }
            }
            
            return true;
        }
        
        return false;
    }
}
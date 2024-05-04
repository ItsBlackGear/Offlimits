package com.blackgear.offlimits.core.mixin.common.level;

import com.blackgear.offlimits.Offlimits;
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
    default boolean hasChunksAt(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (maxY >= Offlimits.INSTANCE.getMinBuildHeight() && minY < Offlimits.INSTANCE.getMaxBuildHeight()) {
            minX >>= 4;
            minZ >>= 4;
            maxX >>= 4;
            maxZ >>= 4;
            
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
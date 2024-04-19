package com.blackgear.offlimits.mixin.server.level;

import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {
    @ModifyConstant(
        method = "playerLoadedChunk",
        constant = @Constant(intValue = 0x0000_FFFF)
    )
    private int offlimits$playerLoadedChunk(int original) {
        return 0xFFFF_FFFF;
    }
}
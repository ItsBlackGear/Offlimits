package com.blackgear.offlimits.core.mixin.server.level;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.world.level.ServerTickList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ServerTickList.class)
public class ServerTickListMixin {
    @ModifyConstant(
        method = "fetchTicksInChunk",
        constant = @Constant(intValue = 256)
    )
    private int offlimits$fetchTicksInChunk(int original){
        return Offlimits.INSTANCE.getMaxBuildHeight();
    }
}
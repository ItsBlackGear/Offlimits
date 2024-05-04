package com.blackgear.offlimits.core.mixin.common.level.chunk;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {
    @ModifyConstant(
        method = "read",
        constant = @Constant(intValue = 16)
    )
    private static int offlimits$read(int original) {
        return Offlimits.INSTANCE.getSectionsCount();
    }
    
    @ModifyConstant(
        method = "write",
        constant = @Constant(intValue = 17)
    )
    private static int offlimits$write(int original) {
        return Offlimits.LIGHT.getLightSectionCount();
    }
}
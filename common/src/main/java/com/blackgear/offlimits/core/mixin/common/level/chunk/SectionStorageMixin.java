package com.blackgear.offlimits.core.mixin.common.level.chunk;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(SectionStorage.class)
public class SectionStorageMixin {
    @ModifyConstant(
        method = "readColumn(Lnet/minecraft/world/level/ChunkPos;Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)V",
        constant = @Constant(intValue = 16)
    )
    private int off$readColumnMax(int original) {
        return Offlimits.INSTANCE.getMaxSection();
    }
    
    @ModifyConstant(
        method = "readColumn(Lnet/minecraft/world/level/ChunkPos;Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)V",
        constant = @Constant(intValue = 0)
    )
    private int off$readColumnMin(int original) {
        return Offlimits.INSTANCE.getMinSection();
    }
    
    @ModifyConstant(
        method = "writeColumn(Lnet/minecraft/world/level/ChunkPos;Lcom/mojang/serialization/DynamicOps;)Lcom/mojang/serialization/Dynamic;",
        constant = @Constant(intValue = 16)
    )
    private int off$writeColumnMax(int original) {
        return Offlimits.INSTANCE.getMaxSection();
    }
    
    
    @ModifyConstant(
        method = "writeColumn(Lnet/minecraft/world/level/ChunkPos;Lcom/mojang/serialization/DynamicOps;)Lcom/mojang/serialization/Dynamic;",
        constant = @Constant(intValue = 0)
    )
    private int off$writeColumnMin(int original) {
        return Offlimits.INSTANCE.getMinSection();
    }
    
    @ModifyConstant(
        method = "flush",
        constant = @Constant(intValue = 16)
    )
    private int off$flushMax(int original) {
        return Offlimits.INSTANCE.getMaxSection();
    }
    
    @ModifyConstant(
        method = "flush",
        constant = @Constant(intValue = 0)
    )
    private int off$flushMin(int original) {
        return Offlimits.INSTANCE.getMinSection();
    }
}
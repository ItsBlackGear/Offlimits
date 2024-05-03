package com.blackgear.offlimits.mixin.common.level.chunk;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(SectionStorage.class)
public class SectionStorageMixin {
    
    // ========== Read Column ==========
    
    @ModifyConstant(
        method = "readColumn(Lnet/minecraft/world/level/ChunkPos;Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)V",
        constant = @Constant(intValue = 16)
    )
    private int offlimits$readColumnMax(int original) {
        return Offlimits.INSTANCE.getMaxSection();
    }
    
    @ModifyConstant(
        method = "readColumn(Lnet/minecraft/world/level/ChunkPos;Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)V",
        constant = @Constant(intValue = 0)
    )
    private int offlimits$readColumnMin(int original) {
        return Offlimits.INSTANCE.getMinSection();
    }
    
    // ========== Write Column ==========
    
    @ModifyConstant(
        method = "writeColumn(Lnet/minecraft/world/level/ChunkPos;Lcom/mojang/serialization/DynamicOps;)Lcom/mojang/serialization/Dynamic;",
        constant = @Constant(intValue = 16)
    )
    private int offlimits$writeColumnMax(int original) {
        return Offlimits.INSTANCE.getMaxSection();
    }
    
    
    @ModifyConstant(
        method = "writeColumn(Lnet/minecraft/world/level/ChunkPos;Lcom/mojang/serialization/DynamicOps;)Lcom/mojang/serialization/Dynamic;",
        constant = @Constant(intValue = 0)
    )
    private int offlimits$writeColumnMin(int original) {
        return Offlimits.INSTANCE.getMinSection();
    }
    
    // ========== Flush ==========
    
    @ModifyConstant(
        method = "flush",
        constant = @Constant(intValue = 16)
    )
    private int offlimits$flushMax(int original) {
        return Offlimits.INSTANCE.getMaxSection();
    }
    
    @ModifyConstant(
        method = "flush",
        constant = @Constant(intValue = 0)
    )
    private int offlimits$flushMin(int original) {
        return Offlimits.INSTANCE.getMinSection();
    }
}
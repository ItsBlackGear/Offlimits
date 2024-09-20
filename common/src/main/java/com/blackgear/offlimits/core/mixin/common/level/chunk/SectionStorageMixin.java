package com.blackgear.offlimits.core.mixin.common.level.chunk;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(SectionStorage.class)
public abstract class SectionStorageMixin {
    @ModifyConstant(
        method = {
            "readColumn(Lnet/minecraft/world/level/ChunkPos;Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)V"
        },
        constant = {
            @Constant(intValue = 0, ordinal = 0),
            @Constant(intValue = 0, ordinal = 2)
        }
    )
    private int offlimits$updateValidReadMinSections(int constant) {
        return Offlimits.LEVEL.getMinSection();
    }
    
    @ModifyConstant(
        method = {
            "writeColumn(Lnet/minecraft/world/level/ChunkPos;Lcom/mojang/serialization/DynamicOps;)Lcom/mojang/serialization/Dynamic;",
            "flush"
        },
        constant = @Constant(intValue = 0)
    )
    private int offlimits$updateMinSections(int constant) {
        return Offlimits.LEVEL.getMinSection();
    }
    
    @ModifyConstant(
        method = {
            "readColumn(Lnet/minecraft/world/level/ChunkPos;Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)V",
            "writeColumn(Lnet/minecraft/world/level/ChunkPos;Lcom/mojang/serialization/DynamicOps;)Lcom/mojang/serialization/Dynamic;",
            "flush"
        },
        constant = @Constant(intValue = 16)
    )
    private int offlimits$updateMaxSections(int constant) {
        return Offlimits.LEVEL.getMaxSection();
    }
}
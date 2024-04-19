package com.blackgear.offlimits.mixin.common.level.chunk;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.world.level.chunk.ProtoTickList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ProtoTickList.class)
public class ProtoTickListMixin {
    @ModifyConstant(
        method = "<init>(Ljava/util/function/Predicate;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/nbt/ListTag;)V",
        constant = @Constant(intValue = 16)
    )
    private int init(int original) {
        return Offlimits.INSTANCE.getSectionsCount();
    }
}
package com.blackgear.offlimits.core.mixin;

import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.GenerationStep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.BitSet;
import java.util.Map;

@Mixin(ProtoChunk.class)
public interface ProtoChunkAccessor {
    @Accessor
    Map<GenerationStep.Carving, BitSet> getCarvingMasks();
}

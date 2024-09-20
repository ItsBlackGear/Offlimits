package com.blackgear.offlimits.core.mixin.common.level;

import com.blackgear.offlimits.Offlimits;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.function.Function;

@Mixin(PoiSection.class)
interface PoiSectionAccessor {
    @Invoker
    boolean callIsValid();
}

@Mixin(PoiManager.class)
public class PoiManagerMixin extends SectionStorage<PoiSection> {
    @Shadow @Final private LongSet loadedChunks;
    
    public PoiManagerMixin(File file, Function<Runnable, Codec<PoiSection>> function, Function<Runnable, PoiSection> function2, DataFixer dataFixer, DataFixTypes dataFixTypes, boolean bl) {
        super(file, function, function2, dataFixer, dataFixTypes, bl);
    }
    
    @ModifyConstant(
        method = "getInChunk",
        constant = @Constant(intValue = 16)
    )
    private int off$getInChunkMax(int original) {
        return Offlimits.LEVEL.getMaxSection();
    }
    
    @ModifyConstant(
        method = "getInChunk",
        constant = @Constant(intValue = 0)
    )
    private int off$getInChunkMin(int original) {
        return Offlimits.LEVEL.getMinSection();
    }
    
    @Inject(
        method = "ensureLoadedAndValid",
        at = @At("HEAD"),
        cancellable = true
    )
    private void off$ensureLoadedAndValid(
        LevelReader levelReader,
        BlockPos pos,
        int coordinateOffset,
        CallbackInfo ci
    ) {
        SectionPos.aroundChunk(new ChunkPos(pos), Math.floorDiv(coordinateOffset, 16))
            .map(sectionPos -> Pair.of(sectionPos, this.getOrLoad(sectionPos.asLong())))
            .filter(pair -> !(pair.getSecond()).map(poiSection -> ((PoiSectionAccessor) poiSection).callIsValid()).orElse(false))
            .map(pair -> pair.getFirst().chunk())
            .filter(chunkPos -> this.loadedChunks.add(chunkPos.toLong()))
            .forEach(chunkPos -> levelReader.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.EMPTY));
        ci.cancel();
    }
}
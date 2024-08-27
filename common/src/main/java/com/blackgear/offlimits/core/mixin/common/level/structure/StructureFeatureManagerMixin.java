package com.blackgear.offlimits.core.mixin.common.level.structure;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.FeatureAccess;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Stream;

@Mixin(StructureFeatureManager.class)
public abstract class StructureFeatureManagerMixin {
    @Shadow @Final private LevelAccessor level;
    
    @Shadow @Nullable public abstract StructureStart<?> getStartForFeature(SectionPos sectionPos, StructureFeature<?> structure, FeatureAccess reader);
    
    @Inject(
        method = "startsForFeature",
        at = @At("HEAD"),
        cancellable = true
    )
    public void startsForFeature(SectionPos pos, StructureFeature<?> structure, CallbackInfoReturnable<Stream<? extends StructureStart<?>>> cir) {
        cir.setReturnValue(
            this.level
                .getChunk(pos.x(), pos.z(), ChunkStatus.STRUCTURE_REFERENCES)
                .getReferencesForFeature(structure)
                .stream()
                .map(long_ -> SectionPos.of(new ChunkPos(long_), Offlimits.INSTANCE.getMinSection()))
                .map(sectionPos -> this.getStartForFeature(sectionPos, structure, this.level.getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.STRUCTURE_STARTS)))
                .filter(structureStart -> structureStart != null && structureStart.isValid())
        );
    }
}
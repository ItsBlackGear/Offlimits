package com.blackgear.offlimits.core.mixin.server.level;

import com.blackgear.offlimits.Offlimits;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkHolder.class)
public class ChunkHolderMixin {
    @Mutable @Shadow @Final private ShortSet[] changedBlocksPerSection;
    
    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    public void offlimits$init(
        ChunkPos chunkPos,
        int ticketLevel,
        LevelLightEngine levelLightEngine,
        ChunkHolder.LevelChangeListener levelChangeListener,
        ChunkHolder.PlayerProvider playerProvider,
        CallbackInfo ci
    ) {
        this.changedBlocksPerSection = new ShortSet[Offlimits.INSTANCE.getSectionsCount()];
    }
}
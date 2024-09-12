package com.blackgear.offlimits.core.mixin.client;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.ChunkSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientChunkCache.class)
public abstract class ClientChunkCacheMixin extends ChunkSource {
    @Redirect(
        method = "replaceWithPacketData",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/SectionPos;of(III)Lnet/minecraft/core/SectionPos;"
        )
    )
    private SectionPos offlimits$updateSectionStatus(int chunkX, int chunkY, int chunkZ) {
        return SectionPos.of(chunkX, Offlimits.LEVEL.getSectionYFromSectionIndex(chunkY), chunkZ);
    }
}
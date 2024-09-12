package com.blackgear.offlimits.core.forge.mixin.compat.embeddium;

import com.blackgear.offlimits.Offlimits;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSection;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo @Mixin(WorldSlice.class)
public abstract class WorldSliceMixin {
    @Shadow
    @Final
    private static int NEIGHBOR_BLOCK_RADIUS;
    @Shadow @Final private static int NEIGHBOR_CHUNK_RADIUS;
    @Shadow @Final private static int SECTION_TABLE_ARRAY_SIZE;
    
    @Shadow public static int getLocalSectionIndex(int x, int y, int z) { throw new AssertionError(); }
    
    @Inject(
        method = "prepare",
        at = @At("HEAD"),
        remap = false,
        cancellable = true
    )
    private static void offlimits$prepare(Level world, SectionPos origin, ClonedChunkSectionCache sectionCache, CallbackInfoReturnable<ChunkRenderContext> cir) {
        LevelChunk chunk = world.getChunk(origin.getX(), origin.getZ());
        LevelChunkSection section = chunk.getSections()[Offlimits.LEVEL.getSectionIndexFromSectionY(origin.getY())];
        
        // If the chunk section is absent or empty, simply terminate now. There will never be anything in this chunk
        // section to render, so we need to signal that a chunk render task shouldn't created. This saves a considerable
        // amount of time in queueing instant build tasks and greatly accelerates how quickly the world can be loaded.
        if (LevelChunkSection.isEmpty(section)) {
            cir.setReturnValue(null);
        }
        
        BoundingBox volume = new BoundingBox(origin.minBlockX() - NEIGHBOR_BLOCK_RADIUS,
            origin.minBlockY() - NEIGHBOR_BLOCK_RADIUS,
            origin.minBlockZ() - NEIGHBOR_BLOCK_RADIUS,
            origin.maxBlockX() + NEIGHBOR_BLOCK_RADIUS,
            origin.maxBlockY() + NEIGHBOR_BLOCK_RADIUS,
            origin.maxBlockZ() + NEIGHBOR_BLOCK_RADIUS);
        
        // The min/max bounds of the chunks copied by this slice
        final int minChunkX = origin.getX() - NEIGHBOR_CHUNK_RADIUS;
        final int minChunkY = origin.getY() - NEIGHBOR_CHUNK_RADIUS;
        final int minChunkZ = origin.getZ() - NEIGHBOR_CHUNK_RADIUS;
        
        final int maxChunkX = origin.getX() + NEIGHBOR_CHUNK_RADIUS;
        final int maxChunkY = origin.getY() + NEIGHBOR_CHUNK_RADIUS;
        final int maxChunkZ = origin.getZ() + NEIGHBOR_CHUNK_RADIUS;
        
        ClonedChunkSection[] sections = new ClonedChunkSection[SECTION_TABLE_ARRAY_SIZE];
        
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                for (int chunkY = minChunkY; chunkY <= maxChunkY; chunkY++) {
                    sections[getLocalSectionIndex(chunkX - minChunkX, chunkY - minChunkY, chunkZ - minChunkZ)] =
                        sectionCache.acquire(chunkX, chunkY, chunkZ);
                }
            }
        }
        
        cir.setReturnValue(new ChunkRenderContext(origin, sections, volume));
    }
}
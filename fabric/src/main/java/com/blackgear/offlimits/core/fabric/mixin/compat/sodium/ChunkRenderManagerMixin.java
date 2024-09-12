package com.blackgear.offlimits.core.fabric.mixin.compat.sodium;

import com.blackgear.offlimits.Offlimits;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.*;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.common.util.IdTable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo @Mixin(ChunkRenderManager.class)
public class ChunkRenderManagerMixin<T extends ChunkGraphicsState> {
    @Shadow @Final private ChunkRenderBackend<T> backend;
    @Shadow @Final private SodiumWorldRenderer renderer;
    @Shadow @Final private ClientLevel world;
    @Shadow @Final private IdTable<ChunkRenderContainer<T>> renders;
    
    @ModifyConstant(
        method = {
            "loadSections",
            "unloadSections"
        },
        constant = @Constant(intValue = 16),
        remap = false
    )
    private int offlimits$updateMaxSections(int original) {
        return Offlimits.LEVEL.getMaxSection();
    }

    @ModifyConstant(
        method = {
            "loadSections",
            "unloadSections"
        },
        constant = @Constant(intValue = 0),
        remap = false
    )
    private int offlimits$updateMinSections(int original) {
        return Offlimits.LEVEL.getMinSection();
    }
    
    @Inject(
        method = "createChunkRender",
        at = @At("HEAD"),
        remap = false,
        cancellable = true
    )
    private void offlimits$createChunkRender(ChunkRenderColumn<T> column, int x, int y, int z, CallbackInfoReturnable<ChunkRenderContainer<T>> cir) {
        ChunkRenderContainer<T> render = new ChunkRenderContainer<>(this.backend, this.renderer, x, y, z, column);
        LevelChunkSection section = this.world.getChunk(x, z).getSections()[Offlimits.LEVEL.getSectionIndexFromSectionY(y)];
        
        if (LevelChunkSection.isEmpty(section)) {
            render.setData(ChunkRenderData.EMPTY);
        } else {
            render.scheduleRebuild(false);
        }
        
        render.setId(this.renders.add(render));
        cir.setReturnValue(render);
    }
}
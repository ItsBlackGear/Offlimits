package com.blackgear.offlimits.core.mixin.client;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.Iterator;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin implements ClientGamePacketListener {
    @Shadow private ClientLevel level;
    
    @ModifyConstant(
        method = "handleLevelChunk",
        constant = @Constant(intValue = 16)
    )
    private int off$handleLevelChunkMax(int original) {
        return Offlimits.INSTANCE.getMaxSection();
    }
    
    @ModifyConstant(
        method = "handleLevelChunk",
        constant = @Constant(intValue = 0)
    )
    private int off$handleLevelChunkMin(int original) {
        return Offlimits.INSTANCE.getMinSection();
    }
    
    @ModifyConstant(
        method = "handleForgetLevelChunk",
        constant = @Constant(intValue = 16)
    )
    private int off$handleForgetLevelChunkMax(int original) {
        return Offlimits.INSTANCE.getMaxSection();
    }
    
    @ModifyConstant(
        method = "handleForgetLevelChunk",
        constant = @Constant(intValue = 0)
    )
    private int off$handleForgetLevelChunkMin(int original) {
        return Offlimits.INSTANCE.getMinSection();
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    private void readSectionList(int i, int j, LevelLightEngine levelLightEngine, LightLayer lightLayer, int k, int l, Iterator<byte[]> iterator, boolean bl) {
        for(int m = 0; m < Offlimits.LIGHT.getLightSectionCount(); ++m) {
            int n = Offlimits.LIGHT.getMinLightSection() + m;
            boolean bl2 = (k & 1 << m) != 0;
            boolean bl3 = (l & 1 << m) != 0;
            if (bl2 || bl3) {
                levelLightEngine.queueSectionData(lightLayer, SectionPos.of(i, n, j), bl2 ? new DataLayer((byte[])((byte[])iterator.next()).clone()) : new DataLayer(), bl);
                this.level.setSectionDirtyWithNeighbors(i, n, j);
            }
        }
    }
}
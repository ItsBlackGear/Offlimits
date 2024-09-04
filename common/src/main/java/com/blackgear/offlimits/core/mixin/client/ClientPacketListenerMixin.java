package com.blackgear.offlimits.core.mixin.client;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin implements ClientGamePacketListener {
    @Shadow private ClientLevel level;
    
    @Shadow private RegistryAccess registryAccess;
    
    @Shadow private Minecraft minecraft;
    
    @Inject(
        method = "handleLevelChunk",
        at = @At("HEAD"),
        cancellable = true
    )
    private void off$handleLevelChunk(ClientboundLevelChunkPacket packet, CallbackInfo ci) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft);
        int i = packet.getX();
        int j = packet.getZ();
        ChunkBiomeContainer biomecontainer = packet.getBiomes() == null ? null : new ChunkBiomeContainer(this.registryAccess.registryOrThrow(Registry.BIOME_REGISTRY), packet.getBiomes());
        LevelChunk chunk = this.level.getChunkSource().replaceWithPacketData(i, j, biomecontainer, packet.getReadBuffer(), packet.getHeightmaps(), packet.getAvailableSections(), packet.isFullChunk());
        if (chunk != null && packet.isFullChunk()) {
            this.level.reAddEntitiesToChunk(chunk);
        }
        
        for(int k = Offlimits.INSTANCE.getMinSection(); k < Offlimits.INSTANCE.getMaxSection(); ++k) {
            this.level.setSectionDirtyWithNeighbors(i, k, j);
        }
        
        for(CompoundTag compoundnbt : packet.getBlockEntitiesTags()) {
            BlockPos blockpos = new BlockPos(compoundnbt.getInt("x"), compoundnbt.getInt("y"), compoundnbt.getInt("z"));
            BlockEntity tileentity = this.level.getBlockEntity(blockpos);
            if (tileentity != null) {
                tileentity.load(this.level.getBlockState(blockpos), compoundnbt);
            }
        }
    }
    
    @Inject(
        method = "handleForgetLevelChunk",
        at = @At("HEAD"),
        cancellable = true
    )
    private void off$handleForgetLevelChunk(ClientboundForgetLevelChunkPacket packet, CallbackInfo ci) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft);
        int i = packet.getX();
        int j = packet.getZ();
        ClientChunkCache clientchunkprovider = this.level.getChunkSource();
        clientchunkprovider.drop(i, j);
        LevelLightEngine worldlightmanager = clientchunkprovider.getLightEngine();
        
        for(int k = Offlimits.INSTANCE.getMinSection(); k < Offlimits.INSTANCE.getMaxSection(); ++k) {
            this.level.setSectionDirtyWithNeighbors(i, k, j);
            worldlightmanager.updateSectionStatus(SectionPos.of(i, k, j), true);
        }
        
        worldlightmanager.enableLightSources(new ChunkPos(i, j), false);
        ci.cancel();
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
                levelLightEngine.queueSectionData(lightLayer, SectionPos.of(i, n, j), bl2 ? new DataLayer(iterator.next().clone()) : new DataLayer(), bl);
                this.level.setSectionDirtyWithNeighbors(i, n, j);
            }
        }
    }
}
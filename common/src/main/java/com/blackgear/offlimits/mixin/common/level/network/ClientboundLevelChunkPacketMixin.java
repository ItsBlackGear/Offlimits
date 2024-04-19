package com.blackgear.offlimits.mixin.common.level.network;

import net.minecraft.network.protocol.game.ClientboundLevelChunkPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ClientboundLevelChunkPacket.class)
public class ClientboundLevelChunkPacketMixin {
    @ModifyConstant(
        method = "<init>(Lnet/minecraft/world/level/chunk/LevelChunk;I)V",
        constant = @Constant(intValue = 0x0000_FFFF)
    )
    private int offlimits$init(int original) {
        return 0xFFFF_FFFF;
    }
}
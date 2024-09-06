package com.blackgear.offlimits.core.mixin.client;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ClientboundLightUpdatePacket.class)
public class ClientboundLightUpdatePacketMixin {
    @ModifyConstant(
        method = {
            "<init>(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/lighting/LevelLightEngine;Z)V",
            "<init>(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/lighting/LevelLightEngine;IIZ)V",
            "read"
        },
        constant = @Constant(intValue = 18)
    )
    private int off$updateLightSectionsCount(int constant) {
        return Offlimits.LIGHT.getLightSectionCount();
    }
    
    @ModifyConstant(
        method = {
            "<init>(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/lighting/LevelLightEngine;Z)V",
            "<init>(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/lighting/LevelLightEngine;IIZ)V"
        },
        constant = @Constant(intValue = -1)
    )
    private int off$initMinLightSections(int constant) {
        return Offlimits.LIGHT.getMinLightSection();
    }
}
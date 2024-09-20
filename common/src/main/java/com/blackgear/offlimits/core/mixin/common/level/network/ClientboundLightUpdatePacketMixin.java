package com.blackgear.offlimits.core.mixin.common.level.network;

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
    private int offlimits$updateTotalLightSections(int constant) {
        return Offlimits.LIGHT.getLightSectionCount();
    }
    
    @ModifyConstant(
        method = {
            "<init>(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/lighting/LevelLightEngine;Z)V"
        },
        constant = @Constant(intValue = -1)
    )
    private int offlimits$updateMinLightSectionsFromChunkMap(int constant) {
        return Offlimits.LIGHT.getMinLightSection();
    }
    
    @ModifyConstant(
        method = "<init>(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/lighting/LevelLightEngine;IIZ)V",
        constant = {
            @Constant(intValue = -1, ordinal = 0),
            @Constant(intValue = -1, ordinal = 2)
        }
    )
    private int offlimits$updateMinLightSectionsFromChunkHolder(int constant) {
        return Offlimits.LIGHT.getMinLightSection();
    }
}
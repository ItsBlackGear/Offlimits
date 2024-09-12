package com.blackgear.offlimits.core.mixin.client;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin implements ClientGamePacketListener {
    @ModifyConstant(
        method = {
            "handleLevelChunk",
            "handleForgetLevelChunk"
        },
        constant = @Constant(intValue = 16)
    )
    private int offlimits$updateMaxSections(int constant) {
        return Offlimits.LEVEL.getMaxSection();
    }

    @ModifyConstant(
        method = {
            "handleLevelChunk",
            "handleForgetLevelChunk"
        },
        constant = @Constant(intValue = 0)
    )
    private int offlimits$updateMinSections(int constant) {
        return Offlimits.LEVEL.getMinSection();
    }
    
    @ModifyConstant(
        method = "readSectionList",
        constant = @Constant(intValue = 18)
    )
    private int offlimits$readLightSections(int constant) {
        return Offlimits.LIGHT.getLightSectionCount();
    }

    @ModifyConstant(
        method = "readSectionList",
        constant = @Constant(intValue = -1)
    )
    private int offlimits$readMinLightSections(int constant) {
        return Offlimits.LIGHT.getMinLightSection();
    }
}
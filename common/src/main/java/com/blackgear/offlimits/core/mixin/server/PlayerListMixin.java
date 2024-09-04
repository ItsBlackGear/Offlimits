package com.blackgear.offlimits.core.mixin.server;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @ModifyConstant(
        method = "respawn",
        constant = @Constant(doubleValue = 256.0D)
    )
    private double offlimits$respawn(double original) {
        return Offlimits.LEVEL.getMaxBuildHeight();
    }
}
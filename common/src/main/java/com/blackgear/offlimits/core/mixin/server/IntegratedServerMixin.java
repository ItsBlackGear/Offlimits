package com.blackgear.offlimits.core.mixin.server;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {
    @ModifyConstant(
        method = "<init>",
        constant = @Constant(intValue = 256)
    )
    private int platform$init(int original) {
        return Offlimits.INSTANCE.getMaxBuildHeight();
    }
}
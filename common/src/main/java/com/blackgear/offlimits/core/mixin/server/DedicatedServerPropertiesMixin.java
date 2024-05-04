package com.blackgear.offlimits.core.mixin.server;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(DedicatedServerProperties.class)
public class DedicatedServerPropertiesMixin {
    @ModifyConstant(
        method = "<init>",
        constant = @Constant(intValue = 256)
    )
    private int platform$init(int original) {
        return Offlimits.INSTANCE.getMaxBuildHeight();
    }
}
package com.blackgear.offlimits.core.mixin.common.access;

import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ConfiguredWorldCarver.class)
public interface ConfiguredWorldCarverAccessor {
    @Accessor
    <WC extends CarverConfiguration> WorldCarver<WC> getWorldCarver();
}

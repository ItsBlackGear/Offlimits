package com.blackgear.offlimits.forge;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.core.config.forge.OfflimitsForgeConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(Offlimits.MOD_ID)
public class OfflimitsForge {
    public OfflimitsForge() {
        Offlimits.init();
        
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, OfflimitsForgeConfig.SPEC, "offlimits.toml");
    }
}
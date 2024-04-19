package com.blackgear.offlimits.forge;

import com.blackgear.offlimits.Offlimits;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Offlimits.MOD_ID)
public class OfflimitsForge {
    public OfflimitsForge() {
        Offlimits.init();
    }
}
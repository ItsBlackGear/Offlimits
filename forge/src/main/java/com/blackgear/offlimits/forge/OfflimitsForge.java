package com.blackgear.offlimits.forge;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.core.config.OfflimitsConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(Offlimits.MOD_ID)
public class OfflimitsForge {
    public OfflimitsForge() {
        Offlimits.init();
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> (client, screen) -> {
            return AutoConfig.getConfigScreen(OfflimitsConfig.class, screen).get();
        });
    }
}
package com.blackgear.offlimits.core.config.fabric;

import com.blackgear.offlimits.Offlimits;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = Offlimits.MOD_ID)
public class OfflimitsFabricConfig implements ConfigData {
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 256, max = 512)
    public int maxBuildHeight = 320;
}
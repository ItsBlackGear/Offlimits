package com.blackgear.offlimits.core.config.fabric;

import com.blackgear.offlimits.core.config.OfflimitsConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;

public class OfflimitsModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfig.getConfigScreen(OfflimitsConfig.class, parent).get();
    }
}
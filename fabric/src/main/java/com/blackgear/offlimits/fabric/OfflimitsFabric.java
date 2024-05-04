package com.blackgear.offlimits.fabric;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.core.config.fabric.OfflimitsFabricConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;

public class OfflimitsFabric implements ModInitializer {
    public static final OfflimitsFabricConfig CONFIG = AutoConfig.register(OfflimitsFabricConfig.class, GsonConfigSerializer::new).getConfig();
    
    @Override
    public void onInitialize() {
        Offlimits.init();
    }
}
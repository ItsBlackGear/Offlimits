package com.blackgear.offlimits.fabric;

import com.blackgear.offlimits.Offlimits;
import net.fabricmc.api.ModInitializer;

public class OfflimitsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Offlimits.bootstrap();
    }
}
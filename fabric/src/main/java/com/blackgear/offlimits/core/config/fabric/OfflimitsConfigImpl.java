package com.blackgear.offlimits.core.config.fabric;

import com.blackgear.offlimits.fabric.OfflimitsFabric;

public class OfflimitsConfigImpl {
    public static int getMaxBuildHeight() {
        return OfflimitsFabric.CONFIG.maxBuildHeight;
    }
}
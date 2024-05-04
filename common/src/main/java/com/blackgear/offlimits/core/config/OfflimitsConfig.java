package com.blackgear.offlimits.core.config;

import dev.architectury.injectables.annotations.ExpectPlatform;

public class OfflimitsConfig {
    public static final int MAX_BUILD_HEIGHT = getMaxBuildHeight();
    
    @ExpectPlatform
    public static int getMaxBuildHeight() {
        throw new AssertionError();
    }
}
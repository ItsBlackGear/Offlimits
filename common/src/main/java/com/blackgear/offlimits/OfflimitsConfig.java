package com.blackgear.offlimits;

import com.blackgear.platform.core.util.config.ConfigBuilder;

public class OfflimitsConfig {
    public final ConfigBuilder.ConfigValue<Integer> maxBuildHeight;
    
    public OfflimitsConfig(ConfigBuilder builder) {
        this.maxBuildHeight = builder.comment("The maximum build height for the world.").defineInRange("maxBuildHeight", 320, 256, 512);
    }
}
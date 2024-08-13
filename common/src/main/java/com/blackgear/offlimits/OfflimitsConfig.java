package com.blackgear.offlimits;

import com.blackgear.platform.core.util.config.ConfigBuilder;

public class OfflimitsConfig {
    public final ConfigBuilder.ConfigValue<Integer> maxBuildHeight;
    
    public final ConfigBuilder.ConfigValue<Boolean> allowTerrainModifications;
    public final ConfigBuilder.ConfigValue<Boolean> areNoiseCavesEnabled;
    public final ConfigBuilder.ConfigValue<Boolean> areAquifersEnabled;
    public final ConfigBuilder.ConfigValue<Boolean> areNoodleCavesEnabled;
    
    public OfflimitsConfig(ConfigBuilder builder) {
        builder.push("height");
            this.maxBuildHeight = builder.comment("The maximum build height for the world.").defineInRange("maxBuildHeight", 320, 256, 512);
        builder.pop();
        
        builder.push("terrain");
            this.allowTerrainModifications = builder.comment("Allows the terrain to support the new underground features").define("allowTerrainModifications", true);
            this.areNoiseCavesEnabled = builder.comment("[!] Requires terrain modifications to be enabled").define("areNoiseCavesEnabled", true);
            this.areAquifersEnabled = builder.comment("[!] Requires terrain modifications to be enabled").define("areAquifersEnabled", true);
            this.areNoodleCavesEnabled = builder.comment("[!] Requires terrain modifications to be enabled").define("areNoodleCavesEnabled", true);
        builder.pop();
    }
}
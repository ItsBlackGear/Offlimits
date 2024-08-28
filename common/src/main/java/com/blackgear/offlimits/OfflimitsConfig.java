package com.blackgear.offlimits;

import com.blackgear.platform.core.util.config.ConfigBuilder;

public class OfflimitsConfig {
    public final ConfigBuilder.ConfigValue<Integer> maxBuildHeight;
    public final ConfigBuilder.ConfigValue<Integer> minBuildHeight;
    
    public final ConfigBuilder.ConfigValue<Integer> worldGenMinY;
    public final ConfigBuilder.ConfigValue<Boolean> allowTerrainModifications;
    
    public final ConfigBuilder.ConfigValue<Boolean> areNoiseCavesEnabled;
    public final ConfigBuilder.ConfigValue<Boolean> areAquifersEnabled;
    public final ConfigBuilder.ConfigValue<Boolean> areNoodleCavesEnabled;
    
    public OfflimitsConfig(ConfigBuilder builder) {
        builder.push("height");
            this.maxBuildHeight = builder.comment("The maximum build height for the world.").defineInRange("maxBuildHeight", 320, 256, 512);
            this.minBuildHeight = builder.comment("The minimum build height for the world.").defineInRange("minBuildHeight", -64, -128, 0);
        builder.pop();
        
        builder.push("terrain");
            this.worldGenMinY = builder.comment("The minimum Y level for world generation.\n[!] This value cannot be greater than minBuildHeight!").defineInRange("worldGenMinY", -64, -128, 0);
            this.allowTerrainModifications = builder.comment("Allows the terrain to support the new underground features.").define("allowTerrainModifications", true);
        builder.pop();
        
        builder.push("modifications");
            this.areNoiseCavesEnabled = builder.comment("Toggle noise cave generation.\n[!] Requires terrain modifications to be enabled").define("areNoiseCavesEnabled", true);
            this.areAquifersEnabled = builder.comment("Toggle aquifer generation.\n[!] Requires terrain modifications to be enabled").define("areAquifersEnabled", true);
            this.areNoodleCavesEnabled = builder.comment("Toggle noodle cave generation.\n[!] Requires terrain modifications to be enabled").define("areNoodleCavesEnabled", true);
        builder.pop();
    }
}
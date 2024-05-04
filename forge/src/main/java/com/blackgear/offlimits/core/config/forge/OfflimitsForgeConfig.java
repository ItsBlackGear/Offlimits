package com.blackgear.offlimits.core.config.forge;

import net.minecraftforge.common.ForgeConfigSpec;

public class OfflimitsForgeConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    public static final ForgeConfigSpec.ConfigValue<Integer> MAX_BUILD_HEIGHT;
    
    static {
        BUILDER.push("height limit");
        MAX_BUILD_HEIGHT = BUILDER.comment("The maximum build height for the world. Higher values may cause performance issues. Restart to apply changes.").defineInRange("maxBuildHeight", 320, 256, 512);
        BUILDER.pop();
        
        SPEC = BUILDER.build();
    
    }
    
//    public final ForgeConfigSpec.ConfigValue<Integer> maxBuildHeight;
//
//    public OfflimitsForgeConfig(ForgeConfigSpec.Builder builder) {
//        builder.push("height limit");
//        this.maxBuildHeight = builder.comment("The maximum build height for the world. Higher values may cause performance issues. Restart to apply changes.").defineInRange("maxBuildHeight", 320, 256, 512);
//        builder.pop();
//
//        builder.build();
//    }
}
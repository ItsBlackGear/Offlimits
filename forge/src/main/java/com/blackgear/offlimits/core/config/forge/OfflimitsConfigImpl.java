package com.blackgear.offlimits.core.config.forge;

import com.blackgear.offlimits.Offlimits;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.Pair;

@Mod.EventBusSubscriber(modid = Offlimits.MOD_ID)
public class OfflimitsConfigImpl {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final OfflimitsConfigImpl.Common COMMON;
    
    public static int getMaxBuildHeight() {
        return COMMON.maxBuildHeight.get();
    }
    
    public static class Common {
        public final ForgeConfigSpec.ConfigValue<Integer> maxBuildHeight;
        
        protected Common(ForgeConfigSpec.Builder builder) {
            builder.push("spawns");
            this.maxBuildHeight = builder.comment("The maximum build height for the world. Higher values may cause performance issues. Restart to apply changes.").defineInRange("maxBuildHeight", 320, 256, 512);
            builder.pop();
        }
    }
    
    static {
        final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }
}
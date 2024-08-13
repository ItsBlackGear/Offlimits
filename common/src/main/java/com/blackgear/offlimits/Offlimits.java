package com.blackgear.offlimits;

import com.blackgear.offlimits.common.utils.HeightLimitAccess;
import com.blackgear.offlimits.common.utils.LightLimitAccess;
import com.blackgear.platform.core.Environment;
import com.blackgear.platform.core.util.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Offlimits {
	public static final String MOD_ID = "offlimits";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
	public static final OfflimitsConfig CONFIG = Environment.registerUnsafeConfig(MOD_ID, ModConfig.Type.COMMON, OfflimitsConfig::new);
	public static final HeightLimitAccess INSTANCE = HeightLimitAccess.of(CONFIG.maxBuildHeight.get(), 0);
	public static final LightLimitAccess LIGHT = new LightLimitAccess(INSTANCE);
	
	public static void bootstrap() {
	}
}
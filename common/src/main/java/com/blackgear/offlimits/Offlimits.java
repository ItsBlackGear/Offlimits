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
	public static final HeightLimitAccess LEVEL = HeightLimitAccess.of(CONFIG.maxBuildHeight.get(), CONFIG.minBuildHeight.get());
	public static final LightLimitAccess LIGHT = new LightLimitAccess(LEVEL);
	
	public static void bootstrap() {
		validateHeightLevels();
	}
	
	private static void validateHeightLevels() {
		if (LEVEL.getHeight() % 16 != 0) {
			throw new IllegalStateException("height bas to be a multiple of 16, but was " + LEVEL.getHeight());
		} else if (CONFIG.worldGenMinY.get() % 16 != 0) {
			throw new IllegalStateException("min_y has to be a multiple of 16, but was " + CONFIG.worldGenMinY.get());
		} else if (LEVEL.getMinBuildHeight() % 16 != 0) {
			throw new IllegalStateException("min_build_height has to be a multiple of 16, but was " + LEVEL.getMinBuildHeight());
		} else if (LEVEL.getMaxBuildHeight() % 16 != 0) {
			throw new IllegalStateException("max_build_height has to be a multiple of 16, but was " + LEVEL.getMaxBuildHeight());
		}
		
		LOGGER.info("Height levels have been validated successfully.");
	}
}
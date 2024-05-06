package com.blackgear.offlimits;

import com.blackgear.offlimits.common.HeightLimitAccess;
import com.blackgear.offlimits.common.LightLimitAccess;
import com.blackgear.offlimits.core.config.OfflimitsConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Offlimits {
	public static final String MOD_ID = "offlimits";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
	public static final OfflimitsConfig CONFIG = AutoConfig.register(OfflimitsConfig.class, GsonConfigSerializer::new).getConfig();
	public static final HeightLimitAccess INSTANCE = HeightLimitAccess.of(CONFIG.maxBuildHeight, 0);
	public static final LightLimitAccess LIGHT = new LightLimitAccess(INSTANCE);
	
	public static void init() {
	}
}
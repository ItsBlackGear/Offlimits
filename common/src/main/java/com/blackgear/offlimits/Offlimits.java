package com.blackgear.offlimits;

import com.blackgear.offlimits.common.HeightLimitAccess;
import com.blackgear.offlimits.common.LightLimitAccess;
import com.blackgear.offlimits.core.config.OfflimitsConfig;

public class Offlimits {
	public static final String MOD_ID = "offlimits";
	public static final HeightLimitAccess INSTANCE = HeightLimitAccess.of(OfflimitsConfig.MAX_BUILD_HEIGHT, 0);
	public static final LightLimitAccess LIGHT = new LightLimitAccess(INSTANCE);
	
	public static void init() {}
}
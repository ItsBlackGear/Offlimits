package com.blackgear.offlimits;

import com.blackgear.offlimits.common.HeightLimitAccess;
import com.blackgear.offlimits.common.LightLimitAccess;

public class Offlimits {
	public static final String MOD_ID = "offlimits";

	public static final HeightLimitAccess INSTANCE = HeightLimitAccess.of(320, 0);
	public static final LightLimitAccess LIGHT = new LightLimitAccess() {};
	
	public static void init() {
	}
}
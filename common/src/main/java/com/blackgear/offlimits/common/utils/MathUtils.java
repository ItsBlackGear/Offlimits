package com.blackgear.offlimits.common.utils;

import net.minecraft.util.Mth;

public class MathUtils {
    /**
     * Maps a value from one range to another and clamps it within the new range.
     *
     * @param value The value to be mapped.
     * @param prevStart The start of the original range.
     * @param prevEnd The end of the original range.
     * @param start The start of the new range.
     * @param end The end of the new range.
     * @return The mapped and clamped value.
     */
    public static double clampedMap(double value, double prevStart, double prevEnd, double start, double end) {
        return Mth.clampedLerp(start, end, Mth.inverseLerp(value, prevStart, prevEnd));
    }
    
    /**
     * Maps a value from one range to another.
     *
     * @param value The value to be mapped.
     * @param prevStart The start of the original range.
     * @param prevEnd The end of the original range.
     * @param start The start of the new range.
     * @param end The end of the new range.
     * @return The mapped value.
     */
    public static double map(double value, double prevStart, double prevEnd, double start, double end) {
        return Mth.lerp(Mth.inverseLerp(value, prevStart, prevEnd), start, end);
    }
}
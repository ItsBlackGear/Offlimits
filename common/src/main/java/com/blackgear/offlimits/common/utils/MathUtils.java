package com.blackgear.offlimits.common.utils;

import net.minecraft.util.Mth;

public class MathUtils {
    private static final int PACKED_X_LENGTH = 1 + Mth.log2(Mth.smallestEncompassingPowerOfTwo(30000000));
    private static final int PACKED_Z_LENGTH = PACKED_X_LENGTH;
    public static final int PACKED_Y_LENGTH = 64 - PACKED_X_LENGTH - PACKED_Z_LENGTH;
    public static final int BITS_FOR_Y = PACKED_Y_LENGTH;
    public static final int Y_SIZE = (1 << BITS_FOR_Y) - 32;
    public static final int MAX_Y = (Y_SIZE >> 1) - 1;
    public static final int MIN_Y = MAX_Y - Y_SIZE + 1;
    public static final int WAY_BELOW_MIN_Y = MIN_Y << 4;
    
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
    
    public static int quantize(double value, int factor) {
        return Mth.floor(value / (double)factor) * factor;
    }
}
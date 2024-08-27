package com.blackgear.offlimits.common.utils;

import net.minecraft.world.level.levelgen.WorldgenRandom;

public class SimpleRandom extends WorldgenRandom {
    private static final int MODULUS_BITS = 48;
    private static final long MODULUS_MASK = 281474976710655L;
    private static final long MULTIPLIER = 25214903917L;
    private static final long INCREMENT = 11L;
    private static final float FLOAT_MULTIPLIER = 5.9604645E-8F;
    private static final double DOUBLE_MULTIPLIER = 1.110223E-16F;
    private long seed;
    private double nextNextGaussian;
    private boolean haveNextNextGaussian;
    
    public SimpleRandom(long l) {
        this.setSeed(l);
    }
    
    @Override
    public void setSeed(long l) {
        this.seed = (l ^ 25214903917L) & 281474976710655L;
    }
    
    public int next(int i) {
        long l = this.seed;
        long m = l * 25214903917L + 11L & 281474976710655L;
        return (int)(m >> 48 - i);
    }
    
    @Override
    public int nextInt() {
        return this.next(32);
    }
    
    @Override
    public int nextInt(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException("Bound must be positive");
        } else if ((i & i - 1) == 0) {
            return (int)((long)i * (long)this.next(31) >> 31);
        } else {
            int j;
            int k;
            do {
                j = this.next(31);
                k = j % i;
            } while(j - k + (i - 1) < 0);
            
            return k;
        }
    }
    
    @Override
    public long nextLong() {
        int i = this.next(32);
        int j = this.next(32);
        long l = (long)i << 32;
        return l + (long)j;
    }
    
    @Override
    public boolean nextBoolean() {
        return this.next(1) != 0;
    }
    
    @Override
    public float nextFloat() {
        return (float)this.next(24) * 5.9604645E-8F;
    }
    
    @Override
    public double nextDouble() {
        int i = this.next(26);
        int j = this.next(27);
        long l = ((long)i << 27) + (long)j;
        return (double)l * 1.110223E-16F;
    }
    
    @Override
    public double nextGaussian() {
        if (this.haveNextNextGaussian) {
            this.haveNextNextGaussian = false;
            return this.nextNextGaussian;
        } else {
            double d;
            double e;
            double f;
            do {
                d = 2.0 * this.nextDouble() - 1.0;
                e = 2.0 * this.nextDouble() - 1.0;
                f = (d * d) + (e * e);
            } while(f >= 1.0 || f == 0.0);
            
            double g = Math.sqrt(-2.0 * Math.log(f) / f);
            this.nextNextGaussian = e * g;
            this.haveNextNextGaussian = true;
            return d * g;
        }
    }
}
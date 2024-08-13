package com.blackgear.offlimits.common.utils;

import net.minecraft.core.SectionPos;

public interface HeightLimitAccess {
    int getHeight();
    
    int getMinBuildHeight();
    
    default int getMaxBuildHeight() {
        return this.getMinBuildHeight() + this.getHeight();
    }
    
    default int getSectionsCount() {
        return this.getMaxSection() - this.getMinSection();
    }
    
    default int getMinSection() {
        return SectionPos.blockToSectionCoord(this.getMinBuildHeight());
    }
    
    default int getMaxSection() {
        return SectionPos.blockToSectionCoord(this.getMaxBuildHeight() - 1) + 1;
    }
    
    default int getSectionIndex(int y) {
        return this.getSectionIndexFromSectionY(SectionPos.blockToSectionCoord(y));
    }
    
    default int getSectionIndexFromSectionY(int y) {
        return y - this.getMinSection();
    }
    
    default int getSectionYFromSectionIndex(int index) {
        return index + this.getMinSection();
    }
    
    static HeightLimitAccess of(int height, int minBuildHeight) {
        return new HeightLimitAccess() {
            @Override
            public int getHeight() {
                return height;
            }
            
            @Override
            public int getMinBuildHeight() {
                return minBuildHeight;
            }
        };
    }
}
package com.blackgear.offlimits.common.level.noise;

public interface NoiseGeneratorSettingsExtension {
    boolean aquifersEnabled();
    
    void setAquifersEnabled(boolean enabled);
    
    boolean noiseCavesEnabled();
    
    void setNoiseCavesEnabled(boolean enabled);
    
    boolean noodleCavesEnabled();
    
    void setNoodleCavesEnabled(boolean enabled);
    
    boolean deepslateEnabled();
    
    void setDeepslateEnabled(boolean enabled);
    
    boolean oreVeinsEnabled();
    
    void setOreVeinsEnabled(boolean enabled);
}
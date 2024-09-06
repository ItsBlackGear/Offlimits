package com.blackgear.offlimits.core.mixin.common.level;

import com.blackgear.offlimits.common.level.noise.NoiseSettingsExtension;
import net.minecraft.world.level.levelgen.NoiseSettings;
import org.spongepowered.asm.mixin.*;

@Mixin(NoiseSettings.class)
public class NoiseSettingsMixin implements NoiseSettingsExtension {
    @Mutable @Shadow @Final private int height;
    @Unique private int minY = 0;
    
    @Override
    public int minY() {
        return this.minY;
    }
    
    @Override
    public void setMinY(int minY) {
        this.minY = minY;
    }
    
    @Override
    public void setHeight(int height) {
        this.height = height;
    }
}
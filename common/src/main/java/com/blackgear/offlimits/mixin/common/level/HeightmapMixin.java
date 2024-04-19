package com.blackgear.offlimits.mixin.common.level;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.util.BitStorage;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Heightmap.class)
public class HeightmapMixin {
    @Redirect(
        method = "<init>",
        at = @At(
            value = "NEW",
            target = "(II)Lnet/minecraft/util/BitStorage;"
        )
    )
    public BitStorage offlimits$init(int bits, int size) {
        int i = Mth.ceillog2(Offlimits.INSTANCE.getHeight() + 1);
        return new BitStorage(i, 256);
    }
}
package com.blackgear.offlimits.core.fabric.mixin.compat.sodium;

//import com.blackgear.offlimits.Offlimits;
//import me.jellysquid.mods.sodium.client.render.chunk.cull.graph.ChunkGraphCuller;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Pseudo;
//import org.spongepowered.asm.mixin.injection.Constant;
//import org.spongepowered.asm.mixin.injection.ModifyConstant;

//@Pseudo @Mixin(ChunkGraphCuller.class)
//public class ChunkGraphCullerMixin {
//    @ModifyConstant(
//        method = "initSearch",
//        constant = @Constant(intValue = 15)
//    )
//    private int offlimits$updateMaxSections(int constant) {
//        return Offlimits.LEVEL.getMaxSection() - 1;
//    }
//
//    @ModifyConstant(
//        method = "initSearch",
//        constant = @Constant(intValue = 0, ordinal = 1)
//    )
//    private int offlimits$updateMinSections(int constant) {
//        return Offlimits.LEVEL.getMinSection();
//    }
//}
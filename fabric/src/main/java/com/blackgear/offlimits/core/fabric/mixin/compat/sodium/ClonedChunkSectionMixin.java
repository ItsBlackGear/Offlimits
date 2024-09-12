//package com.blackgear.offlimits.core.fabric.mixin.compat.sodium;
//
//import com.blackgear.offlimits.Offlimits;
//import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSection;
//import net.minecraft.core.SectionPos;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.chunk.ChunkAccess;
//import net.minecraft.world.level.chunk.LevelChunkSection;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Pseudo;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Pseudo @Mixin(ClonedChunkSection.class)
//public class ClonedChunkSectionMixin {
//    @Inject(
//        method = "getChunkSection",
//        at = @At("HEAD"),
//        remap = false,
//        cancellable = true
//    )
//    private static void offlimits$getChunkSections(ChunkAccess chunk, SectionPos pos, CallbackInfoReturnable<LevelChunkSection> cir) {
//        LevelChunkSection section = null;
//
//        if (!Level.isOutsideBuildHeight(SectionPos.sectionToBlockCoord(pos.getY()))) {
//            section = chunk.getSections()[Offlimits.LEVEL.getSectionIndexFromSectionY(pos.getY())];
//        }
//
//        cir.setReturnValue(section);
//    }
//}
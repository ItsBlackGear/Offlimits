package com.blackgear.offlimits.core.fabric.mixin.client;

import com.blackgear.offlimits.Offlimits;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.network.Connection;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Locale;

@Mixin(DebugScreenOverlay.class)
public class DebugScreenOverlayMixin {
    @Shadow @Final private Minecraft minecraft;
    
    @Inject(
        method = "getGameInformation",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;getY()I",
            ordinal = 4
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    @SuppressWarnings("InvalidInjectorMethodSignature")
    private void off$provideBiomeInfo(
        CallbackInfoReturnable<List<String>> cir,
        String string,
        BlockPos pos,
        Entity entity,
        Direction direction,
        String string2,
        Level level,
        LongSet longSet,
        List<String> list,
        LevelChunk levelChunk,
        int i,
        int j,
        int k,
        LevelChunk chunk,
        StringBuilder stringBuilder
    ) {
        boolean isBelowMinHeight = pos.getY() >= Offlimits.LEVEL.getMinBuildHeight() && pos.getY() < 0;
        boolean isAboveMaxHeight = pos.getY() <= Offlimits.LEVEL.getMaxBuildHeight() && pos.getY() >= 256;
        if (isBelowMinHeight || isAboveMaxHeight) {
            list.add("Biome: " + this.minecraft.level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getKey(this.minecraft.level.getBiome(pos)));
            long inhabitedTime = 0L;
            float moonBrightness = 0.0F;
            
            if (chunk != null) {
                moonBrightness = level.getMoonBrightness();
                inhabitedTime = chunk.getInhabitedTime();
            }
            
            DifficultyInstance difficulty = new DifficultyInstance(level.getDifficulty(), level.getDayTime(), inhabitedTime, moonBrightness);
            list.add(
                String.format(
                    Locale.ROOT,
                    "Local Difficulty: %.2f // %.2f (Day %d)",
                    difficulty.getEffectiveDifficulty(),
                    difficulty.getSpecialMultiplier(),
                    this.minecraft.level.getDayTime() / 24000L
                )
            );
        }
    }
}
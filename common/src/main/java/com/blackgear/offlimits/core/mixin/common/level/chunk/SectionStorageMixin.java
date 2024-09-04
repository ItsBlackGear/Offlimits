package com.blackgear.offlimits.core.mixin.common.level.chunk;

import com.blackgear.offlimits.Offlimits;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.SharedConstants;
import net.minecraft.core.SectionPos;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Mixin(SectionStorage.class)
public abstract class SectionStorageMixin<R> {
    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final private LongLinkedOpenHashSet dirty;
    @Shadow @Final private Long2ObjectMap<Optional<R>> storage;
    @Shadow @Final private DataFixer fixerUpper;
    @Shadow @Final private DataFixTypes type;
    @Shadow @Final private Function<Runnable, Codec<R>> codec;

    @Shadow private static int getVersion(Dynamic<?> dynamic) { return 0; }
    @Shadow protected abstract void setDirty(long sectionPos);
    @Shadow protected abstract void onSectionLoad(long l);
    @Shadow protected abstract void writeColumn(ChunkPos chunkPos);

    @Inject(
        method = "readColumn(Lnet/minecraft/world/level/ChunkPos;Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private <T> void off$readColumn(ChunkPos chunkPos, DynamicOps<T> dynamicOps, @Nullable T object, CallbackInfo ci) {
        if (object == null) {
            for (int i = Offlimits.INSTANCE.getMinSection(); i < Offlimits.INSTANCE.getMaxSection(); ++i) {
                this.storage.put(SectionPos.of(chunkPos, i).asLong(), Optional.empty());
            }
        } else {
            Dynamic<T> dynamic = new Dynamic<>(dynamicOps, object);
            int j = getVersion(dynamic);
            int k = SharedConstants.getCurrentVersion().getWorldVersion();
            boolean flag = j != k;
            Dynamic<T> dynamic2 = this.fixerUpper.update(this.type.getType(), dynamic, j, k);
            OptionalDynamic<T> optionalDynamic = dynamic2.get("Sections");

            for(int l = Offlimits.INSTANCE.getMinSection(); l < Offlimits.INSTANCE.getMaxSection(); ++l) {
                long m = SectionPos.of(chunkPos, l).asLong();
                Optional<R> optional = optionalDynamic.get(Integer.toString(l))
                    .result()
                    .flatMap((dynamic_) -> this.codec.apply(() -> this.setDirty(m)).parse(dynamic_).resultOrPartial(LOGGER::error));
                this.storage.put(m, optional);
                optional.ifPresent((object_) -> {
                    this.onSectionLoad(m);
                    if (flag) {
                        this.setDirty(m);
                    }
                });
            }
        }

        ci.cancel();
    }

    @Inject(
        method = "writeColumn(Lnet/minecraft/world/level/ChunkPos;Lcom/mojang/serialization/DynamicOps;)Lcom/mojang/serialization/Dynamic;",
        at = @At("HEAD"),
        cancellable = true
    )
    private <T> void off$writeColumn(ChunkPos chunkPos, DynamicOps<T> dynamicOps, CallbackInfoReturnable<Dynamic<T>> cir) {
        Map<T, T> map = Maps.newHashMap();

        for(int i = Offlimits.INSTANCE.getMinSection(); i < Offlimits.INSTANCE.getMaxSection(); ++i) {
            long j = SectionPos.of(chunkPos, i).asLong();
            this.dirty.remove(j);
            Optional<R> optional = this.storage.get(j);

            if (optional != null && optional.isPresent()) {
                DataResult<T> dataresult = this.codec.apply(() -> this.setDirty(j)).encodeStart(dynamicOps, optional.get());
                String s = Integer.toString(i);
                dataresult.resultOrPartial(LOGGER::error).ifPresent((object) -> map.put(dynamicOps.createString(s), object));
            }
        }

        cir.setReturnValue(
            new Dynamic<>(
                dynamicOps,
                dynamicOps.createMap(
                    ImmutableMap.of(
                        dynamicOps.createString("Sections"),
                        dynamicOps.createMap(map),
                        dynamicOps.createString("DataVersion"),
                        dynamicOps.createInt(SharedConstants.getCurrentVersion().getWorldVersion())
                    )
                )
            )
        );
    }

    @Inject(
        method = "flush",
        at = @At("HEAD"),
        cancellable = true
    )
    private void off$flush(ChunkPos chunkPos, CallbackInfo ci) {
        if (!this.dirty.isEmpty()) {
            for(int i = Offlimits.INSTANCE.getMinSection(); i < Offlimits.INSTANCE.getMaxSection(); ++i) {
                long j = SectionPos.of(chunkPos, i).asLong();
                if (this.dirty.contains(j)) {
                    this.writeColumn(chunkPos);
                    return;
                }
            }
        }

        ci.cancel();
    }
}
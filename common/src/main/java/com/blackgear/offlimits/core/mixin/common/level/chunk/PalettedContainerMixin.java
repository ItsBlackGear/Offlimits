package com.blackgear.offlimits.core.mixin.common.level.chunk;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.IdMapper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.BitStorage;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.HashMapPalette;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PaletteResize;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.stream.Collectors;

@Mixin(PalettedContainer.class)
public abstract class PalettedContainerMixin<T> implements PaletteResize<T> {
    @Shadow protected BitStorage storage;
    @Shadow private Palette<T> palette;
    @Shadow private int bits;
    @Shadow @Final private Palette<T> globalPalette;
    @Shadow @Final private IdMapper<T> registry;
    @Shadow @Final private PaletteResize<T> dummyPaletteResize;
    @Shadow @Final private Function<CompoundTag, T> reader;
    @Shadow @Final private Function<T, CompoundTag> writer;
    @Shadow @Final private T defaultValue;
    
    @Shadow protected abstract void setBits(int bits);
    @Shadow protected abstract void set(int index, T state);
    @Shadow public abstract void acquire();
    @Shadow protected abstract T getAndSet(int index, T state);
    @Shadow public abstract void release();
    @Shadow private static int getIndex(int x, int y, int z) { throw new AssertionError(); }
    @Shadow public abstract T get(int x, int y, int z);
    @Shadow protected abstract T get(int index);
    
    @Unique private static final int SIZE = 4096;
    @Unique private static final int MIN_PALETTE_SIZE = 4;
    @Unique private final Semaphore off$lock = new Semaphore(1);

    @Inject(
        method = "acquire",
        at = @At("HEAD"),
        cancellable = true
    )
    public void off$acquire(CallbackInfo ci) {
        if (!this.off$lock.tryAcquire()) {
            String message = Thread.getAllStackTraces()
                .keySet()
                .stream()
                .filter(Objects::nonNull)
                .map(thread -> {
                    return thread.getName() + ": \n\tat " + Arrays.stream(thread.getStackTrace())
                        .map(Object::toString)
                        .collect(Collectors.joining("\n\tat "));
                })
                .collect(Collectors.joining("\n"));
            CrashReport report = new CrashReport("Accessing PalettedContainer from multiple threads", new IllegalStateException());
            CrashReportCategory category = report.addCategory("Thread dumps");
            category.setDetail("Thread dumps", message);
            
            throw new ReportedException(report);
        }
        
        ci.cancel();
    }
    
    @Inject(
        method = "release",
        at = @At("HEAD"),
        cancellable = true
    )
    public void off$release(CallbackInfo ci) {
        this.off$lock.release();
        ci.cancel();
    }
    
    @Inject(
        method = "onResize",
        at = @At("HEAD"),
        cancellable = true
    )
    public void off$onResize(int bits, T state, CallbackInfoReturnable<Integer> cir) {
        BitStorage storage = this.storage;
        Palette<T> palette = this.palette;
        this.setBits(bits);
        
        for (int index = 0; index < storage.getSize(); index++) {
            T currentState = palette.valueFor(storage.get(index));
            if (currentState != null) {
                this.set(index, currentState);
            }
        }
        
        cir.setReturnValue(this.palette.idFor(state));
    }
    
    @Inject(
        method = "getAndSet(IIILjava/lang/Object;)Ljava/lang/Object;",
        at = @At("HEAD"),
        cancellable = true
    )
    public void off$getAndSet(int x, int y, int z, T state, CallbackInfoReturnable<T> cir) {
        T object;
        
        try {
            this.acquire();
            object = this.getAndSet(getIndex(x, y, z), state);
        } finally {
            this.release();
        }
        
        cir.setReturnValue(object);
    }
    
    @Inject(
        method = "read(Lnet/minecraft/network/FriendlyByteBuf;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    public void off$read(FriendlyByteBuf buf, CallbackInfo ci) {
        try {
            this.acquire();
            int bits = buf.readByte();
            
            if (this.bits != bits) {
                this.setBits(bits);
            }
            
            this.palette.read(buf);
            buf.readLongArray(this.storage.getRaw());
        } finally {
            this.release();
        }
        
        ci.cancel();
    }
    
    @Inject(
        method = "write(Lnet/minecraft/network/FriendlyByteBuf;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    public void off$write(FriendlyByteBuf buf, CallbackInfo ci) {
        try {
            this.acquire();
            buf.writeByte(this.bits);
            this.palette.write(buf);
            buf.writeLongArray(this.storage.getRaw());
        } finally {
            this.release();
        }
        
        ci.cancel();
    }
    
    @Inject(
        method = "read(Lnet/minecraft/nbt/ListTag;[J)V",
        at = @At("HEAD"),
        cancellable = true
    )
    public void off$read(ListTag listTag, long[] data, CallbackInfo ci) {
        try {
            this.acquire();
            int bits = Math.max(MIN_PALETTE_SIZE, Mth.ceillog2(listTag.size()));
            if (bits != this.bits) {
                this.setBits(bits);
            }
            
            this.palette.read(listTag);
            int dataBits = data.length * 64 / SIZE;
            
            if (this.palette == this.globalPalette) {
                Palette<T> palette = new HashMapPalette<>(this.registry, bits, this.dummyPaletteResize, this.reader, this.writer);
                palette.read(listTag);
                BitStorage storage = new BitStorage(bits, 4096, data);
                
                for (int index = 0; index < SIZE; index++) {
                    this.storage.set(index, this.globalPalette.idFor(palette.valueFor(storage.get(index))));
                }
            } else if (dataBits == this.bits) {
                System.arraycopy(data, 0, this.storage.getRaw(), 0, data.length);
            } else {
                BitStorage storage = new BitStorage(dataBits, SIZE, data);
                
                for (int index = 0; index < SIZE; index++) {
                    this.storage.set(index, storage.get(index));
                }
            }
        } finally {
            this.release();
        }
        
        ci.cancel();
    }
    
    @Inject(
        method = "write(Lnet/minecraft/nbt/CompoundTag;Ljava/lang/String;Ljava/lang/String;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    public void off$write(CompoundTag compoundTag, String paletteName, String paletteDataName, CallbackInfo ci) {
        try {
            this.acquire();
            HashMapPalette<T> palette = new HashMapPalette<>(this.registry, this.bits, this.dummyPaletteResize, this.reader, this.writer);
            T value = this.defaultValue;
            int id = palette.idFor(this.defaultValue);
            int[] ids = new int[SIZE];
            
            for (int index = 0; index < SIZE; index++) {
                T currentValue = this.get(index);
                
                if (currentValue != value) {
                    value = currentValue;
                    id = palette.idFor(currentValue);
                }
                
                ids[index] = id;
            }
            
            ListTag listTag = new ListTag();
            palette.write(listTag);
            compoundTag.put(paletteName, listTag);
            int bits = Math.max(MIN_PALETTE_SIZE, Mth.ceillog2(listTag.size()));
            BitStorage storage = new BitStorage(bits, SIZE);
            
            for (int index = 0; index < ids.length; index++) {
                storage.set(index, ids[index]);
            }
            
            compoundTag.putLongArray(paletteDataName, storage.getRaw());
        } finally {
            this.release();
        }
        
        ci.cancel();
    }
}
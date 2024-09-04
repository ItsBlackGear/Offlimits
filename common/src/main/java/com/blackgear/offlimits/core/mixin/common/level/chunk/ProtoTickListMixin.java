package com.blackgear.offlimits.core.mixin.common.level.chunk;

import com.blackgear.offlimits.Offlimits;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.TickPriority;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.ProtoTickList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.function.Function;

@Mixin(ProtoTickList.class)
public class ProtoTickListMixin<T> {
    @Shadow @Final private ShortList[] toBeTicked;
    
    @Shadow @Final private ChunkPos chunkPos;
    
    @ModifyConstant(
        method = "<init>(Ljava/util/function/Predicate;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/nbt/ListTag;)V",
        constant = @Constant(intValue = 16)
    )
    private int init(int original) {
        return Offlimits.LEVEL.getSectionsCount();
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    public void copyOut(TickList<T> tickList, Function<BlockPos, T> function) {
        for(int i = 0; i < this.toBeTicked.length; ++i) {
            if (this.toBeTicked[i] != null) {
                for(Short short_ : this.toBeTicked[i]) {
                    BlockPos blockPos = ProtoChunk.unpackOffsetCoordinates(short_, Offlimits.LEVEL.getSectionYFromSectionIndex(i), this.chunkPos);
                    tickList.scheduleTick(blockPos, function.apply(blockPos), 0);
                }
                
                this.toBeTicked[i].clear();
            }
        }
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    public void scheduleTick(BlockPos blockPos, T object, int i, TickPriority tickPriority) {
        int j = Offlimits.LEVEL.getSectionIndex(blockPos.getY());
        if (j >= 0 && j < Offlimits.LEVEL.getSectionsCount()) {
            ChunkAccess.getOrCreateOffsetList(this.toBeTicked, j).add(ProtoChunk.packOffsetCoordinates(blockPos));
        }
    }
}
package com.blackgear.offlimits.core.mixin.common.level.portal;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalForcer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(PortalForcer.class)
public abstract class PortalForcerMixin {
    @Shadow @Final private ServerLevel level;
    
    @Shadow protected abstract boolean canHostFrame(BlockPos originalPos, BlockPos.MutableBlockPos offsetPos, Direction direction, int offsetScale);
    
    /**
     * @author BlackGear27
     * @reason Fix Nether Portal spawning // TODO: find a better way to do this.
     */
    @Overwrite
    public Optional<BlockUtil.FoundRectangle> createPortal(BlockPos pos, Direction.Axis axis) {
        Direction direction = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        double closestDistance = -1.0;
        BlockPos closestPos = null;
        double secondClosestDistance = -1.0;
        BlockPos secondClosestPos = null;
        WorldBorder border = this.level.getWorldBorder();
        int maxBuildHeight = Math.min(this.level.getMaxBuildHeight(), Offlimits.LEVEL.getMinBuildHeight() + this.level.getHeight() - 1);
        BlockPos.MutableBlockPos mutablePos = pos.mutable();
        
        for(BlockPos.MutableBlockPos spiralPos : BlockPos.spiralAround(pos, 16, Direction.EAST, Direction.SOUTH)) {
            int heightAtSpiralPos = Math.min(maxBuildHeight, this.level.getHeight(Heightmap.Types.MOTION_BLOCKING, spiralPos.getX(), spiralPos.getZ()));
            if (border.isWithinBounds(spiralPos) && border.isWithinBounds(spiralPos.move(direction, 1))) {
                spiralPos.move(direction.getOpposite(), 1);
                
                for(int currentHeight = heightAtSpiralPos; currentHeight >= Offlimits.LEVEL.getMinBuildHeight(); --currentHeight) {
                    spiralPos.setY(currentHeight);
                    if (this.level.isEmptyBlock(spiralPos)) {
                        int initialHeight = currentHeight;
                        
                        while(currentHeight > Offlimits.LEVEL.getMinBuildHeight() && this.level.isEmptyBlock(spiralPos.move(Direction.DOWN))) {
                            --currentHeight;
                        }
                        
                        if (currentHeight + 4 <= maxBuildHeight) {
                            int heightDifference = initialHeight - currentHeight;
                            if (heightDifference <= 0 || heightDifference >= 3) {
                                spiralPos.setY(currentHeight);
                                if (this.canHostFrame(spiralPos, mutablePos, direction, 0)) {
                                    double distanceToPos = pos.distSqr(spiralPos);
                                    if (this.canHostFrame(spiralPos, mutablePos, direction, -1)
                                        && this.canHostFrame(spiralPos, mutablePos, direction, 1)
                                        && (closestDistance == -1.0 || closestDistance > distanceToPos)) {
                                        closestDistance = distanceToPos;
                                        closestPos = spiralPos.immutable();
                                    }
                                    
                                    if (closestDistance == -1.0 && (secondClosestDistance == -1.0 || secondClosestDistance > distanceToPos)) {
                                        secondClosestDistance = distanceToPos;
                                        secondClosestPos = spiralPos.immutable();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (closestDistance == -1.0 && secondClosestDistance != -1.0) {
            closestPos = secondClosestPos;
            closestDistance = secondClosestDistance;
        }
        
        if (closestDistance == -1.0) {
            int minHeight = Math.max(Offlimits.LEVEL.getMinBuildHeight() + 1, 70);
            int maxHeight = maxBuildHeight - 9;
            if (maxHeight < minHeight) {
                return Optional.empty();
            }
            
            closestPos = new BlockPos(pos.getX(), Mth.clamp(pos.getY(), minHeight, this.level.getHeight() - 10), pos.getZ()).immutable();
            Direction clockwiseDirection = direction.getClockWise();
            if (!border.isWithinBounds(closestPos)) {
                return Optional.empty();
            }
            
            for(int xOffset = -1; xOffset < 2; ++xOffset) {
                for(int zOffset = 0; zOffset < 2; ++zOffset) {
                    for(int yOffset = -1; yOffset < 3; ++yOffset) {
                        BlockState currentBlockState = yOffset < 0 ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState();
                        mutablePos.setWithOffset(closestPos, zOffset * direction.getStepX() + xOffset * clockwiseDirection.getStepX(), yOffset, zOffset * direction.getStepZ() + xOffset * clockwiseDirection.getStepZ());
                        this.level.setBlockAndUpdate(mutablePos, currentBlockState);
                    }
                }
            }
        }
        
        for(int zOffset = -1; zOffset < 3; ++zOffset) {
            for(int yOffset = -1; yOffset < 4; ++yOffset) {
                if (zOffset == -1 || zOffset == 2 || yOffset == -1 || yOffset == 3) {
                    mutablePos.setWithOffset(closestPos, zOffset * direction.getStepX(), yOffset, zOffset * direction.getStepZ());
                    this.level.setBlock(mutablePos, Blocks.OBSIDIAN.defaultBlockState(), 3);
                }
            }
        }
        
        BlockState portalBlockState = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, axis);
        
        for(int zOffset = 0; zOffset < 2; ++zOffset) {
            for(int yOffset = 0; yOffset < 3; ++yOffset) {
                mutablePos.setWithOffset(closestPos, zOffset * direction.getStepX(), yOffset, zOffset * direction.getStepZ());
                this.level.setBlock(mutablePos, portalBlockState, 18);
            }
        }
        
        return Optional.of(new BlockUtil.FoundRectangle(closestPos.immutable(), 2, 3));
    }
}
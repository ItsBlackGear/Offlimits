package com.blackgear.offlimits.core.mixin.client;

import com.blackgear.offlimits.Offlimits;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.Set;

@Mixin(ViewArea.class)
interface ViewAreaAccessor {
    @Invoker
    ChunkRenderDispatcher.RenderChunk callGetRenderChunkAt(BlockPos pos);
}

@Mixin(LevelRenderer.RenderChunkInfo.class)
interface RenderChunkInfoAccessor {
    @Accessor
    ChunkRenderDispatcher.RenderChunk getChunk();
    
    @Accessor
    Direction getSourceDirection();
    
    @Accessor
    byte getDirections();
    
    @Accessor
    int getStep();
}

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow @Final private Minecraft minecraft;
    
    @Shadow private int lastViewDistance;
    
    @Shadow public abstract void allChanged();
    
    @Shadow private ClientLevel level;
    
    @Shadow private double lastCameraX;
    
    @Shadow private double lastCameraY;
    
    @Shadow private double lastCameraZ;
    
    @Shadow private int lastCameraChunkX;
    
    @Shadow private int lastCameraChunkY;
    
    @Shadow private int lastCameraChunkZ;
    
    @Shadow private ViewArea viewArea;
    
    @Shadow private ChunkRenderDispatcher chunkRenderDispatcher;
    
    @Shadow private boolean needsUpdate;
    
    @Shadow private Set<ChunkRenderDispatcher.RenderChunk> chunksToCompile;
    
    @Shadow private double prevCamX;
    
    @Shadow private double prevCamY;
    
    @Shadow private double prevCamZ;
    
    @Shadow private double prevCamRotX;
    
    @Shadow private double prevCamRotY;
    
    @Shadow @Final private ObjectList<LevelRenderer.RenderChunkInfo> renderChunks;
    
    @Shadow @Final public static Direction[] DIRECTIONS;
    
    @Shadow @Final private TextureManager textureManager;
    @Shadow @Final private static ResourceLocation FORCEFIELD_LOCATION;
    
    @Shadow protected abstract void vertex(BufferBuilder bufferBuilder, double d, double e, double f, double g, int i, double h, float j, float k);
    
    @Unique private final LevelRenderer instance = (LevelRenderer) (Object) this;
    
    
    @ModifyConstant(
        method = "<init>",
        constant = @Constant(intValue = 69696)
    )
    private int off$constructor(int constant) {
        return 4356 * Offlimits.LEVEL.getSectionsCount();
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    private void setupRender(Camera activeRenderInfo, Frustum camera, boolean debugCamera, int frameCount, boolean playerSpectator) {
        ViewAreaAccessor viewArea = (ViewAreaAccessor) this.viewArea;
        Vec3 vec3 = activeRenderInfo.getPosition();
        if (this.minecraft.options.renderDistance != this.lastViewDistance) {
            this.allChanged();
        }
        
        this.level.getProfiler().push("camera");
        double d = this.minecraft.player.getX() - this.lastCameraX;
        double e = this.minecraft.player.getY() - this.lastCameraY;
        double f = this.minecraft.player.getZ() - this.lastCameraZ;
        if (this.lastCameraChunkX != this.minecraft.player.xChunk
            || this.lastCameraChunkY != this.minecraft.player.yChunk
            || this.lastCameraChunkZ != this.minecraft.player.zChunk
            || d * d + e * e + f * f > 16.0) {
            this.lastCameraX = this.minecraft.player.getX();
            this.lastCameraY = this.minecraft.player.getY();
            this.lastCameraZ = this.minecraft.player.getZ();
            this.lastCameraChunkX = this.minecraft.player.xChunk;
            this.lastCameraChunkY = this.minecraft.player.yChunk;
            this.lastCameraChunkZ = this.minecraft.player.zChunk;
            this.viewArea.repositionCamera(this.minecraft.player.getX(), this.minecraft.player.getZ());
        }
        
        this.chunkRenderDispatcher.setCamera(vec3);
        this.level.getProfiler().popPush("cull");
        this.minecraft.getProfiler().popPush("culling");
        BlockPos blockPos = activeRenderInfo.getBlockPosition();
        ChunkRenderDispatcher.RenderChunk renderChunk = viewArea.callGetRenderChunkAt(blockPos);
        int i = 16;
        BlockPos blockPos2 = new BlockPos(Mth.floor(vec3.x / 16.0) * 16, Mth.floor(vec3.y / 16.0) * 16, Mth.floor(vec3.z / 16.0) * 16);
        float g = activeRenderInfo.getXRot();
        float h = activeRenderInfo.getYRot();
        this.needsUpdate = this.needsUpdate
            || !this.chunksToCompile.isEmpty()
            || vec3.x != this.prevCamX
            || vec3.y != this.prevCamY
            || vec3.z != this.prevCamZ
            || (double)g != this.prevCamRotX
            || (double)h != this.prevCamRotY;
        this.prevCamX = vec3.x;
        this.prevCamY = vec3.y;
        this.prevCamZ = vec3.z;
        this.prevCamRotX = g;
        this.prevCamRotY = h;
        this.minecraft.getProfiler().popPush("update");
        if (!debugCamera && this.needsUpdate) {
            this.needsUpdate = false;
            this.renderChunks.clear();
            Queue<LevelRenderer.RenderChunkInfo> queue = Queues.newArrayDeque();
            Entity.setViewScale(Mth.clamp((double)this.minecraft.options.renderDistance / 8.0, 1.0, 2.5) * (double)this.minecraft.options.entityDistanceScaling);
            boolean bl = this.minecraft.smartCull;
            if (renderChunk != null) {
                if (playerSpectator && this.level.getBlockState(blockPos).isSolidRender(this.level, blockPos)) {
                    bl = false;
                }
                
                renderChunk.setFrame(frameCount);
                queue.add(this.instance.new RenderChunkInfo(renderChunk, null, 0));
            } else {
                int j = blockPos.getY() > Offlimits.LEVEL.getMinBuildHeight() ? Offlimits.LEVEL.getMaxBuildHeight() - 8 : Offlimits.LEVEL.getMinBuildHeight() + 8;
                int k = Mth.floor(vec3.x / 16.0) * 16;
                int l = Mth.floor(vec3.z / 16.0) * 16;
                List<LevelRenderer.RenderChunkInfo> list = Lists.newArrayList();
                
                for(int m = -this.lastViewDistance; m <= this.lastViewDistance; ++m) {
                    for(int n = -this.lastViewDistance; n <= this.lastViewDistance; ++n) {
                        ChunkRenderDispatcher.RenderChunk renderChunk2 = viewArea.callGetRenderChunkAt(new BlockPos(k + (m << 4) + 8, j, l + (n << 4) + 8));
                        if (renderChunk2 != null && camera.isVisible(renderChunk2.bb)) {
                            renderChunk2.setFrame(frameCount);
                            list.add(this.instance.new RenderChunkInfo(renderChunk2, null, 0));
                        }
                    }
                }
                
                list.sort(Comparator.comparingDouble(renderChunkInfox -> blockPos.distSqr(((RenderChunkInfoAccessor) renderChunkInfox).getChunk().getOrigin().offset(8, 8, 8))));
                queue.addAll(list);
            }
            
            this.minecraft.getProfiler().push("iteration");
            
            while(!queue.isEmpty()) {
                LevelRenderer.RenderChunkInfo renderChunkInfo = queue.poll();
                ChunkRenderDispatcher.RenderChunk renderChunk3 = ((RenderChunkInfoAccessor) renderChunkInfo).getChunk();
                Direction direction = ((RenderChunkInfoAccessor) renderChunkInfo).getSourceDirection();
                this.renderChunks.add(renderChunkInfo);
                
                for(Direction direction2 : DIRECTIONS) {
                    ChunkRenderDispatcher.RenderChunk renderChunk4 = this.getRelativeFrom(blockPos2, renderChunk3, direction2);
                    if ((!bl || !renderChunkInfo.hasDirection(direction2.getOpposite()))
                        && (!bl || direction == null || renderChunk3.getCompiledChunk().facesCanSeeEachother(direction.getOpposite(), direction2))
                        && renderChunk4 != null
                        && renderChunk4.hasAllNeighbors()
                        && renderChunk4.setFrame(frameCount)
                        && camera.isVisible(renderChunk4.bb)) {
                        LevelRenderer.RenderChunkInfo renderChunkInfo2 = this.instance.new RenderChunkInfo(renderChunk4, direction2, ((RenderChunkInfoAccessor) renderChunkInfo).getStep() + 1);
                        renderChunkInfo2.setDirections(((RenderChunkInfoAccessor) renderChunkInfo).getDirections(), direction2);
                        queue.add(renderChunkInfo2);
                    }
                }
            }
            
            this.minecraft.getProfiler().pop();
        }
        
        this.minecraft.getProfiler().popPush("rebuildNear");
        Set<ChunkRenderDispatcher.RenderChunk> set = this.chunksToCompile;
        this.chunksToCompile = Sets.newLinkedHashSet();
        
        for(LevelRenderer.RenderChunkInfo renderChunkInfo : this.renderChunks) {
            ChunkRenderDispatcher.RenderChunk renderChunk3 = ((RenderChunkInfoAccessor) renderChunkInfo).getChunk();
            if (renderChunk3.isDirty() || set.contains(renderChunk3)) {
                this.needsUpdate = true;
                BlockPos blockPos3 = renderChunk3.getOrigin().offset(8, 8, 8);
                boolean bl2 = blockPos3.distSqr(blockPos) < 768.0;
                if (!renderChunk3.isDirtyFromPlayer() && !bl2) {
                    this.chunksToCompile.add(renderChunk3);
                } else {
                    this.minecraft.getProfiler().push("build near");
                    this.chunkRenderDispatcher.rebuildChunkSync(renderChunk3);
                    renderChunk3.setNotDirty();
                    this.minecraft.getProfiler().pop();
                }
            }
        }
        
        this.chunksToCompile.addAll(set);
        this.minecraft.getProfiler().pop();
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    private @Nullable ChunkRenderDispatcher.RenderChunk getRelativeFrom(BlockPos playerPos, ChunkRenderDispatcher.RenderChunk renderChunkBase, Direction facing) {
        BlockPos blockPos = renderChunkBase.getRelativeOrigin(facing);
        if (Mth.abs(playerPos.getX() - blockPos.getX()) > this.lastViewDistance * 16) {
            return null;
        } else if (blockPos.getY() < Offlimits.LEVEL.getMinBuildHeight() || blockPos.getY() >= Offlimits.LEVEL.getMaxBuildHeight()) {
            return null;
        } else {
            return Mth.abs(playerPos.getZ() - blockPos.getZ()) > this.lastViewDistance * 16 ? null : ((ViewAreaAccessor) this.viewArea).callGetRenderChunkAt(blockPos);
        }
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    private void renderWorldBounds(Camera camera) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        WorldBorder worldBorder = this.level.getWorldBorder();
        double d = this.minecraft.options.renderDistance * 16;
        if (!(camera.getPosition().x < worldBorder.getMaxX() - d) || !(camera.getPosition().x > worldBorder.getMinX() + d) || !(camera.getPosition().z < worldBorder.getMaxZ() - d) || !(camera.getPosition().z > worldBorder.getMinZ() + d)) {
            double e = 1.0 - worldBorder.getDistanceToBorder(camera.getPosition().x, camera.getPosition().z) / d;
            e = Math.pow(e, 4.0);
            double f = camera.getPosition().x;
            double g = camera.getPosition().y;
            double h = camera.getPosition().z;
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            this.textureManager.bind(FORCEFIELD_LOCATION);
            RenderSystem.depthMask(Minecraft.useShaderTransparency());
            RenderSystem.pushMatrix();
            int i = worldBorder.getStatus().getColor();
            float j = (float)(i >> 16 & 255) / 255.0F;
            float k = (float)(i >> 8 & 255) / 255.0F;
            float l = (float)(i & 255) / 255.0F;
            RenderSystem.color4f(j, k, l, (float)e);
            RenderSystem.polygonOffset(-3.0F, -3.0F);
            RenderSystem.enablePolygonOffset();
            RenderSystem.defaultAlphaFunc();
            RenderSystem.enableAlphaTest();
            RenderSystem.disableCull();
            float m = (float)(Util.getMillis() % 3000L) / 3000.0F;
            float n = 0.0F;
            float o = 0.0F;
            float p = 128.0F;
            bufferBuilder.begin(7, DefaultVertexFormat.POSITION_TEX);
            double q = Math.max(Mth.floor(h - d), worldBorder.getMinZ());
            double r = Math.min(Mth.ceil(h + d), worldBorder.getMaxZ());
            float s;
            double t;
            double u;
            float v;
            if (f > worldBorder.getMaxX() - d) {
                s = 0.0F;
                
                for(t = q; t < r; s += 0.5F) {
                    u = Math.min(1.0, r - t);
                    v = (float)u * 0.5F;
                    this.vertex(bufferBuilder, f, g, h, worldBorder.getMaxX(), Offlimits.LEVEL.getMaxBuildHeight(), t, m + s, m + 0.0F);
                    this.vertex(bufferBuilder, f, g, h, worldBorder.getMaxX(), Offlimits.LEVEL.getMaxBuildHeight(), t + u, m + v + s, m + 0.0F);
                    this.vertex(bufferBuilder, f, g, h, worldBorder.getMaxX(), Offlimits.LEVEL.getMinBuildHeight(), t + u, m + v + s, m + 128.0F);
                    this.vertex(bufferBuilder, f, g, h, worldBorder.getMaxX(), Offlimits.LEVEL.getMinBuildHeight(), t, m + s, m + 128.0F);
                    ++t;
                }
            }
            
            if (f < worldBorder.getMinX() + d) {
                s = 0.0F;
                
                for(t = q; t < r; s += 0.5F) {
                    u = Math.min(1.0, r - t);
                    v = (float)u * 0.5F;
                    this.vertex(bufferBuilder, f, g, h, worldBorder.getMinX(), Offlimits.LEVEL.getMaxBuildHeight(), t, m + s, m + 0.0F);
                    this.vertex(bufferBuilder, f, g, h, worldBorder.getMinX(), Offlimits.LEVEL.getMaxBuildHeight(), t + u, m + v + s, m + 0.0F);
                    this.vertex(bufferBuilder, f, g, h, worldBorder.getMinX(), Offlimits.LEVEL.getMinBuildHeight(), t + u, m + v + s, m + 128.0F);
                    this.vertex(bufferBuilder, f, g, h, worldBorder.getMinX(), Offlimits.LEVEL.getMinBuildHeight(), t, m + s, m + 128.0F);
                    ++t;
                }
            }
            
            q = Math.max(Mth.floor(f - d), worldBorder.getMinX());
            r = Math.min(Mth.ceil(f + d), worldBorder.getMaxX());
            if (h > worldBorder.getMaxZ() - d) {
                s = 0.0F;
                
                for(t = q; t < r; s += 0.5F) {
                    u = Math.min(1.0, r - t);
                    v = (float)u * 0.5F;
                    this.vertex(bufferBuilder, f, g, h, t, Offlimits.LEVEL.getMaxBuildHeight(), worldBorder.getMaxZ(), m + s, m + 0.0F);
                    this.vertex(bufferBuilder, f, g, h, t + u, Offlimits.LEVEL.getMaxBuildHeight(), worldBorder.getMaxZ(), m + v + s, m + 0.0F);
                    this.vertex(bufferBuilder, f, g, h, t + u, Offlimits.LEVEL.getMinBuildHeight(), worldBorder.getMaxZ(), m + v + s, m + 128.0F);
                    this.vertex(bufferBuilder, f, g, h, t, Offlimits.LEVEL.getMinBuildHeight(), worldBorder.getMaxZ(), m + s, m + 128.0F);
                    ++t;
                }
            }
            
            if (h < worldBorder.getMinZ() + d) {
                s = 0.0F;
                
                for(t = q; t < r; s += 0.5F) {
                    u = Math.min(1.0, r - t);
                    v = (float)u * 0.5F;
                    this.vertex(bufferBuilder, f, g, h, t, Offlimits.LEVEL.getMaxBuildHeight(), worldBorder.getMinZ(), m + s, m + 0.0F);
                    this.vertex(bufferBuilder, f, g, h, t + u, Offlimits.LEVEL.getMaxBuildHeight(), worldBorder.getMinZ(), m + v + s, m + 0.0F);
                    this.vertex(bufferBuilder, f, g, h, t + u, Offlimits.LEVEL.getMinBuildHeight(), worldBorder.getMinZ(), m + v + s, m + 128.0F);
                    this.vertex(bufferBuilder, f, g, h, t, Offlimits.LEVEL.getMinBuildHeight(), worldBorder.getMinZ(), m + s, m + 128.0F);
                    ++t;
                }
            }
            
            bufferBuilder.end();
            BufferUploader.end(bufferBuilder);
            RenderSystem.enableCull();
            RenderSystem.disableAlphaTest();
            RenderSystem.polygonOffset(0.0F, 0.0F);
            RenderSystem.disablePolygonOffset();
            RenderSystem.enableAlphaTest();
            RenderSystem.disableBlend();
            RenderSystem.popMatrix();
            RenderSystem.depthMask(true);
        }
    }
}
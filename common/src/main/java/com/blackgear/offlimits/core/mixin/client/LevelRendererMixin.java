package com.blackgear.offlimits.core.mixin.client;

import com.blackgear.offlimits.Offlimits;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
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
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    
    @Unique private final LevelRenderer instance = (LevelRenderer) (Object) this;
    
    @ModifyConstant(
        method = "<init>",
        constant = @Constant(intValue = 69696)
    )
    private int offlimits$updateSections(int constant) {
        return 4356 * Offlimits.LEVEL.getSectionsCount();
    }
    
    @Inject(
        method = "setupRender",
        at = @At("HEAD"),
        cancellable = true
    )
    private void offlimits$setupRender(Camera camera, Frustum frustum, boolean debugCamera, int frameCount, boolean playerSpectator, CallbackInfo ci) {
        ViewAreaAccessor viewArea = (ViewAreaAccessor) this.viewArea;
        Vec3 cameraPosition = camera.getPosition();
        if (this.minecraft.options.renderDistance != this.lastViewDistance) {
            this.allChanged();
        }
        
        this.level.getProfiler().push("camera");
        double playerX = this.minecraft.player.getX();
        double playerY = this.minecraft.player.getY();
        double playerZ = this.minecraft.player.getZ();
        double deltaX = playerX - this.lastCameraX;
        double deltaY = playerY - this.lastCameraY;
        double deltaZ = playerZ - this.lastCameraZ;
        int cameraChunkX = SectionPos.blockToSectionCoord(Mth.floor(playerX));
        int cameraChunkY = SectionPos.blockToSectionCoord(Mth.floor(playerY));
        int cameraChunkZ = SectionPos.blockToSectionCoord(Mth.floor(playerZ));
        
        if (this.lastCameraChunkX != cameraChunkX || this.lastCameraChunkY != cameraChunkY || this.lastCameraChunkZ != cameraChunkZ || deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ > 16.0) {
            this.lastCameraX = playerX;
            this.lastCameraY = playerY;
            this.lastCameraZ = playerZ;
            this.lastCameraChunkX = cameraChunkX;
            this.lastCameraChunkY = cameraChunkY;
            this.lastCameraChunkZ = cameraChunkZ;
            this.viewArea.repositionCamera(playerX, playerZ);
        }
        
        this.chunkRenderDispatcher.setCamera(cameraPosition);
        this.level.getProfiler().popPush("cull");
        this.minecraft.getProfiler().popPush("culling");
        BlockPos cameraBlockPosition = camera.getBlockPosition();
        ChunkRenderDispatcher.RenderChunk renderChunk = viewArea.callGetRenderChunkAt(cameraBlockPosition);
        BlockPos cameraChunkPos = new BlockPos(Mth.floor(cameraPosition.x / 16.0) * 16, Mth.floor(cameraPosition.y / 16.0) * 16, Mth.floor(cameraPosition.z / 16.0) * 16);
        float cameraXRot = camera.getXRot();
        float cameraYRot = camera.getYRot();
        this.needsUpdate = this.needsUpdate
            || !this.chunksToCompile.isEmpty()
            || cameraPosition.x != this.prevCamX
            || cameraPosition.y != this.prevCamY
            || cameraPosition.z != this.prevCamZ
            || (double) cameraXRot != this.prevCamRotX
            || (double) cameraYRot != this.prevCamRotY;
        this.prevCamX = cameraPosition.x;
        this.prevCamY = cameraPosition.y;
        this.prevCamZ = cameraPosition.z;
        this.prevCamRotX = cameraXRot;
        this.prevCamRotY = cameraYRot;
        this.minecraft.getProfiler().popPush("update");
        if (!debugCamera && this.needsUpdate) {
            this.needsUpdate = false;
            this.updateRenderChunks(frustum, frameCount, playerSpectator, renderChunk, cameraBlockPosition, cameraPosition, viewArea, cameraChunkPos);
        }
        
        this.minecraft.getProfiler().popPush("rebuildNear");
        Set<ChunkRenderDispatcher.RenderChunk> chunksToCompile = this.chunksToCompile;
        this.chunksToCompile = Sets.newLinkedHashSet();
        
        for(LevelRenderer.RenderChunkInfo chunkInfo : this.renderChunks) {
            ChunkRenderDispatcher.RenderChunk chunk = ((RenderChunkInfoAccessor) chunkInfo).getChunk();
            if (chunk.isDirty() || chunksToCompile.contains(chunk)) {
                this.needsUpdate = true;
                BlockPos chunkOrigin = chunk.getOrigin().offset(8, 8, 8);
                boolean isNearPlayer = chunkOrigin.distSqr(cameraBlockPosition) < 768.0;
                
                if (!chunk.isDirtyFromPlayer() && !isNearPlayer) {
                    this.chunksToCompile.add(chunk);
                } else {
                    this.minecraft.getProfiler().push("build near");
                    this.chunkRenderDispatcher.rebuildChunkSync(chunk);
                    chunk.setNotDirty();
                    this.minecraft.getProfiler().pop();
                }
            }
        }
        
        this.chunksToCompile.addAll(chunksToCompile);
        this.minecraft.getProfiler().pop();
        
        ci.cancel();
    }
    
    @Unique
    private void updateRenderChunks(
        Frustum frustum,
        int frameCount,
        boolean playerSpectator,
        ChunkRenderDispatcher.RenderChunk renderChunk,
        BlockPos blockPos,
        Vec3 vec3,
        ViewAreaAccessor viewArea,
        BlockPos blockPos2
    ) {
        this.renderChunks.clear();
        Queue<LevelRenderer.RenderChunkInfo> queue = Queues.newArrayDeque();
        Entity.setViewScale(Mth.clamp((double) this.minecraft.options.renderDistance / 8.0, 1.0, 2.5) * (double) this.minecraft.options.entityDistanceScaling);
        boolean smartCull = this.minecraft.smartCull;
        
        if (renderChunk == null) {
            int buildHeight = blockPos.getY() > Offlimits.LEVEL.getMinBuildHeight() ? Offlimits.LEVEL.getMaxBuildHeight() - 8 : Offlimits.LEVEL.getMinBuildHeight() + 8;
            int chunkX = Mth.floor(vec3.x / 16.0) * 16;
            int chunkZ = Mth.floor(vec3.z / 16.0) * 16;
            List<LevelRenderer.RenderChunkInfo> chunks = Lists.newArrayList();
            
            for(int xOffset = -this.lastViewDistance; xOffset <= this.lastViewDistance; ++xOffset) {
                for(int zOffset = -this.lastViewDistance; zOffset <= this.lastViewDistance; ++zOffset) {
                    ChunkRenderDispatcher.RenderChunk neighborChunk = viewArea.callGetRenderChunkAt(new BlockPos(chunkX + (xOffset << 4) + 8, buildHeight, chunkZ + (zOffset << 4) + 8));
                    if (neighborChunk != null && frustum.isVisible(neighborChunk.bb)) {
                        neighborChunk.setFrame(frameCount);
                        chunks.add(this.instance.new RenderChunkInfo(neighborChunk, null, 0));
                    }
                }
            }
            
            chunks.sort(Comparator.comparingDouble(renderChunkInfox -> blockPos.distSqr(((RenderChunkInfoAccessor) renderChunkInfox).getChunk().getOrigin().offset(8, 8, 8))));
            queue.addAll(chunks);
        } else {
            if (playerSpectator && this.level.getBlockState(blockPos).isSolidRender(this.level, blockPos)) {
                smartCull = false;
            }
            
            renderChunk.setFrame(frameCount);
            queue.add(this.instance.new RenderChunkInfo(renderChunk, null, 0));
        }
        
        this.minecraft.getProfiler().push("iteration");
        
        while(!queue.isEmpty()) {
            LevelRenderer.RenderChunkInfo chunkInfo = queue.poll();
            ChunkRenderDispatcher.RenderChunk chunk = ((RenderChunkInfoAccessor) chunkInfo).getChunk();
            Direction sourceDirection = ((RenderChunkInfoAccessor) chunkInfo).getSourceDirection();
            this.renderChunks.add(chunkInfo);
            
            for(Direction direction : DIRECTIONS) {
                ChunkRenderDispatcher.RenderChunk relativeChunk = this.getRelativeFrom(blockPos2, chunk, direction);
                if ((!smartCull || !chunkInfo.hasDirection(direction.getOpposite()))
                    && (!smartCull || sourceDirection == null || chunk.getCompiledChunk().facesCanSeeEachother(sourceDirection.getOpposite(), direction))
                    && relativeChunk != null
                    && relativeChunk.hasAllNeighbors()
                    && relativeChunk.setFrame(frameCount)
                    && frustum.isVisible(relativeChunk.bb)) {
                    LevelRenderer.RenderChunkInfo renderChunkInfo = this.instance.new RenderChunkInfo(relativeChunk, direction, ((RenderChunkInfoAccessor) chunkInfo).getStep() + 1);
                    renderChunkInfo.setDirections(((RenderChunkInfoAccessor) chunkInfo).getDirections(), direction);
                    queue.add(renderChunkInfo);
                }
            }
        }
        
        this.minecraft.getProfiler().pop();
    }
    
    /**
     * @author
     * @reason
     */
    @Overwrite
    private @Nullable ChunkRenderDispatcher.RenderChunk getRelativeFrom(BlockPos playerPos, ChunkRenderDispatcher.RenderChunk renderChunkBase, Direction facing) {
        BlockPos pos = renderChunkBase.getRelativeOrigin(facing);
        if (Mth.abs(playerPos.getX() - pos.getX()) > this.lastViewDistance * 16) {
            return null;
        } else if (pos.getY() < Offlimits.LEVEL.getMinBuildHeight() || pos.getY() >= Offlimits.LEVEL.getMaxBuildHeight()) {
            return null;
        } else {
            return Mth.abs(playerPos.getZ() - pos.getZ()) > this.lastViewDistance * 16 ? null : ((ViewAreaAccessor) this.viewArea).callGetRenderChunkAt(pos);
        }
    }
    
    /**
     * @author
     * @reason Update the world rendering to render the new height changes.
     */
    @Overwrite
    private void renderWorldBounds(Camera camera) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        WorldBorder border = this.level.getWorldBorder();
        double renderDistance = (double) this.minecraft.options.renderDistance * 16;
        
        if (
            !(camera.getPosition().x < border.getMaxX() - renderDistance)
            || !(camera.getPosition().x > border.getMinX() + renderDistance)
            || !(camera.getPosition().z < border.getMaxZ() - renderDistance)
            || !(camera.getPosition().z > border.getMinZ() + renderDistance)
        ) {
            double borderDistance = 1.0 - border.getDistanceToBorder(camera.getPosition().x, camera.getPosition().z) / renderDistance;
            borderDistance = Math.pow(borderDistance, 4.0);
            borderDistance = Mth.clamp(borderDistance, 0.0, 1.0);
            double cameraX = camera.getPosition().x;
            double cameraZ = camera.getPosition().z;
            double renderDepth = (double) this.minecraft.gameRenderer.getRenderDistance() * 4.0F;
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            this.textureManager.bind(FORCEFIELD_LOCATION);
            RenderSystem.depthMask(Minecraft.useShaderTransparency());
            RenderSystem.pushMatrix();
            int color = border.getStatus().getColor();
            float red = (float) (color >> 16 & 0xFF) / 255.0F;
            float green = (float) (color >> 8 & 0xFF) / 255.0F;
            float blue = (float) (color & 0xFF) / 255.0F;
            RenderSystem.color4f(red, green, blue, (float)borderDistance);
            RenderSystem.polygonOffset(-3.0F, -3.0F);
            RenderSystem.enablePolygonOffset();
            RenderSystem.disableCull();
            float timeFraction = (float) (Util.getMillis() % 3000L) / 3000.0F;
            float depthOffset = (float) (renderDepth - Mth.frac(camera.getPosition().y));
            bufferBuilder.begin(7, DefaultVertexFormat.POSITION_TEX);
            double minZ = Math.max(Mth.floor(cameraZ - renderDistance), border.getMinZ());
            double maxZ = Math.min(Mth.ceil(cameraZ + renderDistance), border.getMaxZ());
            
            if (cameraX > border.getMaxX() - renderDistance) {
                float textureOffset = 0.0F;
                
                for (double z = minZ; z < maxZ; textureOffset += 0.5F) {
                    double zStep = Math.min(1.0, maxZ - z);
                    float zOffset = (float) zStep * 0.5F;
                    bufferBuilder.vertex(border.getMaxX() - cameraX, -renderDepth, z - cameraZ).uv(timeFraction - textureOffset, timeFraction + depthOffset).endVertex();
                    bufferBuilder.vertex(border.getMaxX() - cameraX, -renderDepth, z + zStep - cameraZ).uv(timeFraction - (zOffset + textureOffset), timeFraction + depthOffset).endVertex();
                    bufferBuilder.vertex(border.getMaxX() - cameraX, renderDepth, z + zStep - cameraZ).uv(timeFraction - (zOffset + textureOffset), timeFraction + 0.0F).endVertex();
                    bufferBuilder.vertex(border.getMaxX() - cameraX, renderDepth, z - cameraZ).uv(timeFraction - textureOffset, timeFraction + 0.0F).endVertex();
                    z++;
                }
            }
            
            if (cameraX < border.getMaxX() + renderDistance) {
                float textureOffset = 0.0F;
                
                for(double z = minZ; z < maxZ; textureOffset += 0.5F) {
                    double zStep = Math.min(1.0, maxZ - z);
                    float zOffset = (float) zStep * 0.5F;
                    bufferBuilder.vertex(border.getMinX() - cameraX, -renderDepth, z - cameraZ).uv(timeFraction + textureOffset, timeFraction + depthOffset).endVertex();
                    bufferBuilder.vertex(border.getMinX() - cameraX, -renderDepth, z + zStep - cameraZ).uv(timeFraction + zOffset + textureOffset, timeFraction + depthOffset).endVertex();
                    bufferBuilder.vertex(border.getMinX() - cameraX, renderDepth, z + zStep - cameraZ).uv(timeFraction + zOffset + textureOffset, timeFraction + 0.0F).endVertex();
                    bufferBuilder.vertex(border.getMinX() - cameraX, renderDepth, z - cameraZ).uv(timeFraction + textureOffset, timeFraction + 0.0F).endVertex();
                    z++;
                }
            }
            
            minZ = Math.max(Mth.floor(cameraX - renderDistance), border.getMinX());
            maxZ = Math.min(Mth.ceil(cameraX + renderDistance), border.getMaxX());
            if (cameraZ > border.getMaxZ() - renderDistance) {
                float textureOffset = 0.0F;
                
                for(double z = minZ; z < maxZ; textureOffset += 0.5F) {
                    double zStep = Math.min(1.0, maxZ - z);
                    float zOffset = (float) zStep * 0.5F;
                    bufferBuilder.vertex(z - cameraX, -renderDepth, border.getMaxZ() - cameraZ).uv(timeFraction + textureOffset, timeFraction + depthOffset).endVertex();
                    bufferBuilder.vertex(z + zStep - cameraX, -renderDepth, border.getMaxZ() - cameraZ).uv(timeFraction + zOffset + textureOffset, timeFraction + depthOffset).endVertex();
                    bufferBuilder.vertex(z + zStep - cameraX, renderDepth, border.getMaxZ() - cameraZ).uv(timeFraction + zOffset + textureOffset, timeFraction + 0.0F).endVertex();
                    bufferBuilder.vertex(z - cameraX, renderDepth, border.getMaxZ() - cameraZ).uv(timeFraction + textureOffset, timeFraction + 0.0F).endVertex();
                    z++;
                }
            }
            
            if (cameraZ < border.getMinZ() + renderDistance) {
                float textureOffset = 0.0F;
                
                for(double z = minZ; z < maxZ; textureOffset += 0.5F) {
                    double zStep = Math.min(1.0, maxZ - z);
                    float zOffset = (float) zStep * 0.5F;
                    bufferBuilder.vertex(z - cameraX, -renderDepth, border.getMinZ() - cameraZ).uv(timeFraction - textureOffset, timeFraction + depthOffset).endVertex();
                    bufferBuilder.vertex(z + zStep - cameraX, -renderDepth, border.getMinZ() - cameraZ).uv(timeFraction - (zOffset + textureOffset), timeFraction + depthOffset).endVertex();
                    bufferBuilder.vertex(z + zStep - cameraX, renderDepth, border.getMinZ() - cameraZ).uv(timeFraction - (zOffset + textureOffset), timeFraction + 0.0F).endVertex();
                    bufferBuilder.vertex(z - cameraX, renderDepth, border.getMinZ() - cameraZ).uv(timeFraction - textureOffset, timeFraction + 0.0F).endVertex();
                    z++;
                }
            }
            
            bufferBuilder.end();
            BufferUploader.end(bufferBuilder);
            RenderSystem.enableCull();
            RenderSystem.polygonOffset(0.0F, 0.0F);
            RenderSystem.disablePolygonOffset();
            RenderSystem.disableBlend();
            RenderSystem.popMatrix();
            RenderSystem.depthMask(true);
        }
    }
}
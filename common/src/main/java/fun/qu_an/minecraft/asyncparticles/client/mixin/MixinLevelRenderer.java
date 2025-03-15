package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 499)
public abstract class MixinLevelRenderer {
	@Inject(method = "renderLevel", at = @At(value = "HEAD"))
	private void onRenderLevelHead(DeltaTracker deltaTracker,
								   boolean renderBlockOutline,
								   Camera camera,
								   GameRenderer gameRenderer,
								   LightTexture lightTexture,
								   Matrix4f frustumMatrix,
								   Matrix4f projectionMatrix,
								   CallbackInfo ci) {
		// as early as possible
		AsyncRenderer.start(deltaTracker.getGameTimeDeltaPartialTick(false), camera);
	}

	@Inject(method = "renderLevel",
		slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderWorldBorder(Lnet/minecraft/client/Camera;)V")),
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderDebug(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/Camera;)V"))
	private void onRenderLevelTail(DeltaTracker deltaTracker,
								   boolean renderBlockOutline,
								   Camera camera,
								   GameRenderer gameRenderer,
								   LightTexture lightTexture,
								   Matrix4f frustumMatrix,
								   Matrix4f projectionMatrix,
								   CallbackInfo ci,
								   @Local(ordinal = 0) float f) {
		AsyncRenderer.join(f, camera, lightTexture);
	}

	// See fabric/MixinLevelRenderer.java
	// See forge/MixinLevelRenderer.java
//	@WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V"))
//	private void redirectRenderParticles(ParticleEngine instance, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, LightTexture lightTexture, Camera camera, float f, Operation<Void> original) {
//		AsyncRenderer.irisTranslucent(poseStack, f, camera, lightTexture);
//	}

	@Redirect(method = "renderLevel",
		slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;particlesTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;")),
		at = @At(value = "INVOKE", ordinal = 0, target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;clear(Z)V"))
	private void redirectClearRenderTarget(RenderTarget instance, boolean bl) {
	}

	@Redirect(method = "renderLevel",
		slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;particlesTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;")),
		at = @At(value = "INVOKE", ordinal = 0, target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;copyDepthFrom(Lcom/mojang/blaze3d/pipeline/RenderTarget;)V"))
	private void redirectCopyDepthFrom(RenderTarget instance, RenderTarget target) {
	}

	@Redirect(method = "renderLevel",
		slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderStateShard;PARTICLES_TARGET:Lnet/minecraft/client/renderer/RenderStateShard$OutputStateShard;")),
		at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/renderer/RenderStateShard$OutputStateShard;setupRenderState()V"))
	private void redirectSetupRenderState(RenderStateShard.OutputStateShard instance) {
	}

	@Redirect(method = "renderLevel",
		slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderStateShard;PARTICLES_TARGET:Lnet/minecraft/client/renderer/RenderStateShard$OutputStateShard;")),
		at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/renderer/RenderStateShard$OutputStateShard;clearRenderState()V"))
	private void redirectClearRenderState(RenderStateShard.OutputStateShard instance) {
	}

	@WrapMethod(method = "setSectionDirty(IIIZ)V")
	public void setSectionDirty(int x, int y, int z, boolean reRenderOnMainThread, Operation<Void> original) {
		if (RenderSystem.isOnRenderThread() || !SimplePropertiesConfig.forceSyncLevelRendererMarkDirty()) {
			original.call(x, y, z, reRenderOnMainThread);
		} else {
			RenderSystem.recordRenderCall(() -> original.call(x, y, z, reRenderOnMainThread));
		}
	}

	@WrapMethod(method = "setBlockDirty(Lnet/minecraft/core/BlockPos;Z)V")
	public void setBlockDirty(BlockPos pos, boolean reRenderOnMainThread, Operation<Void> original) {
		if (RenderSystem.isOnRenderThread() || !SimplePropertiesConfig.forceSyncLevelRendererMarkDirty()) {
			original.call(pos, reRenderOnMainThread);
		} else {
			RenderSystem.recordRenderCall(() -> original.call(pos, reRenderOnMainThread));
		}
	}

	@WrapMethod(method = "setBlocksDirty")
	public void setBlocksDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Operation<Void> original) {
		if (RenderSystem.isOnRenderThread() || !SimplePropertiesConfig.forceSyncLevelRendererMarkDirty()) {
			original.call(minX, minY, minZ, maxX, maxY, maxZ);
		} else {
			RenderSystem.recordRenderCall(() -> original.call(minX, minY, minZ, maxX, maxY, maxZ));
		}
	}

	@WrapMethod(method = "setSectionDirtyWithNeighbors")
	public void setSectionDirtyWithNeighbors(int sectionX, int sectionY, int sectionZ, Operation<Void> original) {
		if (RenderSystem.isOnRenderThread() || !SimplePropertiesConfig.forceSyncLevelRendererMarkDirty()) {
			original.call(sectionX, sectionY, sectionZ);
		} else {
			RenderSystem.recordRenderCall(() -> original.call(sectionX, sectionY, sectionZ));
		}
	}

	@WrapMethod(method = "destroyBlockProgress")
	public void destroyBlockProgress(int breakerId, BlockPos pos, int progress, Operation<Void> original) {
		if (RenderSystem.isOnRenderThread() || !SimplePropertiesConfig.forceSyncLevelRendererMarkDirty()) {
			original.call(breakerId, pos, progress);
		} else {
			RenderSystem.recordRenderCall(() -> original.call(breakerId, pos, progress));
		}
	}
}

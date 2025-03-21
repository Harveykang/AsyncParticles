package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.*;
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
	private void onRenderLevelHead(PoseStack poseStack, float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, CallbackInfo ci) {
		AsyncRenderer.start(f, camera);
	}

	@Inject(method = "renderLevel",
		at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/renderer/LevelRenderer;renderDebug(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/Camera;)V"))
	private void onRenderLevelTail(PoseStack poseStack, float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, CallbackInfo ci) {
		AsyncRenderer.join(poseStack, f, camera, lightTexture);
	}

	@Inject(method = "renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V",
		at = @At(value = "FIELD", ordinal = 0, target = "Lnet/minecraft/client/renderer/LevelRenderer;transparencyChain:Lnet/minecraft/client/renderer/PostChain;"))
	private void onRenderLevelTransparencyChain(PoseStack poseStack, float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
		AsyncRenderer.irisOpaque(poseStack, f, camera, lightTexture);
	}

	@Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=entities"))
	private void beforeRenderEntities(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
		AsyncRenderer.irisSync(poseStack, partialTick, camera, lightTexture);
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
		if (RenderSystem.isOnRenderThread()) {
			original.call(x, y, z, reRenderOnMainThread);
		} else {
			ThreadUtil.submitClientTask(() -> original.call(x, y, z, reRenderOnMainThread));
		}
	}

	@WrapMethod(method = "setBlockDirty(Lnet/minecraft/core/BlockPos;Z)V")
	public void setBlockDirty(BlockPos pos, boolean reRenderOnMainThread, Operation<Void> original) {
		if (RenderSystem.isOnRenderThread()) {
			original.call(pos, reRenderOnMainThread);
		} else {
			ThreadUtil.submitClientTask(() -> original.call(pos, reRenderOnMainThread));
		}
	}

	@WrapMethod(method = "setBlocksDirty")
	public void setBlocksDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Operation<Void> original) {
		if (RenderSystem.isOnRenderThread()) {
			original.call(minX, minY, minZ, maxX, maxY, maxZ);
		} else {
			ThreadUtil.submitClientTask(() -> original.call(minX, minY, minZ, maxX, maxY, maxZ));
		}
	}

	@WrapMethod(method = "setSectionDirtyWithNeighbors")
	public void setSectionDirtyWithNeighbors(int sectionX, int sectionY, int sectionZ, Operation<Void> original) {
		if (RenderSystem.isOnRenderThread()) {
			original.call(sectionX, sectionY, sectionZ);
		} else {
			ThreadUtil.submitClientTask(() -> original.call(sectionX, sectionY, sectionZ));
		}
	}

	@WrapMethod(method = "destroyBlockProgress")
	public void destroyBlockProgress(int breakerId, BlockPos pos, int progress, Operation<Void> original) {
		if (RenderSystem.isOnRenderThread()) {
			original.call(breakerId, pos, progress);
		} else {
			ThreadUtil.submitClientTask(() -> original.call(breakerId, pos, progress));
		}
	}
}

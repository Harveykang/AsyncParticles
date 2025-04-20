package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 499)
public abstract class MixinLevelRenderer {
	@Shadow private @Nullable Frustum capturedFrustum;

	@Shadow @Final
	private Vector3d frustumPos;

	@Shadow private Frustum cullingFrustum;

	@Inject(method = "renderLevel", at = @At(value = "HEAD"))
	private void onRenderLevelHead(PoseStack poseStack, float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, CallbackInfo ci) {
		if (this.capturedFrustum != null) {
			Frustum frustum = this.capturedFrustum;
			frustum.prepare(this.frustumPos.x, this.frustumPos.y, this.frustumPos.z);
			AsyncRenderer.frustum = frustum;
		} else {
			AsyncRenderer.frustum = this.cullingFrustum;
		}
		AsyncRenderer.start(f, camera);
	}

	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;prepare(DDD)V"))
	private void redirectPrepare(Frustum frustum, double x, double y, double z) {
		// do nothing
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
}

package fun.qu_an.minecraft.asyncparticles.client.mixin.render;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
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
	@Shadow
	private @Nullable Frustum capturedFrustum;

	@Shadow
	@Final
	private Vector3d frustumPos;

	@Shadow
	private Frustum cullingFrustum;

	@Shadow @Nullable public PostChain transparencyChain;

	@Inject(method = "renderLevel", at = @At(value = "HEAD"))
	private void onRenderLevelHead(PoseStack poseStack,
								   float f,
								   long l,
								   boolean bl,
								   Camera camera,
								   GameRenderer gameRenderer,
								   LightTexture lightTexture,
								   Matrix4f matrix4f,
								   CallbackInfo ci,
								   @Share(namespace = "asyncparticles", value = "isRenderAsync")
								   LocalBooleanRef isRenderAsync,
								   @Share(namespace = "asyncparticles", value = "isMixedParticleRendering")
								   LocalBooleanRef isMixedParticleRendering) {
		boolean b = ConfigHelper.isRenderAsync();
		isRenderAsync.set(b);
		if (this.capturedFrustum != null) {
			Frustum frustum = this.capturedFrustum;
			frustum.prepare(this.frustumPos.x, this.frustumPos.y, this.frustumPos.z);
			AsyncRenderer.frustum = frustum;
		} else {
			AsyncRenderer.frustum = this.cullingFrustum;
		}
		// TODO move to iris compat
		AsyncRenderer.captureParticleRenderingSetting();
		isMixedParticleRendering.set(AsyncRenderer.isMixedParticleRendering());
		AsyncRenderer.start(f, camera, b);
	}

	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;prepare(DDD)V"))
	private void redirectPrepare(Frustum frustum, double x, double y, double z) {
		// do nothing
	}

	@Redirect(method = "renderLevel",
		slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;particlesTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;")),
		at = @At(value = "INVOKE", ordinal = 0, target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;clear(Z)V"))
	private void redirectClearRenderTarget(RenderTarget instance, boolean bl,
										   @Share(namespace = "asyncparticles", value = "isRenderAsync")
										   LocalBooleanRef isRenderAsync) {
		if (!isRenderAsync.get()) {
			instance.clear(bl);
		}
	}

	@Redirect(method = "renderLevel",
		slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;particlesTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;")),
		at = @At(value = "INVOKE", ordinal = 0, target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;copyDepthFrom(Lcom/mojang/blaze3d/pipeline/RenderTarget;)V"))
	private void redirectCopyDepthFrom(RenderTarget instance, RenderTarget target,
									   @Share(namespace = "asyncparticles", value = "isRenderAsync")
									   LocalBooleanRef isRenderAsync) {
		if (!isRenderAsync.get()) {
			instance.copyDepthFrom(target);
		}
	}

	@Redirect(method = "renderLevel",
		slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderStateShard;PARTICLES_TARGET:Lnet/minecraft/client/renderer/RenderStateShard$OutputStateShard;")),
		at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/renderer/RenderStateShard$OutputStateShard;setupRenderState()V"))
	private void redirectSetupRenderState(RenderStateShard.OutputStateShard instance,
										  @Share(namespace = "asyncparticles", value = "isRenderAsync")
										  LocalBooleanRef isRenderAsync) {
		if (!isRenderAsync.get()) {
			instance.setupRenderState();
		}
	}

	@Redirect(method = "renderLevel",
		slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderStateShard;PARTICLES_TARGET:Lnet/minecraft/client/renderer/RenderStateShard$OutputStateShard;")),
		at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/renderer/RenderStateShard$OutputStateShard;clearRenderState()V"))
	private void redirectClearRenderState(RenderStateShard.OutputStateShard instance,
										  @Share(namespace = "asyncparticles", value = "isRenderAsync")
										  LocalBooleanRef isRenderAsync) {
		if (!isRenderAsync.get()) {
			instance.clearRenderState();
		}
	}

	@Inject(method = "renderLevel", // priority = 499, inject earlier
		at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/renderer/LevelRenderer;renderDebug(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/Camera;)V"))
	private void onRenderLevelTail(PoseStack poseStack,
								   float f,
								   long l,
								   boolean bl,
								   Camera camera,
								   GameRenderer gameRenderer,
								   LightTexture lightTexture,
								   Matrix4f matrix4f,
								   CallbackInfo ci,
								   @Share(namespace = "asyncparticles", value = "isRenderAsync")
								   LocalBooleanRef isRenderAsync,
								   @Share(namespace = "asyncparticles", value = "isMixedParticleRendering")
								   LocalBooleanRef isMixedParticleRendering) {
		if (isRenderAsync.get() &&
			!isMixedParticleRendering.get() &&
			!ConfigHelper.isCompatibilityRendering() &&
			transparencyChain == null) {
			AsyncRenderer.endAll(poseStack, f, camera, lightTexture);
		}
	}
}

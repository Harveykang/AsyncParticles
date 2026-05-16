package fun.qu_an.minecraft.asyncparticles.client.mixin.core.render;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncRenderBehavior;
import fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.particle.GpuParticleBehavior;
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

	@Inject(method = "renderLevel", at = @At(value = "HEAD"))
	private void onRenderLevelHead(PoseStack poseStack,
								   float partialTick,
								   long l,
								   boolean bl,
								   Camera camera,
								   GameRenderer gameRenderer,
								   LightTexture lightTexture,
								   Matrix4f matrix4f,
								   CallbackInfo ci,
								   @Share(namespace = AsyncParticlesClient.MOD_ID, value = "internalRenderingMode")
								   LocalIntRef irm) {
		GpuParticleBehavior.INSTANCE.beginFrame();
		if (this.capturedFrustum != null) {
			Frustum frustum = this.capturedFrustum;
			frustum.prepare(this.frustumPos.x, this.frustumPos.y, this.frustumPos.z);
			AsyncRenderBehavior.INSTANCE.setFrustum(frustum);
		} else {
			AsyncRenderBehavior.INSTANCE.setFrustum(this.cullingFrustum);
		}
		int irmValue = InternalRenderingMode.updateInternalMode(ConfigHelper.getParticleRenderingMode());
		irm.set(irmValue);
		AsyncRenderBehavior.INSTANCE.start(partialTick, camera, irmValue);
	}

	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;prepare(DDD)V"))
	private void redirectPrepare(Frustum frustum, double x, double y, double z) {
		// no-op
	}

	@Redirect(method = "renderLevel",
		slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;particlesTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;")),
		at = @At(value = "INVOKE", ordinal = 0, target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;clear(Z)V"))
	private void redirectClearRenderTarget(RenderTarget instance, boolean bl) {
		// no-op
	}

	@Redirect(method = "renderLevel",
		slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;particlesTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;")),
		at = @At(value = "INVOKE", ordinal = 0, target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;copyDepthFrom(Lcom/mojang/blaze3d/pipeline/RenderTarget;)V"))
	private void redirectCopyDepthFrom(RenderTarget instance, RenderTarget target) {
		// no-op
	}

	@Redirect(method = "renderLevel",
		slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderStateShard;PARTICLES_TARGET:Lnet/minecraft/client/renderer/RenderStateShard$OutputStateShard;")),
		at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/renderer/RenderStateShard$OutputStateShard;setupRenderState()V"))
	private void redirectSetupRenderState(RenderStateShard.OutputStateShard instance) {
		// no-op
	}

	@Redirect(method = "renderLevel",
		slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderStateShard;PARTICLES_TARGET:Lnet/minecraft/client/renderer/RenderStateShard$OutputStateShard;")),
		at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/renderer/RenderStateShard$OutputStateShard;clearRenderState()V"))
	private void redirectClearRenderState(RenderStateShard.OutputStateShard instance) {
		// no-op
	}
}

package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
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
	@Nullable
	public PostChain transparencyChain;

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

	@Inject(method = "renderLevel", order = 1500, // later
		slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderStateShard;WEATHER_TARGET:Lnet/minecraft/client/renderer/RenderStateShard$OutputStateShard;")),
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PostChain;process(F)V"))
	private void onRenderLevelTail0(DeltaTracker deltaTracker,
									boolean renderBlockOutline,
									Camera camera,
									GameRenderer gameRenderer,
									LightTexture lightTexture,
									Matrix4f frustumMatrix,
									Matrix4f projectionMatrix,
									CallbackInfo ci,
									@Local(ordinal = 0) float f) {
		// Fabulous Graphics
		AsyncRenderer.join(f, camera, lightTexture);
	}

	@Inject(method = "renderLevel", order = 1500, // later
		slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderStateShard;WEATHER_TARGET:Lnet/minecraft/client/renderer/RenderStateShard$OutputStateShard;")),
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderDebug(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/Camera;)V"))
	private void onRenderLevelTail1(DeltaTracker deltaTracker,
									boolean renderBlockOutline,
									Camera camera,
									GameRenderer gameRenderer,
									LightTexture lightTexture,
									Matrix4f frustumMatrix,
									Matrix4f projectionMatrix,
									CallbackInfo ci,
									@Local(ordinal = 0) float f) {
		// non-Fabulous Graphics
		if (transparencyChain == null) {
			AsyncRenderer.join(f, camera, lightTexture);
		}
	}

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

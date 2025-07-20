package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.iris_like;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.blaze3d.vertex.PoseStack;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode.*;

@Mixin(value = LevelRenderer.class, priority = 1500) // After mixin.render.MixinLevelRenderer
public abstract class MixinLevelRenderer_Late {
	@Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=entities"))
	private void beforeRenderEntities(PoseStack poseStack,
									  float partialTick,
									  long finishNanoTime,
									  boolean renderBlockOutline,
									  Camera camera,
									  GameRenderer gameRenderer,
									  LightTexture lightTexture,
									  Matrix4f projectionMatrix,
									  CallbackInfo ci,
									  @Share(namespace = "asyncparticles", value = "internalRenderingMode")
									  LocalIntRef irm) {
		if (irm.get() != DELAYED_ASYNC){
			AsyncRenderer.irisCustom(poseStack, partialTick, camera, lightTexture);
		}
	}

	@Inject(method = "renderLevel",
		at = @At(value = "FIELD", ordinal = 0, target = "Lnet/minecraft/client/renderer/LevelRenderer;transparencyChain:Lnet/minecraft/client/renderer/PostChain;"))
	private void onRenderLevelTranslucent(PoseStack poseStack,
										  float partialTick,
										  long l,
										  boolean bl,
										  Camera camera,
										  GameRenderer gameRenderer,
										  LightTexture lightTexture,
										  Matrix4f projectionMatrix,
										  CallbackInfo ci,
										  @Share(namespace = "asyncparticles", value = "internalRenderingMode")
										  LocalIntRef irm) {
		switch (irm.get()) {
			case BEFORE_SYNC -> AsyncRenderer.endAll(poseStack, partialTick, camera, lightTexture, false);
			case MIXED_SYNC, SYNC -> AsyncRenderer.irisOpaque(poseStack, partialTick, camera, lightTexture, false);
			case MIXED_ASYNC, COMPATIBILITY_ASYNC ->
				AsyncRenderer.irisOpaque(poseStack, partialTick, camera, lightTexture, true);
			case BEFORE_ASYNC -> AsyncRenderer.endAll(poseStack, partialTick, camera, lightTexture, true);
		}
	}

	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", remap = false,
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V"))
	private void redirectRenderParticles(ParticleEngine instance,
										 PoseStack poseStack,
										 MultiBufferSource.BufferSource bufferSource,
										 LightTexture lightTexture,
										 Camera camera,
										 float partialTick,
										 Frustum frustum,
										 @Share(namespace = "asyncparticles", value = "internalRenderingMode")
										 LocalIntRef irm) {

		switch (irm.get()) {
			case MIXED_SYNC, SYNC -> AsyncRenderer.irisTranslucent(poseStack, partialTick, camera, lightTexture, false);
			case MIXED_ASYNC, COMPATIBILITY_ASYNC ->
				AsyncRenderer.irisTranslucent(poseStack, partialTick, camera, lightTexture, true);
		}
	}
}

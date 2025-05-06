package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.iris_like;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.blaze3d.vertex.PoseStack;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 1500) // After mixin.render.MixinLevelRenderer
public abstract class MixinLevelRenderer_Late {
	@Shadow
	@Final
	public RenderBuffers renderBuffers;

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
									  @Share(namespace = "asyncparticles", value = "isRenderAsync")
									  LocalBooleanRef isRenderAsync,
									  @Share(namespace = "asyncparticles", value = "isMixedParticleRendering")
									  LocalBooleanRef isMixedParticleRendering) {
		if (!isMixedParticleRendering.get()) {
			return;
		}
		if (isRenderAsync.get()) {
			AsyncRenderer.irisSync(poseStack, partialTick, camera, lightTexture);
		} else {
			ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
			((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.OPAQUE);
			particleEngine.render(poseStack, this.renderBuffers.bufferSource(), lightTexture, camera, partialTick, AsyncRenderer.frustum);
		}
	}

	@WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", remap = false,
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V"))
	private void redirectRenderParticles(ParticleEngine instance,
										 PoseStack poseStack,
										 MultiBufferSource.BufferSource bufferSource,
										 LightTexture lightTexture,
										 Camera camera,
										 float partialTicks,
										 Frustum frustum,
										 Operation<Void> original,
										 @Share(namespace = "asyncparticles", value = "isRenderAsync")
										 LocalBooleanRef isRenderAsync,
										 @Share(namespace = "asyncparticles", value = "isMixedParticleRendering")
										 LocalBooleanRef isMixedParticleRendering) {
		if (isRenderAsync.get()) {
			if (isMixedParticleRendering.get()) {
				AsyncRenderer.irisTranslucent(poseStack, partialTicks, camera, lightTexture);
			} else if (ConfigHelper.isCompatibilityRendering()) {
				AsyncRenderer.join(poseStack, partialTicks, camera, lightTexture);
			}
		} else {
			if (isMixedParticleRendering.get()) {
				((PhasedParticleEngine) Minecraft.getInstance().particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.TRANSLUCENT);
			} else {
				((PhasedParticleEngine) Minecraft.getInstance().particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.EVERYTHING);
			}
			original.call(instance, poseStack, bufferSource, lightTexture, camera, partialTicks, frustum);
		}
	}
}

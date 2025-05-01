package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.iris;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.*;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 499)
public abstract class MixinLevelRenderer {
	@Shadow @Final public RenderBuffers renderBuffers;

	@WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V"))
	private void redirectRenderParticles(ParticleEngine instance, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, LightTexture lightTexture, Camera camera, float f, Operation<Void> original) {
		if (SimplePropertiesConfig.isRenderAsync()) {
			AsyncRenderer.irisTranslucent(poseStack, f, camera, lightTexture);
		} else {
			if (AsyncRenderer.isMixedParticleRenderingSetting()){
				((PhasedParticleEngine) Minecraft.getInstance().particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.TRANSLUCENT);
			} else {
				((PhasedParticleEngine) Minecraft.getInstance().particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.EVERYTHING);
			}
			original.call(instance, poseStack, bufferSource, lightTexture, camera, f);
		}
	}

	@Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=entities"))
	private void beforeRenderEntities(PoseStack poseStack,
									  float partialTick,
									  long finishNanoTime,
									  boolean renderBlockOutline,
									  Camera camera,
									  GameRenderer gameRenderer,
									  LightTexture lightTexture,
									  Matrix4f projectionMatrix,
									  CallbackInfo ci) {
		if (SimplePropertiesConfig.isRenderAsync()) {
			AsyncRenderer.irisSync(poseStack, partialTick, camera, lightTexture);
		} else if (AsyncRenderer.isMixedParticleRenderingSetting()) {
			ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
			((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.OPAQUE);
			particleEngine.render(poseStack, this.renderBuffers.bufferSource(), lightTexture, camera, partialTick);
		}
	}
}

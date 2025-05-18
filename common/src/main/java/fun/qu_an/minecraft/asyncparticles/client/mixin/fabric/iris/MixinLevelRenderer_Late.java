package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.iris;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.*;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 1500) // After mixin.render.MixinLevelRenderer
public abstract class MixinLevelRenderer_Late {
	@Inject(method = "renderLevel", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/multiplayer/ClientLevel;entitiesForRendering()Ljava/lang/Iterable;"))
	private void beforeRenderEntities(DeltaTracker deltaTracker,
									  boolean renderBlockOutline,
									  Camera camera,
									  GameRenderer gameRenderer,
									  LightTexture lightTexture,
									  Matrix4f frustumMatrix,
									  Matrix4f projectionMatrix,
									  CallbackInfo ci,
									  @Local(ordinal = 0) float partialTick,
									  @Share(namespace = "asyncparticles", value = "isRenderAsync")
									  LocalBooleanRef isRenderAsync,
									  @Share(namespace = "asyncparticles", value = "isMixedParticleRendering")
									  LocalBooleanRef isMixedParticleRendering) {
		if (!isRenderAsync.get() && isMixedParticleRendering.get()) {
			AsyncRenderer.endOpaque(partialTick, camera, lightTexture);
		}
	}

	@Inject(method = "renderLevel", at = @At(value = "FIELD", ordinal = 0,
		target = "Lnet/minecraft/client/renderer/LevelRenderer;transparencyChain:Lnet/minecraft/client/renderer/PostChain;"))
	private void onRenderLevelTransparencyChain(DeltaTracker deltaTracker,
												boolean renderBlockOutline,
												Camera camera,
												GameRenderer gameRenderer,
												LightTexture lightTexture,
												Matrix4f frustumMatrix,
												Matrix4f projectionMatrix,
												CallbackInfo ci,
												@Local(ordinal = 0) float partialTick,
												@Share(namespace = "asyncparticles", value = "isRenderAsync")
												LocalBooleanRef isRenderAsync,
												@Share(namespace = "asyncparticles", value = "isMixedParticleRendering")
												LocalBooleanRef isMixedParticleRendering) {
		if (isRenderAsync.get() && isMixedParticleRendering.get()) {
			AsyncRenderer.endOpaque(partialTick, camera, lightTexture);
		}
	}

	@Redirect(method = "renderLevel", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V"))
	private void redirectRenderParticles(ParticleEngine instance,
										 LightTexture lightTexture,
										 Camera camera,
										 float partialTick,
										 @Share(namespace = "asyncparticles", value = "isRenderAsync")
										 LocalBooleanRef isRenderAsync,
										 @Share(namespace = "asyncparticles", value = "isMixedParticleRendering")
										 LocalBooleanRef isMixedParticleRendering) {
		if (isRenderAsync.get()) {
			if (isMixedParticleRendering.get()) {
				AsyncRenderer.endTranslucent(partialTick, camera, lightTexture);
			} else if (ConfigHelper.isCompatibilityRendering()) {
				AsyncRenderer.endAll(partialTick, camera, lightTexture);
			}
		} else {
			if (isMixedParticleRendering.get()) {
				((PhasedParticleEngine) Minecraft.getInstance().particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.TRANSLUCENT);
			} else {
				((PhasedParticleEngine) Minecraft.getInstance().particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.EVERYTHING);
			}
			instance.render(lightTexture, camera, partialTick);
		}
	}
}

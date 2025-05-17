package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.iris_like;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(value = LevelRenderer.class, priority = 1500) // After mixin.render.MixinLevelRenderer
public abstract class MixinLevelRenderer_Late {
	@WrapWithCondition(method = "renderLevel", at = @At(value = "INVOKE", ordinal = 0, remap = false,
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"))
	private boolean redirectRenderParticles0(ParticleEngine instance,
											LightTexture lightTexture,
											Camera camera,
											float v,
											Frustum frustum,
											Predicate<ParticleRenderType> predicate,
											@Local(ordinal = 0) float partialTick,
											@Share(namespace = "asyncparticles", value = "isRenderAsync")
											LocalBooleanRef isRenderAsync) {
		if (!isRenderAsync.get()) {
			return true;
		}
//		assert !isMixedParticleRendering.get();
		if (ConfigHelper.isCompatibilityRendering()) {
			AsyncRenderer.join(partialTick, camera, lightTexture);
		}
		return false;
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE", ordinal = 1, remap = false,
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"))
	private void beforeRenderParticles1(DeltaTracker deltaTracker,
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
			ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
			particleEngine.render(lightTexture, camera, partialTick, AsyncRenderer.frustum, t -> !t.isTranslucent());
		}
	}

	@WrapWithCondition(method = "renderLevel", at = @At(value = "INVOKE", ordinal = 1, remap = false,
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"))
	private boolean redirectRenderParticles1(ParticleEngine instance,
											 LightTexture lightTexture,
											 Camera camera,
											 float v,
											 Frustum frustum,
											 Predicate<ParticleRenderType> predicate,
											 @Local(ordinal = 0) float partialTick,
											 @Share(namespace = "asyncparticles", value = "isRenderAsync")
											 LocalBooleanRef isRenderAsync,
											 @Share(namespace = "asyncparticles", value = "isMixedParticleRendering")
											 LocalBooleanRef isMixedParticleRendering) {
		if (!isRenderAsync.get()) {
			return true;
		}
		if (isMixedParticleRendering.get()) {
			AsyncRenderer.irisOpaque(partialTick, camera, lightTexture, t -> !t.isTranslucent());
		} else if (ConfigHelper.isCompatibilityRendering()) {
			AsyncRenderer.join(partialTick, camera, lightTexture);
		}
		return false;
	}

	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", remap = false, ordinal = 2,
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"))
	private void redirectRenderParticles2(ParticleEngine instance,
										  LightTexture lightTexture,
										  Camera camera,
										  float v,
										  Frustum frustum,
										  Predicate<ParticleRenderType> predicate,
										  @Local(ordinal = 0) float partialTick,
										  @Share(namespace = "asyncparticles", value = "isRenderAsync")
										  LocalBooleanRef isRenderAsync,
										  @Share(namespace = "asyncparticles", value = "isMixedParticleRendering")
										  LocalBooleanRef isMixedParticleRendering) {
		if (isRenderAsync.get()) {
			if (isMixedParticleRendering.get()) {
				AsyncRenderer.irisTranslucent(partialTick, camera, lightTexture, ParticleRenderType::isTranslucent);
			} else if (ConfigHelper.isCompatibilityRendering()) {
				AsyncRenderer.join(partialTick, camera, lightTexture);
			}
		} else {
			if (isMixedParticleRendering.get()) {
				instance.render(lightTexture, camera, v, frustum, ParticleRenderType::isTranslucent);
			}
		}
	}
}

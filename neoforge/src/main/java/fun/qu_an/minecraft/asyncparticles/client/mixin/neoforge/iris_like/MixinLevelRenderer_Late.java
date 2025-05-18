package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.iris_like;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

@Mixin(value = LevelRenderer.class, priority = 1500) // After mixin.render.MixinLevelRenderer
public abstract class MixinLevelRenderer_Late {
	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", ordinal = 0, remap = false,
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"))
	private void redirectRenderParticles0(ParticleEngine instance,
										  LightTexture lightTexture,
										  Camera camera,
										  float partialTick,
										  Frustum frustum,
										  Predicate<ParticleRenderType> predicate,
										  @Share(namespace = "asyncparticles", value = "isRenderAsync")
										  LocalBooleanRef isRenderAsync) {
		if (!isRenderAsync.get()) {
			instance.render(lightTexture, camera, partialTick, frustum, predicate);
		} else if (ConfigHelper.isCompatibilityRendering()) {
//			assert !isMixedParticleRendering.get();
			AsyncRenderer.endAll(partialTick, camera, lightTexture);
		}
	}

	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", ordinal = 1, remap = false,
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"))
	private void redirectRenderParticles1(ParticleEngine instance,
										  LightTexture lightTexture,
										  Camera camera,
										  float partialTick,
										  Frustum frustum,
										  Predicate<ParticleRenderType> predicate,
										  @Share(namespace = "asyncparticles", value = "isRenderAsync")
										  LocalBooleanRef isRenderAsync,
										  @Share(namespace = "asyncparticles", value = "isMixedParticleRendering")
										  LocalBooleanRef isMixedParticleRendering) {
		if (!isRenderAsync.get()) {
			instance.render(lightTexture, camera, partialTick, frustum, predicate);
		} else if (isMixedParticleRendering.get() || ConfigHelper.isCompatibilityRendering()) {
			AsyncRenderer.endOpaque(partialTick, camera, lightTexture);
		}
	}

	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", remap = false, ordinal = 2,
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"))
	private void redirectRenderParticles2(ParticleEngine instance,
										  LightTexture lightTexture,
										  Camera camera,
										  float partialTick,
										  Frustum frustum,
										  Predicate<ParticleRenderType> predicate,
										  @Share(namespace = "asyncparticles", value = "isRenderAsync")
										  LocalBooleanRef isRenderAsync,
										  @Share(namespace = "asyncparticles", value = "isMixedParticleRendering")
										  LocalBooleanRef isMixedParticleRendering) {
		if (!isRenderAsync.get()) {
			instance.render(lightTexture, camera, partialTick, frustum, predicate);
		} else if (isMixedParticleRendering.get() || ConfigHelper.isCompatibilityRendering()) {
			AsyncRenderer.endTranslucent(partialTick, camera, lightTexture);
		}
	}
}

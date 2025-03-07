package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;

@Mixin(value = LevelRenderer.class, priority = 499)
public abstract class MixinLevelRenderer {
	@WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", ordinal = 1, remap = false,
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"))
	private void redirectRenderParticles1(ParticleEngine instance,
										  LightTexture lightTexture,
										  Camera camera,
										  float f,
										  Frustum frustum,
										  Predicate<ParticleRenderType> predicate,
										  Operation<Void> original) {
		AsyncRenderer.irisOpaque(f, camera, lightTexture, predicate);
	}

	@WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", ordinal = 2, remap = false,
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"))
	private void redirectRenderParticles2(ParticleEngine instance,
										  LightTexture lightTexture,
										  Camera camera,
										  float f,
										  Frustum frustum,
										  Predicate<ParticleRenderType> predicate,
										  Operation<Void> original) {
		AsyncRenderer.irisTranslucent(f, camera, lightTexture, predicate);
	}

	@WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", ordinal = 0, remap = false,
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"))
	private void redirectRenderParticles0(ParticleEngine instance,
										  LightTexture lightTexture,
										  Camera camera,
										  float v,
										  Frustum frustum,
										  Predicate<ParticleRenderType> predicate,
										  Operation<Void> original) {
		// do nothing
	}
}

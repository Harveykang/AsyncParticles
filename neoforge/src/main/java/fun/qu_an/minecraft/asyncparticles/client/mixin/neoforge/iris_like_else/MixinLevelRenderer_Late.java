package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.iris_like_else;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;

@Mixin(value = LevelRenderer.class, priority = 1500) // After mixin.render.MixinLevelRenderer
public abstract class MixinLevelRenderer_Late {
	@WrapWithCondition(method = "renderLevel", at = {
		@At(value = "INVOKE", remap = false, ordinal = 0,
			target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"),
		@At(value = "INVOKE", remap = false, ordinal = 2,
			target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V")
	})
	private boolean redirectRenderParticles(ParticleEngine instance,
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

	@WrapWithCondition(method = "renderLevel", at = @At(value = "INVOKE", remap = false, ordinal = 1,
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"))
	private boolean redirectRenderParticles1(ParticleEngine instance,
											 LightTexture lightTexture,
											 Camera camera,
											 float v,
											 Frustum frustum,
											 Predicate<ParticleRenderType> predicate,
											 @Local(ordinal = 0) float partialTick,
											 @Share(namespace = "asyncparticles", value = "isRenderAsync")
											 LocalBooleanRef isRenderAsync) {
		return !isRenderAsync.get();
	}
}

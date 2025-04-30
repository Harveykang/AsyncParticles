package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.iris_else;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * @implNote Suppressed if Iris mod loaded.
 */
@Mixin(value = LevelRenderer.class, priority = 1500) // After mixin.render.MixinLevelRenderer
public abstract class MixinLevelRenderer_Late {
	@WrapWithCondition(method = "renderLevel", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V"))
	private boolean redirectRenderParticles(ParticleEngine instance,
											LightTexture lightTexture,
											Camera camera,
											float partialTick,
											@Share(namespace = "asyncparticles", value = "isRenderAsync")
											LocalBooleanRef isRenderAsync) {
		if (!isRenderAsync.get()) {
			return true;
		}
		// assert !isMixedParticleRendering.get();
		if (ConfigHelper.isCompatibilityRendering()) {
			AsyncRenderer.join(partialTick, camera, lightTexture);
		}
		return false;
	}
}

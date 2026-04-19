package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.iris_else;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncRenderBehavior;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode.*;

/**
 * @implNote Suppressed if Iris mod loaded.
 */
@Mixin(LevelRenderer.class) // After mixin.render.MixinLevelRenderer
public abstract class MixinLevelRenderer {
	@Redirect(method = "renderLevel", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V"))
	private void redirectRenderParticles(ParticleEngine instance,
										 LightTexture lightTexture,
										 Camera camera,
										 float partialTick,
										 @Share(namespace = "asyncparticles", value = "internalRenderingMode")
										 LocalIntRef irm) {
		switch (irm.get()) {
			case SYNC -> AsyncRenderBehavior.getInstance().endAll(partialTick, camera, lightTexture, false);
			case COMPATIBILITY_ASYNC -> AsyncRenderBehavior.getInstance().endAll(partialTick, camera, lightTexture, true);
		}
	}
}

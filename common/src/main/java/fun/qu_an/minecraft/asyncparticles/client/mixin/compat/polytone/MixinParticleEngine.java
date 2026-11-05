package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.polytone;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.polytone.PolytoneParticleAddon;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ParticleEngine.class)
public abstract class MixinParticleEngine {
	@SuppressWarnings("MixinExtrasOperationParameters")
	@WrapMethod(method = "tickParticle")
	private void tickWithRenderState(Particle particle, Operation<Void> original) {
		if (particle instanceof PolytoneParticleAddon polytone && polytone.asyncparticles$shouldLockTickAndRender()) {
			synchronized (particle) {
				original.call(particle);
				((ParticleAddon) particle).asyncparticles$setTicked();
			}
		} else {
			original.call(particle);
		}
	}

	@SuppressWarnings("MixinExtrasOperationParameters")
	@Dynamic
	@WrapOperation(method = {
		"Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V",
		"Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"},
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
	private void renderWithTickState(Particle particle,
									 VertexConsumer buffer,
									 Camera camera,
									 float adjustedPartialTick,
									 Operation<Void> original,
									 @Local(argsOnly = true) float partialTick) {
		if (particle instanceof PolytoneParticleAddon polytone && polytone.asyncparticles$shouldLockTickAndRender()) {
			synchronized (particle) {
				float currentPartialTick = ((ParticleAddon) particle).asyncparticles$isTicked() ? partialTick : partialTick + 1f;
				original.call(particle, buffer, camera, currentPartialTick);
			}
		} else {
			original.call(particle, buffer, camera, adjustedPartialTick);
		}
	}
}

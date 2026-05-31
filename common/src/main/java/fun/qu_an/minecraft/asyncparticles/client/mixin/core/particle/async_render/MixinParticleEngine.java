
package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.async_render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_render.AsyncParticleRenderState;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_render.AsyncRenderBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_render.DeferredParticleRenderState;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ParticleEngine.class)
public abstract class MixinParticleEngine {
	/**
	 * @see MixinParticlesRenderState#onSubmit
	 */
	@WrapOperation(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleGroup;extractRenderState(Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/Camera;F)Lnet/minecraft/client/renderer/state/level/ParticleGroupRenderState;"))
	public ParticleGroupRenderState onExtractRenderState(ParticleGroup<? extends Particle> instance,
	                                                     Frustum frustum,
	                                                     Camera camera,
	                                                     float v,
	                                                     Operation<ParticleGroupRenderState> original) {
		DeferredParticleRenderState state = new DeferredParticleRenderState();
		AsyncRenderBehavior.submit(() -> {
			ParticleGroupRenderState renderState = original.call(instance, frustum, camera, v);
			if (renderState instanceof AsyncParticleRenderState asyncState) {
				asyncState.afterAdd();
			}
			state.setDelegate(renderState);
		});
		return state;
	}
}

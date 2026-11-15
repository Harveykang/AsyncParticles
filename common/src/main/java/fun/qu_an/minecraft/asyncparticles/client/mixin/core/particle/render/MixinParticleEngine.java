package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ParticleEngine.class)
public abstract class MixinParticleEngine {
	@WrapOperation(method = "extract", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/particle/ParticleGroup;extractRenderState(Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/Camera;F)Lnet/minecraft/client/renderer/state/level/ParticleGroupRenderState;"))
	private ParticleGroupRenderState asyncparticles$extractGroup(ParticleGroup<?> group, Frustum frustum, Camera camera,
	                                                            float partialTick, Operation<ParticleGroupRenderState> original) {
		try {
			return original.call(group, frustum, camera, partialTick);
		} catch (Exception e) {
			AsyncTickBehavior.getInstance().getExceptionHandler().onRenderParticleException(e);
			if (group instanceof GpuParticleGroup gpuGroup) {
				return gpuGroup.asyncparticles$clearCpuRenderState();
			}
			return (_, _) -> {};
		}
	}
}

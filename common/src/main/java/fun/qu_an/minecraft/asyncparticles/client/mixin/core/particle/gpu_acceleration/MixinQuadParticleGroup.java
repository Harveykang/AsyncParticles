package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuQuadParticleRenderState;
import fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.async_tick.MixinParticleGroup;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(QuadParticleGroup.class)
public abstract class MixinQuadParticleGroup extends MixinParticleGroup {
	@WrapOperation(method = "<init>", at = @At(value = "NEW",
		target = "()Lnet/minecraft/client/renderer/state/level/QuadParticleRenderState;"))
	private QuadParticleRenderState redirectNewQuadParticleRenderState(Operation<QuadParticleRenderState> original) {
		if ((ParticleGroup<?>) (Object) this instanceof GpuParticleGroup) {
			return new GpuQuadParticleRenderState((QuadParticleGroup) (Object) this);
		} else {
			return original.call();
		}
	}
}

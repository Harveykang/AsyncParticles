package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleGroup;
import net.minecraft.client.particle.ParticleGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Queue;

@Mixin(ParticleGroup.class)
public abstract class MixinParticleGroup {
	@WrapOperation(method = "isEmpty", at = @At(value = "INVOKE", target = "Ljava/util/Queue;isEmpty()Z"))
	private boolean wrapIsEmpty(Queue<?> instance, Operation<Boolean> original) {
		boolean call = original.call(instance);
		if (!(this instanceof GpuParticleGroup gpuParticleGroup)) {
			return call;
		}
		return gpuParticleGroup.asyncparticles$getGpuParticles().isEmpty() && call;
	}

	@WrapMethod(method = "size")
	private int wrapSize(Operation<Integer> original) {
		int call = original.call();
		if (!(this instanceof GpuParticleGroup gpuParticleGroup)) {
			return call;
		}
		return gpuParticleGroup.asyncparticles$getGpuParticles().size() + call;
	}
}

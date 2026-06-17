package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.SingleQuadParticle;
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

	@ModifyExpressionValue(method = "add", at = @At(value = "INVOKE", target = "Ljava/util/Queue;size()I"))
	private int injectAdd(int original) {
		if (!(this instanceof GpuParticleGroup gpuParticleGroup)) {
			return original;
		}
		return gpuParticleGroup.asyncparticles$getGpuParticles().size() + original;
	}

	@WrapOperation(method = "add", at = @At(value = "INVOKE", target = "Ljava/util/Queue;add(Ljava/lang/Object;)Z"))
	private boolean wrapAddToQueue(Queue<?> instance, Object particle, Operation<Boolean> original) {
		if (ConfigHelper.isGpuParticles()
			&& this instanceof GpuParticleGroup gpuParticleGroup
			&& particle instanceof SingleQuadParticle sqp
			&& GpuParticleBehavior.getInstance().canRenderFast(sqp)) {
			GpuParticleBehavior.getInstance().onAdd(sqp);
			return gpuParticleGroup.asyncparticles$getGpuParticles().add(sqp);
		}
		return original.call(instance, particle);
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

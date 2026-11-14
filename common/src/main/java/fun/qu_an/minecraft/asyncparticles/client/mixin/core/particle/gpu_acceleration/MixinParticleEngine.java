
package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.ParticleHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import net.minecraft.client.particle.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;

@Mixin(value = ParticleEngine.class, priority = 1500)
public abstract class MixinParticleEngine implements ParticleEngineAddon {
	@Shadow
	public Map<ParticleRenderType, Queue<Particle>> particles;

	@ModifyExpressionValue(method = "countParticles", at = @At(value = "INVOKE", target = "Ljava/util/stream/IntStream;sum()I"))
	private int modifyCount(int i) {
		return i + GpuParticleBehavior.getInstance().gpuParticles.values().stream().mapToInt(Collection::size).sum();
	}

	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/Queue;add(Ljava/lang/Object;)Z"))
	public boolean wrapAdd(Queue<?> instance, Object e, Operation<Boolean> original) {
		if (ConfigHelper.isGpuParticles()
			&& e instanceof TextureSheetParticle tsp
			&& GpuParticleBehavior.getInstance().canRenderFast(tsp)) {
			GpuParticleBehavior.getInstance().onAddGpu(tsp);
			GpuParticleBehavior.getInstance().gpuParticles.computeIfAbsent(tsp.getRenderType(), k -> ParticleHelper.newParticleQueue()).add(tsp);
		} else {
			original.call(instance, e);
		}
		return true;
	}
}


package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.IParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.particle.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.Queue;

@Mixin(value = ParticleEngine.class, priority = 1500)
public abstract class MixinParticleEngine implements ParticleEngineAddon {
	@Shadow
	@Final
	public Map<ParticleRenderType, ParticleGroup<?>> particles;

	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleGroup;add(Lnet/minecraft/client/particle/Particle;)V"))
	public void wrapAdd(ParticleGroup<?> group,
	                    Particle particle,
	                    Operation<Void> original) {
		if (ConfigHelper.isGpuParticles()
			&& group instanceof GpuParticleGroup gpuParticleGroup
			&& particle instanceof SingleQuadParticle sqp
			&& GpuParticleBehavior.getInstance().canRenderFast(sqp)) {
			GpuParticleBehavior.getInstance().onAdd(sqp);
			gpuParticleGroup.asyncparticles$getGpuParticles().add(sqp);
		} else {
			original.call(group, particle);
		}
	}

	@Inject(method = "tick", order = 500, at = @At("TAIL"))
	private void tickTail(CallbackInfo ci) {
		if (!ConfigHelper.isGpuParticles()) {
			return;
		}
		GpuParticleBehavior.getInstance().flushBufferAndSwap();
		int sum = 0;
		for (ParticleGroup<?> group : particles.values()) {
			if (group instanceof GpuParticleGroup gpuGroup) {
				sum += gpuGroup.asyncparticles$getGpuParticles().size();
			}
		}
		GpuParticleBehavior.getInstance().setUpNextTickRendering(sum);
		IParticleRenderer renderer = GpuParticleBehavior.getInstance().getOrCreateRenderer();
		renderer.prepareBuffer();
		AsyncTickBehavior.getInstance().getTickTaskManager().addTask(() -> {
			int size = Math.max(8, ConfigHelper.getParticleLimit() >> 1);
			Map<SingleQuadParticle.Layer, List<SingleQuadParticle>> particles = new Reference2ReferenceOpenHashMap<>();
			for (Map.Entry<ParticleRenderType, ParticleGroup<?>> entry : this.particles.entrySet()) {
				ParticleGroup<?> particleGroup = entry.getValue();
				if (!(particleGroup instanceof GpuParticleGroup gpuGroup)) {
					continue;
				}
				Queue<SingleQuadParticle> gpuParticles = gpuGroup.asyncparticles$getGpuParticles();
				if (gpuParticles.isEmpty()) {
					continue;
				}
				for (SingleQuadParticle sqp : gpuParticles) {
					particles.computeIfAbsent(sqp.getLayer(), _ -> new ReferenceArrayList<>(size)).add(sqp);
				}
			}
			renderer.tick(GpuParticleBehavior.getInstance().getPerTickCameraPos(), particles);
		});
	}
}

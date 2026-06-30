
package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.IParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.Queue;

@Mixin(value = ParticleEngine.class, priority = 500)
public abstract class MixinParticleEngine implements ParticleEngineAddon {
	@Shadow
	@Final
	public Map<ParticleRenderType, ParticleGroup<?>> particles;

	@Inject(method = "tick", at = @At("TAIL"))
	private void tick(CallbackInfo ci) {
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


package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.tick;

import fun.qu_an.minecraft.asyncparticles.client.addon.AsyncTickableParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleGroupAddition;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.IParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickParticleGroupBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleGroup;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.particle.*;
import net.minecraft.util.profiling.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

@Mixin(ParticleEngine.class)
public abstract class MixinParticleEngine implements ParticleEngineAddon {
	@Shadow
	@Final
	private Queue<TrackingEmitter> trackingEmitters;

	@Shadow
	@Final
	private Queue<Particle> particlesToAdd;

	@Shadow
	@Final
	public Map<ParticleRenderType, ParticleGroup<?>> particles;

	@Shadow
	protected abstract ParticleGroup<?> createParticleGroup(ParticleRenderType type);

	/**
	 * @author Harvey_Husky
	 * @reason Too many changes, need to rewrite the entire method.
	 */
	@Overwrite
	public void tick() {
		if (!AsyncTickBehavior.getInstance().shouldTickParticleEngine()) {
			return;
		}

		Particle particle;
		boolean tickAsync = ConfigHelper.isAsyncTickParticle();
		this.particles.forEach((renderType, group) -> {
			if (group.isEmpty()) {
				return;
			}
			Profiler.get().push(renderType.name());
			if (tickAsync && AsyncTickParticleGroupBehavior.canTickAsync(group)) {
				AsyncTickBehavior.getInstance().dispatch(group::tickParticles);
			} else {
				group.tickParticles();
			}
			Profiler.get().pop();
		});
		if (!this.trackingEmitters.isEmpty()) {
			for (TrackingEmitter trackingEmitter : this.trackingEmitters) {
				trackingEmitter.tick(); // TODO can be async-lized safely?
			}
		}
		if (!tickAsync) {
			particles.values().forEach(g -> ((ParticleGroupAddition) g).asyncparticles$removeDeadParticles());
			AsyncTickBehavior.getInstance().doEmittersRemoveIf(trackingEmitters);
		}

		boolean enableGpu = tickAsync && ConfigHelper.isGpuParticles();
		if (!particlesToAdd.isEmpty()) {
			boolean appendNewParticlesToRenderer = ConfigHelper.isAppendNewParticlesToRenderer();
			// Write like this to be compatible with e.g. Spectrum mod
			//noinspection ForLoopReplaceableByForEach
			for (Iterator<Particle> iterator = particlesToAdd.iterator(); iterator.hasNext(); ) {
				particle = iterator.next();
				ParticleRenderType renderType = particle.getGroup();
				ParticleGroup<?> group = this.particles.computeIfAbsent(renderType, this::createParticleGroup);
				if (enableGpu
					&& group instanceof GpuParticleGroup gpuParticleGroup
					&& particle instanceof SingleQuadParticle sqp
					&& GpuParticleBehavior.getInstance().canRenderFast(sqp)) {
					GpuParticleBehavior.getInstance().onAdd(sqp);
					gpuParticleGroup.asyncparticles$getGpuParticles().add(sqp);
				} else {
					group.add(particle);
				}
			}
			particlesToAdd.clear();
		}

		if (enableGpu) {
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
			AsyncTickBehavior.getInstance().dispatch(() -> {
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

	@Override
	public void asyncparticle$tickSyncParticles() {
		particles.values().forEach(AsyncTickParticleGroupBehavior::tickSyncParticles);
	}
}


package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.tick;

import fun.qu_an.minecraft.asyncparticles.client.addon.AsyncTickableParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleGroupAddition;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.render.IParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickParticleGroupBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.ParticleLimit;
import net.minecraft.util.profiling.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.*;

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

	@Shadow
	protected abstract void updateCount(ParticleLimit limit, int change);

	/**
	 * @author Harvey_Husky
	 * @reason Too many changes, need to rewrite the entire method.
	 */
	@Overwrite
	public void tick() {
		if (!AsyncTickBehavior.getInstance().shouldTickParticleEngine()) {
			return;
		}

		// Keep local var table as they were
		Particle particle;
		boolean tickAsync = ConfigHelper.isAsyncTickParticle();
		this.particles.forEach((renderType, group) -> {
			if (group.isEmpty()) {
				return;
			}
			Profiler.get().push(renderType.name());
			if (tickAsync && group instanceof AsyncTickableParticleGroup
				&& AsyncTickParticleGroupBehavior.canTickAsync(group)) {
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

		if (!particlesToAdd.isEmpty()) {
			//noinspection ForLoopReplaceableByForEach
			for (Iterator<Particle> iterator = particlesToAdd.iterator(); iterator.hasNext(); ) {
				particle = iterator.next();
				if (!this.particles.computeIfAbsent(particle.getGroup(), this::createParticleGroup).add(particle)) {
					particle.getParticleLimit().ifPresent(options -> this.updateCount(options, -1));
				}
			}
			particlesToAdd.clear();
		}

		if (ConfigHelper.isGpuParticles()) {
			GpuParticleBehavior.getInstance().swapAllBuffers();
			GpuParticleBehavior.getInstance().setUpNextTickRendering(ConfigHelper.getParticleLimit());
			IParticleRenderer renderer = GpuParticleBehavior.getInstance().getOrCreateRenderer(ParticleRenderType.SINGLE_QUADS);
			renderer.mapBuffer();
			AsyncTickBehavior.getInstance().dispatch(() -> {
				Map<SingleQuadParticle.Layer, Collection<SingleQuadParticle>> particles = new Reference2ReferenceOpenHashMap<>();
				for (Map.Entry<ParticleRenderType, ParticleGroup<?>> entry : this.particles.entrySet()) {
					ParticleGroup<?> particleGroup = entry.getValue();
					if (!(particleGroup instanceof GpuParticleGroup gpuParticleGroup)) {
						continue;
					}
					Queue<SingleQuadParticle> gpuParticles = gpuParticleGroup.asyncparticles$getGpuParticles();
					if (gpuParticles.isEmpty()) {
						continue;
					}
					int size = Math.max(8, gpuParticles.size() >> 1);
					for (SingleQuadParticle tsp : gpuParticles) {
						particles.computeIfAbsent(tsp.getLayer(), _ -> new ReferenceArrayList<>(size)).add(tsp);
					}
				}
				renderer.tick(GpuParticleBehavior.getInstance().getCameraPos(), particles);
			});

		}
	}

	@Override
	public void asyncparticle$tickSyncParticles() {
		particles.values().forEach(g -> {
			if (g instanceof AsyncTickableParticleGroup asyncGroup) {
				asyncGroup.asyncparticles$tickSyncParticles();
			}
		});
	}
}


package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.tick;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleGroupAddition;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.TaskHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickParticleGroupBehavior;
import net.minecraft.client.particle.*;
import net.minecraft.util.profiling.Profiler;
import org.spongepowered.asm.mixin.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.function.BiConsumer;

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

		// Keep local var table as they were
		Particle particle;
		boolean tickAsync = ConfigHelper.isAsyncTickParticle();
		if (tickAsync) {
			TaskHelper taskHelper = AsyncTickBehavior.getInstance().getTickTaskManager();
			asyncparticles$forEach(this.particles, (renderType, group) -> {
				if (group.isEmpty()) {
					return;
				}
				Profiler.get().push(renderType.name());
				if (AsyncTickParticleGroupBehavior.canTickAsync(group)) {
					taskHelper.addTask(((ParticleGroupAddition) group)::asyncparticles$asyncTickParticles);
				} else {
					group.tickParticles();
				}
				Profiler.get().pop();
			});
			taskHelper.groupTasks(true);
		} else {
			// forEach is an inject point (eg. ParticleCore)
			this.particles.forEach((type, group) -> {
				Profiler.get().push(type.name());
				group.tickParticles();
				Profiler.get().pop();
			});
		}
		if (!this.trackingEmitters.isEmpty()) {
			for (TrackingEmitter trackingEmitter : this.trackingEmitters) {
				trackingEmitter.tick(); // TODO can be async-lized safely?
				// clear in AsyncTickBehavior
			}
		}
		if (!tickAsync) {
			AsyncTickBehavior.getInstance().doEmittersRemoveIf(trackingEmitters);
		}

		if (!particlesToAdd.isEmpty()) {
			// Write like this to be compatible with e.g. Spectrum mod
			//noinspection ForLoopReplaceableByForEach
			for (Iterator<Particle> iterator = particlesToAdd.iterator(); iterator.hasNext(); ) {
				particle = iterator.next();
				ParticleRenderType renderType = particle.getGroup();
				ParticleGroup<?> group = this.particles.computeIfAbsent(renderType, this::createParticleGroup);
				group.add(particle);
			}
			particlesToAdd.clear();
		}
	}

	@Unique
	private <K, V> void asyncparticles$forEach(Map<K, V> i, BiConsumer<K, V> f) {
		i.forEach(f);
	}

	@Override
	public void asyncparticle$tickSyncParticles() {
		particles.values().forEach(AsyncTickParticleGroupBehavior::tickSyncParticles);
	}
}

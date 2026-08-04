
package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.tick;

import fun.qu_an.minecraft.asyncparticles.client.addon.*;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.TaskHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import net.minecraft.client.particle.*;
import net.minecraft.util.profiling.Profiler;
import org.spongepowered.asm.mixin.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
		// Keep local var table as they were
		Particle particle;
		AsyncTickBehavior tickBehavior = AsyncTickBehavior.getInstance();
		boolean tickAsync = ConfigHelper.isAsyncParticleTick() && tickBehavior.isParticlePhase();
		boolean asyncAll = tickAsync && !ConfigHelper.isGpuOnlyAsyncParticleTick();
		if (asyncAll) {
			TaskHelper taskHelper = tickBehavior.getTickTaskManager();
			asyncparticles$forEach(this.particles, (renderType, group) -> {
				if (group.isEmpty()) {
					return;
				}
				Profiler.get().push(renderType.name());
				if (((ParticleGroupAddition) group).asyncparticles$canTickAsync()) {
					((AsyncTickableParticleGroup) group).asyncparticles$tickSyncParticles(false);
					taskHelper.addTask(() -> ((ParticleGroupAddition) group).asyncparticles$tickParticles(false));
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
				trackingEmitter.tick();
				// clear in AsyncTickBehavior
			}
			if (!tickAsync) {
				tickBehavior.doEmittersRemoveIf(trackingEmitters);
			}
		}

		if (!particlesToAdd.isEmpty()) {
			// Write like this to be compatible with e.g. Spectrum mod
			//noinspection ForLoopReplaceableByForEach
			for (Iterator<Particle> iterator = particlesToAdd.iterator(); iterator.hasNext(); ) {
				particle = iterator.next();
				ParticleGroup<?> group = this.particles.computeIfAbsent(particle.getGroup(), type -> {
					ParticleGroup<?> particleGroup = createParticleGroup(type);
					if (asyncAll && ((ParticleGroupAddition) particleGroup).asyncparticles$canTickAsync()) {
						AsyncTickBehavior.getInstance().getTickTaskManager()
							.addTask(() -> ((ParticleGroupAddition) particleGroup).asyncparticles$tickParticles(false));
					}
					return particleGroup;
				});
				// add to sync queue only if async tick enabled, gpu only disabled, and particle class is sync
				if (tickAsync
					&& ((ParticleGroupAddition) group).asyncparticles$canTickAsync()
//					&& ConfigHelper.isAsyncTickParticle() // tested in asyncparticles$canTickAsync()
					&& tickBehavior.shouldSync(((ParticleAddon) particle).asyncparticles$getRealClass())) {
					if (GpuParticleBehavior.getInstance().canRenderFast(particle)) {
						((AsyncTickableParticleGroup) group).asyncparticles$addSyncGpuParticle(particle);
					} else if (asyncAll) {
						((AsyncTickableParticleGroup) group).asyncparticles$addSyncParticle(particle);
					}
				}
				group.add(particle);
			}
			particlesToAdd.clear();
		}
	}

	@Unique
	private static  <K, V> void asyncparticles$forEach(Map<K, V> i, BiConsumer<K, V> f) {
		i.forEach(f);
	}

	@Unique
	private static  <E> void asyncparticles$forEach(Iterable<E> i, Consumer<E> f) {
		i.forEach(f);
	}
}

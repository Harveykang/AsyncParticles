
package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.tick;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleGroupAddition;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.TaskHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickParticleGroupBehavior;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.ParticleLimit;
import net.minecraft.util.profiling.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
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
		TaskHelper taskHelper = AsyncTickBehavior.getInstance().getTickTaskManager();
		this.particles.forEach((renderType, group) -> {
			if (group.isEmpty()) {
				return;
			}
			Profiler.get().push(renderType.name());
			if (tickAsync && AsyncTickParticleGroupBehavior.canTickAsync(group)) {
				taskHelper.addTask(group::tickParticles);
			} else {
				group.tickParticles();
			}
			Profiler.get().pop();
		});
		taskHelper.groupTasks(true);
		if (!this.trackingEmitters.isEmpty()) {
			for (TrackingEmitter trackingEmitter : this.trackingEmitters) {
				trackingEmitter.tick(); // TODO can be async-lized safely?
				// clear in AsyncTickBehavior
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
	}

	@Override
	public void asyncparticle$tickSyncParticles() {
		particles.values().forEach(AsyncTickParticleGroupBehavior::tickSyncParticles);
	}
}

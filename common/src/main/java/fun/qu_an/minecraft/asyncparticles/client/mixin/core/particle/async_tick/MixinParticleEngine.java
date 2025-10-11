
package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.async_tick;

import com.google.common.collect.Lists;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_tick.AsyncTickParticleGroupBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_tick.AsyncTickableParticleGroup;
import net.minecraft.client.particle.*;
import net.minecraft.util.profiling.Profiler;
import org.spongepowered.asm.mixin.*;

import java.util.List;
import java.util.Map;
import java.util.Queue;

@Mixin(ParticleEngine.class)
public abstract class MixinParticleEngine {
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
	protected abstract ParticleGroup<?> createParticleGroup(ParticleRenderType particleRenderType);

	/**
	 * @author Harvey_Husky
	 * @reason Too many changes, need to rewrite the entire method.
	 */
	@Overwrite
	public void tick() {
		this.particles.forEach((particleRenderType, particleGroup) -> {
			Profiler.get().push(particleRenderType.name());
			if (particleGroup instanceof AsyncTickableParticleGroup &&
				AsyncTickParticleGroupBehavior.canTickAsync(particleGroup)){
				AsyncTickBehavior.dispatch(particleGroup::tickParticles);
			} else {
				particleGroup.tickParticles();
			}
			Profiler.get().pop();
		});
		if (!this.trackingEmitters.isEmpty()) {
			List<TrackingEmitter> list = Lists.newArrayList();

			for (TrackingEmitter trackingEmitter : this.trackingEmitters) {
				trackingEmitter.tick();
				if (!trackingEmitter.isAlive()) {
					list.add(trackingEmitter);
				}
			}

			this.trackingEmitters.removeAll(list);
		}

		Particle particle;
		if (!this.particlesToAdd.isEmpty()) {
			while ((particle = this.particlesToAdd.poll()) != null) {
				this.particles.computeIfAbsent(particle.getGroup(), this::createParticleGroup).add(particle);
			}
		}
	}
}

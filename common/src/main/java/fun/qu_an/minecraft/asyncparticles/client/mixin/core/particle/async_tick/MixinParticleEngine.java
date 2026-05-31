
package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.async_tick;

import com.google.common.collect.Lists;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.ParticleHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_tick.AsyncTickParticleGroupBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_tick.AsyncTickableParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.util.profiling.Profiler;
import org.spongepowered.asm.mixin.*;

import java.util.Iterator;
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
		Particle particle;
		boolean tickAsync = ConfigHelper.isTickAsync();
		this.particles.forEach((particleRenderType, particleGroup) -> {
			Profiler.get().push(particleRenderType.name());
			if (!tickAsync
				|| !(particleGroup instanceof AsyncTickableParticleGroup)
				|| !AsyncTickParticleGroupBehavior.canTickAsync(particleGroup)) {
				particleGroup.tickParticles();
			} else {
				AsyncTickBehavior.dispatch(particleGroup::tickParticles);
			}
			Profiler.get().pop();
		});
		if (!this.trackingEmitters.isEmpty()) {
			List<TrackingEmitter> list = Lists.newArrayList();

			for (TrackingEmitter trackingEmitter : this.trackingEmitters) {
				trackingEmitter.tick(); // TODO can be async-lized safely?
				if (!trackingEmitter.isAlive()) {
					list.add(trackingEmitter);
				}
			}

			this.trackingEmitters.removeAll(list);
		}

		boolean gpuParticles = ConfigHelper.isGpuParticles();
		if (!particlesToAdd.isEmpty()) {
			boolean appendNewParticlesToRenderer = ConfigHelper.isAppendNewParticlesToRenderer();
			// Write like this to be compatible with e.g. Spectrum mod
			//noinspection ForLoopReplaceableByForEach
			for (Iterator<Particle> iterator = particlesToAdd.iterator(); iterator.hasNext(); ) {
				particle = iterator.next();
				boolean canComputeFast;
				if (!gpuParticles || !tickAsync) {
					canComputeFast = false;
				} else if (((ParticleAddon) particle).asyncparticles$isTickSync()) {
					AsyncTickBehavior.recordSync(particle);
					canComputeFast = false;
				} else {
					canComputeFast = particle instanceof SingleQuadParticle tsp && GpuParticleBehavior.INSTANCE.canRenderFast(tsp);
				}
				ParticleGroup<?> group;
				ParticleRenderType renderType = particle.getGroup();
				if (!canComputeFast) {
					group = this.particles.computeIfAbsent(renderType, this::createParticleGroup);
				} else {
					group = this.particles.computeIfAbsent(renderType, k -> {
						GpuParticleBehavior.INSTANCE.createRenderer(k);
						return createParticleGroup(k);
					});
					if (appendNewParticlesToRenderer) {
						GpuParticleBehavior.INSTANCE.getRenderer(renderType).append(GpuParticleBehavior.INSTANCE.getCameraPos(), ((SingleQuadParticle) particle));
					}
				}
				group.add(particle);
			}
			particlesToAdd.clear();
		}
		if (tickAsync) {
			if (gpuParticles) {
				GpuParticleBehavior.INSTANCE.swapAllBuffers();
				GpuParticleBehavior.INSTANCE.setGpuParticleLimit(ConfigHelper.getParticleLimit());
				Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
				GpuParticleBehavior.INSTANCE.setCameraPos(camera.position());
			}
		}
	}
}


package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.async_tick;

import com.google.common.collect.Lists;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleGroupAddition;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_tick.AsyncTickParticleGroupBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_tick.AsyncTickableParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_tick.GpuParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.IParticleRenderer;
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
	protected abstract ParticleGroup<?> createParticleGroup(ParticleRenderType type);

	/**
	 * @author Harvey_Husky
	 * @reason Too many changes, need to rewrite the entire method.
	 */
	@Overwrite
	public void tick() {

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

		Particle particle;
		boolean tickAsync = ConfigHelper.isTickAsync();
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
					group = GpuParticleBehavior.INSTANCE.gpuParticles.computeIfAbsent(renderType, k -> {
						GpuParticleBehavior.INSTANCE.createRenderer(k);
						return new GpuParticleGroup((ParticleEngine) (Object) this, renderType);
					});
					if (appendNewParticlesToRenderer) {
						GpuParticleBehavior.INSTANCE.getRenderer(renderType).append(GpuParticleBehavior.INSTANCE.getCameraPos(), ((SingleQuadParticle) particle));
					}
				}
				group.add(particle);
			}
			particlesToAdd.clear();
		}
		if (tickAsync && gpuParticles) {
			GpuParticleBehavior.INSTANCE.swapAllBuffers();
			GpuParticleBehavior.INSTANCE.setGpuParticleLimit(ConfigHelper.getParticleLimit());
			Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
			GpuParticleBehavior.INSTANCE.setCameraPos(camera.position());
			GpuParticleBehavior.INSTANCE.gpuParticles.forEach(((renderType, group) -> {
				if (group.isEmpty()) {
					return;
				}
				Profiler.get().push(renderType.name());
				IParticleRenderer renderer = GpuParticleBehavior.INSTANCE.getRenderer(renderType);
				renderer.mapBuffer();
				AsyncTickBehavior.dispatch(() -> {
					group.tickParticles();
					((ParticleGroupAddition) group).asyncparticles$cleanUp();
					renderer.tick(GpuParticleBehavior.INSTANCE.getCameraPos(), group.getAll());
				});
				Profiler.get().pop();
			}));
		}

		this.particles.forEach((renderType, group) -> {
			if (group.isEmpty()) {
				return;
			}
			Profiler.get().push(renderType.name());
			if (!tickAsync
				|| !(group instanceof AsyncTickableParticleGroup)
				|| !AsyncTickParticleGroupBehavior.canTickAsync(group)) {
				group.tickParticles();
			} else {
				AsyncTickBehavior.dispatch(group::tickParticles);
			}
			Profiler.get().pop();
		});
	}
}

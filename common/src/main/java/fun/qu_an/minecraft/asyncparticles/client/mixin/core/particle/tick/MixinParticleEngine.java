
package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.tick;

import com.google.common.collect.Lists;
import fun.qu_an.minecraft.asyncparticles.client.addon.AsyncTickableParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickParticleGroupBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleGroup;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
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
		boolean gpuParticles = tickAsync && ConfigHelper.isGpuParticles();
		if (!particlesToAdd.isEmpty()) {
			boolean appendNewParticlesToRenderer = ConfigHelper.isAppendNewParticlesToRenderer();
			// Write like this to be compatible with e.g. Spectrum mod
			//noinspection ForLoopReplaceableByForEach
			for (Iterator<Particle> iterator = particlesToAdd.iterator(); iterator.hasNext(); ) {
				particle = iterator.next();
				boolean canComputeFast;
				if (!gpuParticles) {
					canComputeFast = false;
				} else {
					canComputeFast = particle instanceof SingleQuadParticle sqp && GpuParticleBehavior.getInstance().canRenderFast(sqp);
				}
				ParticleRenderType renderType = particle.getGroup();
				ParticleGroup<?> group;
				if (!canComputeFast) {
					group = this.particles.computeIfAbsent(renderType, this::createParticleGroup);
				} else {
					ParticleRenderType gpuRenderType = GpuParticleBehavior.getInstance().ofRenderType(renderType);
					group = this.particles.computeIfAbsent(gpuRenderType, gpuRenderType1 ->
						GpuParticleBehavior.getInstance().gpuParticles.computeIfAbsent(gpuRenderType1,
							gpuRenderType2 -> {
								asyncparticle$addRenderType(gpuRenderType2);
								return new GpuParticleGroup((ParticleEngine) (Object) this, gpuRenderType2);
							}));
				}
				group.add(particle);
			}
			particlesToAdd.clear();
		}
		if (gpuParticles) {
			GpuParticleBehavior.getInstance().swapAllBuffers();
			GpuParticleBehavior.getInstance().setGpuParticleLimit(ConfigHelper.getParticleLimit());
			Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
			GpuParticleBehavior.getInstance().setCameraPos(camera.position()); // Set the next camera position
			GpuParticleBehavior.getInstance().gpuParticles.forEach(((renderType, gpuGroup) -> {
				if (gpuGroup.isEmpty()) {
					return;
				}
				Profiler.get().push(renderType.name());
				gpuGroup.mapBuffers();
				AsyncTickBehavior.getInstance().dispatch(gpuGroup::tickParticles);
				Profiler.get().pop();
			}));
		}

		this.particles.forEach((renderType, group) -> {
			if (group.isEmpty() || group instanceof GpuParticleGroup) {
				return;
			}
			Profiler.get().push(renderType.name());
			if (!tickAsync
				|| !(group instanceof AsyncTickableParticleGroup)
				|| !AsyncTickParticleGroupBehavior.canTickAsync(group)) {
				group.tickParticles();
			} else {
				AsyncTickBehavior.getInstance().dispatch(group::tickParticles);
			}
			Profiler.get().pop();
		});
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

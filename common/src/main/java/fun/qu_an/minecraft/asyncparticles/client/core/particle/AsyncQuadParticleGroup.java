package fun.qu_an.minecraft.asyncparticles.client.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.core.particle.render.AsyncQuadParticleRenderState;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;
import org.jetbrains.annotations.NotNull;

public class AsyncQuadParticleGroup extends QuadParticleGroup {
	public AsyncQuadParticleGroup(ParticleEngine particleEngine, ParticleRenderType particleRenderType) {
		super(particleEngine, particleRenderType);
	}

	private static final boolean renderParallel = false;

//	public boolean isEmpty() {
//		return this.particles.isEmpty();
//	}
//
//	public void tickParticles() {
////		assert ThreadUtil.isOnParticleTickerThread();
//		boolean enableLightCache = ConfigHelper.isParticleLightCache();
//		ParticleCullingMode particleCullingMode = ConfigHelper.getParticleCullingMode();
//		boolean forceDone = ConfigHelper.forceDoneParticleTick();
//		for (Particle particle : particles) {
//			if (AsyncTickBehavior.isCancelled() && !forceDone) {
//				return;
//			}
//			if (((ParticleAddon) particle).asyncparticles$isTicked()) {
//				// Skip the first tick that the particle is added to the queue.
//				if (enableLightCache) {
//					((LightCachedParticleAddon) particle).asyncparticles$refresh();
//				}
//				switch (particleCullingMode) {
//					case ASYNC_AABB -> ((ParticleAddon) particle).asyncparticles$tickAABBCulling();
//					case ASYNC_SPHERE -> ((ParticleAddon) particle).asyncparticles$tickSphereCulling();
//				}
//				continue;
//			}
//			if (((ParticleAddon) particle).asyncparticles$isTickSync()) {
////					AsyncTickBehavior.recordSync(particle);
//				continue;
//			}
//			try {
//				tickParticle(particle);
//			} catch (Throwable t) {
//				AsyncTickBehavior.onTickingParticleException(particle, t);
//			}
//			((ParticleAddon) particle).asyncparticles$setTicked();
//			if (enableLightCache) {
//				((LightCachedParticleAddon) particle).asyncparticles$refresh();
//			}
//			switch (particleCullingMode) {
//				case ASYNC_AABB -> ((ParticleAddon) particle).asyncparticles$tickAABBCulling();
//				case ASYNC_SPHERE -> ((ParticleAddon) particle).asyncparticles$tickSphereCulling();
//			}
//		}
//	}
//
//	public void cleanUp() {
//		if (ConfigHelper.isParallelQueueRemoval()) {
//			((IterationSafeEvictingQueue<? extends Particle>) particles)
//				.parallelRemoveIf(particle ->
//						AsyncTickBehavior.shouldRemove(particle, ConfigHelper.isRemoveIfMissedTick()),
//					ConfigHelper.isParallelQueueEviction(),
//					AsyncTickBehavior.THREADS,
//					AsyncTickBehavior.EXECUTOR);
//		} else {
//			particles.removeIf(particle ->
//				AsyncTickBehavior.shouldRemove(particle, ConfigHelper.isRemoveIfMissedTick()));
//		}
//	}
//
//	private void tickParticle(Particle particle) {
//		try {
//			particle.tick();
//		} catch (Throwable var5) {
//			CrashReport crashReport = CrashReport.forThrowable(var5, "Ticking Particle");
//			CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being ticked");
//			crashReportCategory.setDetail("Particle", particle::toString);
//			crashReportCategory.setDetail("Particle Type", particle.getGroup()::toString);
//			throw new ReportedException(crashReport);
//		}
//	}
//
//	public void add(SingleQuadParticle particle) {
//		this.particles.add(particle);
//	}
//
//	public int size() {
//		return this.particles.size();
//	}
//
//	public Queue<SingleQuadParticle> getAll() {
//		return this.particles;
//	}

	@Override
	public @NotNull ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float f) {
		super.extractRenderState(frustum, camera, f);
		getRenderState().afterAdd();
		return getRenderState();
	}

	public AsyncQuadParticleRenderState getRenderState() {
		return (AsyncQuadParticleRenderState) particleTypeRenderState;
	}

	public ParticleRenderType getParticleType() {
		return particleType;
	}
}

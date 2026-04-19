package fun.qu_an.minecraft.asyncparticles.client.particle;

import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.ParticleCullingMode;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;

import java.util.Spliterator;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

public class TickParticleRecursiveAction extends RecursiveAction {
	private static final int MAX_DEPTH = (int) Math.round(Math.log(HashCommon.nextPowerOfTwo(AsyncTickBehavior.INSTANCE.THREADS)) / Math.log(2)) + 2;
	private final Spliterator<Particle> spliterator;
	private final int depth;

	public TickParticleRecursiveAction(Spliterator<Particle> spliterator) {
		this(spliterator, 0);
	}

	private TickParticleRecursiveAction(Spliterator<Particle> spliterator, int depth) {
		this.spliterator = spliterator;
		this.depth = depth;
	}

	@Override
	public void compute() {
		Spliterator<Particle> sub;
		if (spliterator.estimateSize() > 192 && depth < MAX_DEPTH && (sub = spliterator.trySplit()) != null) {
			ForkJoinTask<Void> left = new TickParticleRecursiveAction(sub, depth + 1).fork();
			ForkJoinTask<Void> right = new TickParticleRecursiveAction(spliterator, depth + 1).fork();
			left.join();
			right.join();
		} else {
			boolean enableLightCache = ConfigHelper.particleLightCache();
			ParticleCullingMode particleCullingMode = ConfigHelper.getParticleCullingMode();
			boolean forceDone = ConfigHelper.forceDoneParticleTick();
			ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
			spliterator.forEachRemaining(particle -> {
				if (AsyncTickBehavior.INSTANCE.isCancelled() && !forceDone) {
					return;
				}
				if (!((ParticleAddon) particle).asyncparticles$isTicked()) {
					if (((ParticleAddon) particle).asyncparticles$isTickSync()) {
						AsyncTickBehavior.INSTANCE.recordSync(particle);
						return;
					}
					try {
						particleEngine.tickParticle(particle);
					} catch (Throwable t) {
						AsyncTickBehavior.INSTANCE.onTickingParticleException(particle, t);
					}
					((ParticleAddon) particle).asyncparticles$setTicked();
				}
				if (enableLightCache) {
					((LightCachedParticleAddon) particle).asyncparticles$refresh();
				}
				switch (particleCullingMode) {
					case ASYNC_AABB -> ((ParticleAddon) particle).asyncparticles$tickAABBCulling();
					case ASYNC_SPHERE -> ((ParticleAddon) particle).asyncparticles$tickSphereCulling();
				}
			});
		}
	}
}

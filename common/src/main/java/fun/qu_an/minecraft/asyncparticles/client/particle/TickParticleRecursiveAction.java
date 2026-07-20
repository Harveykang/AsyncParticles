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
	private static final int MAX_DEPTH = (int) Math.round(Math.log(HashCommon.nextPowerOfTwo(AsyncTickBehavior.THREADS)) / Math.log(2)) + 2;
	private final Spliterator<Particle> spliterator;
	private final int depth;
	private final boolean isGpu;

	public TickParticleRecursiveAction(Spliterator<Particle> spliterator, boolean isGpu) {
		this(spliterator, 0, isGpu);
	}

	private TickParticleRecursiveAction(Spliterator<Particle> spliterator, int depth, boolean isGpu) {
		this.spliterator = spliterator;
		this.depth = depth;
		this.isGpu = isGpu;
	}

	@Override
	public void compute() {
		Spliterator<Particle> sub;
		if (spliterator.estimateSize() > 192 && depth < MAX_DEPTH && (sub = spliterator.trySplit()) != null) {
			ForkJoinTask<Void> left = new TickParticleRecursiveAction(sub, depth + 1, isGpu).fork();
			ForkJoinTask<Void> right = new TickParticleRecursiveAction(spliterator, depth + 1, isGpu).fork();
			left.join();
			right.join();
		} else {
			ParticleCullingMode particleCullingMode = isGpu ?
				ParticleCullingMode.DISABLED :
				ConfigHelper.getParticleCullingMode();
			boolean forceDone = ConfigHelper.forceDoneParticleTick();
			ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
			spliterator.forEachRemaining(particle -> {
				if (AsyncTickBehavior.getInstance().isCancelled() && !forceDone) {
					return;
				}
				if (!particle.isAlive()) {
					return;
				}
				ParticleAddon particleAddon = (ParticleAddon) particle;
				boolean shouldTick;
				if (particleAddon.asyncparticles$isTicked()) {
					// Skip the first tick after enqueued that the particle is added to the queue.
					shouldTick = isGpu;
				} else if (particleAddon.asyncparticles$isTickSync()) {
					AsyncTickBehavior.getInstance().recordSync(particle);
					return;
				} else {
					shouldTick = true;
				}
				if (shouldTick) {
					try {
						particleEngine.tickParticle(particle);
					} catch (Throwable t) {
						if (TickExceptionHandler.getInstance().onTickingParticleException(particle, t)) {
							return;
						}
					}
					particleAddon.asyncparticles$setTicked();
				}
				((LightCachedParticleAddon) particle).asyncparticles$tickLightCache();
				switch (particleCullingMode) {
					case ASYNC_AABB -> particleAddon.asyncparticles$tickAABBCulling();
					case ASYNC_SPHERE -> particleAddon.asyncparticles$tickSphereCulling();
				}
			});
		}
	}
}

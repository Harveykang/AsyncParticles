package fun.qu_an.minecraft.asyncparticles.client.core.particle.tick;

import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.util.Utils;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;

import java.util.Spliterator;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

public class TickParticleRecursiveAction<T extends Particle> extends RecursiveAction {
	private static final int MAX_DEPTH = (int) Math.round(Math.log(HashCommon.nextPowerOfTwo(AsyncTickBehavior.THREADS)) / Math.log(2)) + 2;
	private final ParticleEngine engine;
	private final Spliterator<T> spliterator;
	private final int depth;
	private final boolean isGpu;

	private TickParticleRecursiveAction(ParticleEngine engine, Spliterator<T> spliterator, int depth, boolean isGpu) {
		this.engine = engine;
		this.spliterator = spliterator;
		this.depth = depth;
		this.isGpu = isGpu;
	}

	public static <T extends Particle> void execute(ParticleEngine engine, Spliterator<T> spliterator, boolean isGpu) {
		new TickParticleRecursiveAction<>(engine, spliterator, 0, isGpu).compute();
	}

	@Override
	public void compute() {
		Spliterator<T> sub;
		if (spliterator.estimateSize() > 192 && depth < MAX_DEPTH && (sub = spliterator.trySplit()) != null) {
			ForkJoinTask<Void> left = new TickParticleRecursiveAction<>(engine, sub, depth + 1, isGpu).fork();
			ForkJoinTask<Void> right = new TickParticleRecursiveAction<>(engine, spliterator, depth + 1, isGpu).fork();
			left.join();
			right.join();
		} else {
			spliterator.forEachRemaining(particle -> {
				if (!particle.isAlive()) {
					// This is to be compatible with e.g. Figura mod
					// Trust JIT
					Utils.DUMMY_ITERATOR.remove();
					return;
				}
				ParticleAddon particleAddon = (ParticleAddon) particle;
				boolean isSyncParticle = AsyncTickBehavior.getInstance().isSyncParticle(particle);
				// Skip the first tick after the particle is added to the queue.
				// GPU particles don't skip the first tick, but skip the first refresh.
				// skip the first refresh will fix black destruction gpu particles.
				boolean shouldTick = !isSyncParticle && (!particleAddon.asyncparticles$isTicked() || isGpu);
				if (shouldTick) {
					try {
						engine.tickParticle(particle);
					} catch (Throwable t) {
						if (AsyncTickBehavior.getInstance().getExceptionHandler().onTickParticleException(particle, t)) {
							return;
						}
					}
					particleAddon.asyncparticles$setTicked();
				}
				if (!isSyncParticle) {
					((LightCachedParticleAddon) particle).asyncparticles$tickLightCache();
				}
			});
		}
	}
}

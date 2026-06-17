package fun.qu_an.minecraft.asyncparticles.client.core.particle.tick;

import fun.qu_an.minecraft.asyncparticles.client.addon.AsyncTickableParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.Utils;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.ReportedException;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;

import java.util.Spliterator;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

public class TickParticleRecursiveAction<T extends Particle> extends RecursiveAction {
	private static final int MAX_DEPTH = (int) Math.round(Math.log(HashCommon.nextPowerOfTwo(AsyncTickBehavior.THREADS)) / Math.log(2)) + 2;
	private final ParticleGroup<?> group;
	private final Spliterator<T> spliterator;
	private final int depth;
	private final boolean isGpu;

	public TickParticleRecursiveAction(ParticleGroup<?> group, Spliterator<T> spliterator, boolean isGpu) {
		this(group, spliterator, 0, isGpu);
	}

	private TickParticleRecursiveAction(ParticleGroup<?> group, Spliterator<T> spliterator, int depth, boolean isGpu) {
		this.group = group;
		this.spliterator = spliterator;
		this.depth = depth;
		this.isGpu = isGpu;
	}

	@Override
	public void compute() {
		Spliterator<T> sub;
		if (spliterator.estimateSize() > 192 && depth < MAX_DEPTH && (sub = spliterator.trySplit()) != null) {
			ForkJoinTask<Void> left = new TickParticleRecursiveAction<>(group, sub, depth + 1, isGpu).fork();
			ForkJoinTask<Void> right = new TickParticleRecursiveAction<>(group, spliterator, depth + 1, isGpu).fork();
			left.join();
			right.join();
		} else {
			boolean enableLightCache = ConfigHelper.particleLightCache();
			spliterator.forEachRemaining(particle -> {
				if (!particle.isAlive()) {
					// This is to be compatible with e.g. Figura mod
					// Trust JIT
					Utils.DUMMY_ITERATOR.remove();
					return;
				}
				ParticleAddon particleAddon = (ParticleAddon) particle;
				boolean shouldTick;
				boolean shouldRefresh;
				if (particleAddon.asyncparticles$isTicked()) {
					// Skip the first tick after the particle is added to the queue.
					// GPU particles don't skip the first tick, but skip the first refresh.
					// skip the first refresh will fix black destruction gpu particles.
					shouldTick = isGpu;
					shouldRefresh = !isGpu && enableLightCache;
				} else if (((ParticleAddon) particle).asyncparticles$isTickSync()) {
//				assert this instanceof AsyncTickableParticleGroup;
					((AsyncTickableParticleGroup) group).asyncparticles$recordSync(particle);
					return;
				} else {
					shouldTick = true;
					shouldRefresh = enableLightCache;
				}
				if (shouldTick) {
					try {
						group.tickParticle(particle);
					} catch (Throwable t) {
						ReportedException re = AsyncTickBehavior.getInstance().onTickParticleException(particle, t);
						if (re != null) {
							throw re;
						}
					}
					particleAddon.asyncparticles$setTicked();
				}
				LightCachedParticleAddon lightCachedParticle = (LightCachedParticleAddon) particle;
				if (shouldRefresh) {
					lightCachedParticle.asyncparticles$enableLightCache();
					lightCachedParticle.asyncparticles$refresh();
				} else {
					lightCachedParticle.asyncparticles$disableLightCache();
				}
			});
		}
	}
}

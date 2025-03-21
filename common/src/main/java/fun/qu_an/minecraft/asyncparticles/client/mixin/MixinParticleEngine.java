package fun.qu_an.minecraft.asyncparticles.client.mixin;

import fun.qu_an.minecraft.asyncparticles.client.*;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSCompat;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import fun.qu_an.minecraft.asyncparticles.client.util.BusyWaitEvictingQueue;
import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeEvictingQueue;
import fun.qu_an.minecraft.asyncparticles.client.util.TrackedParticleCountsMap;
import fun.qu_an.minecraft.asyncparticles.client.util.Utils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(value = ParticleEngine.class, priority = 500)
public abstract class MixinParticleEngine {
	@Mutable
	@Shadow
	@Final
	public Queue<Particle> particlesToAdd;

	@Shadow
	@Final
	public Map<ParticleRenderType, Queue<Particle>> particles;

	@Shadow
	protected ClientLevel level;

	@Mutable
	@Shadow
	@Final
	public Queue<TrackingEmitter> trackingEmitters;

	@Mutable
	@Shadow
	@Final
	private Object2IntOpenHashMap<ParticleGroup> trackedParticleCounts;

	@Inject(method = "<init>", at = @At(value = "RETURN"))
	public void init(CallbackInfo ci) {
		trackedParticleCounts = new TrackedParticleCountsMap();
		particlesToAdd = new BusyWaitEvictingQueue<>(1024, SimplePropertiesConfig.limit, this::asyncParticles$onEvicted);
	}

	@Shadow
	public abstract void updateCount(ParticleGroup group, int count);

	@Shadow
	@Final
	private static Logger LOGGER;

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public void tick() {
//		assert AsyncTicker.shouldTickParticles;
		particles.forEach((particleRenderType, queue) -> {
			if (queue.isEmpty()) {
				return;
			}
			ProfilerFiller profiler = this.level.getProfiler();
			profiler.push(particleRenderType.toString());
			AsyncTicker.PARTICLE_OPERATIONS.add(() -> tickParticleList(queue));
			profiler.pop();
		});

		if (!this.trackingEmitters.isEmpty()) {
			AsyncTicker.PARTICLE_OPERATIONS.add(() -> {
				for (TrackingEmitter emitter : this.trackingEmitters) {
					if (AsyncTicker.isCancelled() && !SimplePropertiesConfig.forceDoneParticleTick()) {
						return;
					}
					if (!emitter.isAlive()) {
						continue;
					}
					if (((ParticleAddon) emitter).asyncedParticles$isTickSync()) {
						AsyncTicker.recordSync(emitter);
						continue;
					}
					try {
						emitter.tick();
						if (ModListHelper.VS_LOADED) {
							VSCompat.removeIfOutSight(emitter);
						}
					} catch (Exception t) {
						if (AsyncTicker.isTolerable(t)) {
							LOGGER.warn("Exception ticking emitter particle {}, you can ignore it if it doesn't happen frequently.", emitter, t);
							continue;
						}
						if (SimplePropertiesConfig.markSyncIfTickFailed()) {
							LOGGER.warn("Exception ticking emitter particle {}, marking as sync", emitter, t);
							((ParticleAddon) emitter).asyncedParticles$setTickSync();
							AsyncTicker.markAsSync(emitter.getClass());
							AsyncTicker.recordSync(emitter);
						} else {
							throw t;
						}
					}
				}
			});
		}

		AsyncTicker.waitForCleanUp();

		if (!this.particlesToAdd.isEmpty()) {
			particlesToAdd.forEach(p -> {
				if (p == null) {
					// might be null because ArrayDeque is not thread-safe,
					// but we can use it safely here because we clear it every tick
					return;
				}
				if (((ParticleAddon) p).asyncedParticles$isTickSync()) {
					AsyncTicker.recordSync(p);
				}
				Queue<Particle> queue = this.particles.computeIfAbsent(p.getRenderType(),
					(p_107347_) -> {
//						EvictingQueue<Particle> queue1 = EvictingQueue.create(SimplePropertiesConfig.limit);
						Queue<Particle> queue1 = new IterationSafeEvictingQueue<>(
							16,
							SimplePropertiesConfig.limit,
							this::asyncParticles$onEvicted);
						// fix the first added particle not ticked.
						AsyncTicker.PARTICLE_OPERATIONS.add(() -> tickParticleList(queue1));
						return queue1;
					});
				p.getParticleGroup().ifPresent(g -> updateCount(g, 1));
				queue.add(p);
			});
			particlesToAdd.clear();
			// FIXME: 实现线程安全的低锁开销队列，目前会因为一些粒子在tick时添加新的粒子导致并发访问
			//  不会抛异常，因为遍历的时候不会检查为空性，无明显影响，但可能导致一些模组的粒子计数出现偏差
		}
	}

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	private void tickParticleList(Collection<Particle> collection) {
		if (collection.isEmpty()) {
			return;
		}
		for (Particle particle : collection) {
			if (AsyncTicker.isCancelled() && !SimplePropertiesConfig.forceDoneParticleTick()) {
				return;
			}
			if (!particle.isAlive()) {
				continue;
			}
			if (((ParticleAddon) particle).asyncedParticles$isTickSync()) {
				AsyncTicker.recordSync(particle);
				continue;
			}
			try {
				particle.tick();
				if (particle instanceof LightCachedParticleAddon lightCachedParticle
					&& SimplePropertiesConfig.particleLightCache()) {
					lightCachedParticle.asyncParticles$refresh();
				}
				((ParticleAddon) particle).asyncParticles$setTicked();
				if (ModListHelper.VS_LOADED) {
					VSCompat.removeIfOutSight(particle);
				}
			} catch (Exception t) {
				if (AsyncTicker.isTolerable(t)) {
					LOGGER.warn("Exception ticking particle {}, you can ignore it if it doesn't happen frequently.", particle, t);
					continue;
				}
				if (SimplePropertiesConfig.markSyncIfTickFailed()) {
					LOGGER.warn("Exception ticking particle {}, marking as sync", particle, t);
					((ParticleAddon) particle).asyncedParticles$setTickSync();
					AsyncTicker.markAsSync(particle.getClass());
					AsyncTicker.recordSync(particle);
				} else {
					throw Utils.toThrowDirectly(asyncParticles$constructCrashReport(particle, t));
				}
			}
		}
	}

	@Unique
	private static Exception asyncParticles$constructCrashReport(Particle particle, Exception t) {
		CrashReport crashReport = CrashReport.forThrowable(t, "Ticking Particle");
		CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being ticked");
		crashReportCategory.setDetail("Particle", particle::toString);
		crashReportCategory.setDetail("Particle Type", particle.getRenderType()::toString);
		return new ReportedException(crashReport);
	}

	@Inject(method = "add", at = @At(value = "HEAD"), cancellable = true)
	public void add(Particle particle, CallbackInfo ci) {
		if (!AsyncTicker.shouldTickParticles) {
			particle.remove(); // to compatible with some mods...
			ci.cancel();
		} else if (particle instanceof LightCachedParticleAddon lightCachedParticle
				   && SimplePropertiesConfig.particleLightCache()) {
			lightCachedParticle.asyncParticles$refresh();
		}
	}

	@Redirect(method = "add", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;updateCount(Lnet/minecraft/core/particles/ParticleGroup;I)V"))
	public void redirectUpdateCount(ParticleEngine instance, ParticleGroup group, int count) {
		// do nothing
		// we check this later in tick()
	}

	@Inject(method = "clearParticles", at = @At("HEAD"))
	public void redirectClearParticles(CallbackInfo ci) {
		particlesToAdd.forEach(this::asyncParticles$onEvicted);
		particlesToAdd = new BusyWaitEvictingQueue<>(1024, SimplePropertiesConfig.limit, this::asyncParticles$onEvicted);
		particles.values().forEach(queue -> queue.forEach(this::asyncParticles$onEvicted));
		AsyncTicker.onParticleEngineClear();
	}

	@Unique
	private void asyncParticles$onEvicted(Particle particle) {
		if (particle.isAlive()) {
			particle.getParticleGroup().ifPresent(g -> updateCount(g, -1));
			particle.remove();
		}
	}
}

package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import fun.qu_an.minecraft.asyncparticles.client.*;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSCompat;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeEvictingQueue;
import fun.qu_an.minecraft.asyncparticles.client.util.TrackedParticleCountsMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
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
import java.util.concurrent.ConcurrentLinkedQueue;

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
	private Queue<TrackingEmitter> trackingEmitters;

	@Mutable
	@Shadow
	@Final
	private Object2IntOpenHashMap<ParticleGroup> trackedParticleCounts;

	@Inject(method = "<init>", at = @At(value = "RETURN"))
	public void init(CallbackInfo ci) {
		trackedParticleCounts = new TrackedParticleCountsMap();
		trackingEmitters = new ConcurrentLinkedQueue<>();
	}

	@Shadow
	public abstract void updateCount(ParticleGroup group, int count);

	@Shadow
	@Final
	private static Logger LOGGER;

	@Shadow
	@Mutable
	@Final
	public static List<ParticleRenderType> RENDER_ORDER;

	@Shadow public abstract void tickParticle(Particle particle);

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public void tick() {
		if (!AsyncTicker.shouldTickParticles) {
			if (!this.particlesToAdd.isEmpty()) {
				particlesToAdd.forEach(p -> {
					if (p == null) { // might be null because ArrayDeque is not thread-safe
						return;
					}
					p.remove();
					// this will fix some mod's particle count management bug
				});
				particlesToAdd.clear();
			}
			if (AsyncTicker.particleCleanup != null) {
				AsyncTicker.particleCleanup.join();
				AsyncTicker.particleCleanup = null;
			}
			return;
		}

		particles.forEach((particleRenderType, queue) -> {
			ProfilerFiller profiler = this.level.getProfiler();
			profiler.push(particleRenderType.toString());
			AsyncTicker.PARTICLE_OPERATIONS.add(() -> tickParticleList(queue));
			profiler.pop();
		});

		if (!this.trackingEmitters.isEmpty()) {
			AsyncTicker.PARTICLE_OPERATIONS.add(() -> {
				HashSet<TrackingEmitter> set = null;
				for (TrackingEmitter emitter : this.trackingEmitters) {
					if (AsyncTicker.isCancelled() && !SimplePropertiesConfig.forceDoneParticleTick()) {
						if (set != null) {
							this.trackingEmitters.removeAll(set);
						}
						return;
					}
					if (!emitter.isAlive()) {
						if (set == null) {
							set = new HashSet<>();
						}
						set.add(emitter);
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
				if (set != null) {
					this.trackingEmitters.removeAll(set);
				}
			});
		}

		if (AsyncTicker.particleCleanup != null) {
			AsyncTicker.particleCleanup.join();
			AsyncTicker.particleCleanup = null;
		}

		if (!this.particlesToAdd.isEmpty()) {
			particlesToAdd.forEach(p -> {
				if (p == null) { // might be null because ArrayDeque is not thread-safe
					return;
				}
				if (((ParticleAddon) p).asyncedParticles$isTickSync()) {
					AsyncTicker.recordSync(p);
				}
				Queue<Particle> queue = this.particles.computeIfAbsent(p.getRenderType(),
					k -> {
						Queue<Particle> queue1 = new IterationSafeEvictingQueue<>(256, SimplePropertiesConfig.limit,
							p1 -> {
								if (p1.isAlive()){
									p1.remove();
								}
								p1.getParticleGroup().ifPresent(g -> updateCount(g, -1));
							});
						// fix the first added particle not ticked.
						AsyncTicker.PARTICLE_OPERATIONS.add(() -> tickParticleList(queue1));
						// fix not added to RENDER_ORDER
						// e.g. LodestoneParticleRenderType#*#withDepthFade()
						if (!ModListHelper.IS_FORGE &&
							k != ParticleRenderType.NO_RENDER &&
							!RENDER_ORDER.contains(k)) {
							RENDER_ORDER = ImmutableList.<ParticleRenderType>builder()
								.addAll(RENDER_ORDER)
								.add(k)
								.build();
						}
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
		Iterator<Particle> iterator = collection.iterator();
		//noinspection WhileLoopReplaceableByForEach
		while (iterator.hasNext()) {
			if (AsyncTicker.isCancelled() && !SimplePropertiesConfig.forceDoneParticleTick()) {
				return;
			}
			Particle particle = iterator.next();
			if (((ParticleAddon) particle).asyncedParticles$isTickSync()) {
				AsyncTicker.recordSync(particle);
				continue;
			}
			try {
				// keep this tick() to compatible with some mixins...
				tickParticle(particle);
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
					throw t;
				}
			}
		}
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
		AsyncTicker.onParticleEngineClear();
	}
}

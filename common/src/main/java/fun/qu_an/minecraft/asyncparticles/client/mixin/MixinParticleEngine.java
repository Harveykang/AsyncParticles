package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.sugar.Local;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

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
		particlesToAdd = new BusyWaitEvictingQueue<>(1024, SimplePropertiesConfig.limit, AsyncTicker::onEvicted);
		random = new SingleThreadedRandomSource(ThreadLocalRandom.current().nextInt());
	}

	@Shadow
	public abstract void updateCount(ParticleGroup group, int count);

	@Shadow
	@Final
	private static Logger LOGGER;

	@Shadow
	public abstract void tickParticle(Particle particle);

	@Shadow
	@Mutable
	public static List<ParticleRenderType> RENDER_ORDER;

	@Mutable
	@Shadow @Final private RandomSource random;

	@Inject(method = "tickParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/CrashReport;forThrowable(Ljava/lang/Throwable;Ljava/lang/String;)Lnet/minecraft/CrashReport;"))
	public void onTickParticle(Particle particle, CallbackInfo ci, @Local Throwable t) {
		throw Utils.toThrowDirectly(t);
	}

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public void tick() {
//		assert AsyncTicker.shouldTickParticles;
		particles.forEach((particleRenderType, queue) -> {
			// submit this task even though the queue is empty
			// we'll add particles later
			ProfilerFiller profiler = this.level.getProfiler();
			profiler.push(particleRenderType.toString());
			AsyncTicker.PARTICLE_OPERATIONS.add(() -> tickParticleList(queue));
			profiler.pop();
		});

		AsyncTicker.PARTICLE_OPERATIONS.add(() -> {
			// submit this task even though the queue is empty
			// we'll add particles later
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
				} catch (Throwable t) {
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

		AsyncTicker.waitForCleanUp();

		if (!this.particlesToAdd.isEmpty()) {
			particlesToAdd.forEach(p -> {
				if (p == null) {
					// might be null because ArrayDeque is not thread-safe,
					// but we can use it safely here because we clear it every tick
					return;
				}
				if (p instanceof TrackingEmitter emitter) {
					trackingEmitters.add(emitter);
					return;
				}
				if (((ParticleAddon) p).asyncedParticles$isTickSync()) {
					AsyncTicker.recordSync(p);
				}
				Queue<Particle> queue = this.particles.computeIfAbsent(p.getRenderType(),
					k -> {
//						EvictingQueue<Particle> queue1 = EvictingQueue.create(SimplePropertiesConfig.limit);
						Queue<Particle> queue1 = new IterationSafeEvictingQueue<>(
							16,
							SimplePropertiesConfig.limit,
							AsyncTicker::onEvicted);
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
				tickParticle(particle);
				if (particle instanceof LightCachedParticleAddon lightCachedParticle
					&& SimplePropertiesConfig.particleLightCache()) {
					lightCachedParticle.asyncParticles$refresh();
				}
				((ParticleAddon) particle).asyncParticles$setTicked();
				if (ModListHelper.VS_LOADED) {
					VSCompat.removeIfOutSight(particle);
				}
			} catch (Throwable t) {
				boolean tolerable = AsyncTicker.isTolerable(t);
				if (tolerable &&
					!AsyncTicker.EXCEPTION_TRACKER.addException(particle.getClass(), t)) {
					continue;
				}
				if (SimplePropertiesConfig.markSyncIfTickFailed()) {
					LOGGER.warn("Exception ticking particle {}, marking as sync", particle, t);
					((ParticleAddon) particle).asyncedParticles$setTickSync();
					AsyncTicker.markAsSync(particle.getClass());
					AsyncTicker.recordSync(particle);
				} else if (tolerable) {
					LocalPlayer player = Minecraft.getInstance().player;
					if (player != null) {
						player.sendSystemMessage(Component.literal(
								"Exception %s thrown while ticking particle %s exceeds the threshold, please contact the author: "
									.formatted(t.getClass().getSimpleName(), particle.getClass()))
							.append(Component.literal(AsyncparticlesClient.ISSUE_URL)
								.setStyle(Style.EMPTY.withClickEvent(
									new ClickEvent(ClickEvent.Action.OPEN_URL, AsyncparticlesClient.ISSUE_URL)))));
					}
					LOGGER.warn("Exception {} thrown while ticking particle {} exceeds the threshold, please contact the author: {}",
						t.getClass().getSimpleName(),
						particle,
						AsyncparticlesClient.ISSUE_URL,
						t);
				} else {
					throw AsyncTicker.constructCrashReport(particle, t);
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

	@Redirect(method = {
		"createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;)V",
		"createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;I)V"
	}, at = @At(value = "INVOKE", target = "Ljava/util/Queue;add(Ljava/lang/Object;)Z"))
	public boolean redirectAdd(Queue<?> instance, Object o) {
		return particlesToAdd.add((Particle) o); // redirect to particlesToAdd
	}

	@Inject(method = "clearParticles", at = @At("HEAD"))
	public void redirectClearParticles(CallbackInfo ci) {
		particlesToAdd.forEach(AsyncTicker::onEvicted);
		particlesToAdd = new BusyWaitEvictingQueue<>(1024, SimplePropertiesConfig.limit, AsyncTicker::onEvicted);
		particles.values().forEach(queue -> queue.forEach(AsyncTicker::onEvicted));
		AsyncTicker.onParticleEngineClear();
	}
}

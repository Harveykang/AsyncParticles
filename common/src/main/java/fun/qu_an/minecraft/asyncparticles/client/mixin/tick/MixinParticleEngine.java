package fun.qu_an.minecraft.asyncparticles.client.mixin.tick;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.*;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.*;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Mixin(value = ParticleEngine.class, priority = 500)
public abstract class MixinParticleEngine {
	@Shadow
	public Queue<Particle> particlesToAdd;

	@Shadow
	public Map<ParticleRenderType, Queue<Particle>> particles;

	@Shadow
	protected ClientLevel level;

	@Shadow
	public Queue<TrackingEmitter> trackingEmitters;

	@Mutable
	@Shadow
	@Final
	private Object2IntOpenHashMap<ParticleGroup> trackedParticleCounts;

	@Inject(method = "<init>", order = 9000, at = @At(value = "RETURN"))
	public void initTail(CallbackInfo ci) {
		trackedParticleCounts = new TrackedParticleCountsMap();
		particlesToAdd = BusyWaitEvictingQueue.newInstance(1024, ConfigHelper.getParticleLimit(), AsyncTicker::onEvicted);
		trackingEmitters = BusyWaitEvictingQueue.newInstance(256, ConfigHelper.getParticleLimit(), AsyncTicker::onEvicted);
		random = new SingleThreadedRandomSource(ThreadLocalRandom.current().nextInt());
	}

	@Shadow
	public abstract void tickParticle(Particle particle);

	@Mutable
	@Shadow
	@Final
	private RandomSource random;

	@Shadow
	public abstract void updateCount(ParticleGroup group, int count);

	@Inject(method = "tickParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/CrashReport;forThrowable(Ljava/lang/Throwable;Ljava/lang/String;)Lnet/minecraft/CrashReport;"))
	public void onTickParticle(Particle particle, CallbackInfo ci, @Local Throwable t) {
		if (ConfigHelper.isTickAsync()) {
			throw ExceptionUtil.toThrowDirectly(t);
		}
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

		// submit this task even though the queue is empty
		// we'll add particles later
		AsyncTicker.PARTICLE_OPERATIONS.add(this::asyncparticles$tickEmitters);

		// Keep local variable tables as they were
		Particle particle;

		boolean tickAsync = ConfigHelper.isTickAsync();
		if (tickAsync) {
			AsyncTicker.waitForCleanUp();
		} else {
			AsyncTicker.PARTICLE_OPERATIONS.forEach(Runnable::run);
			AsyncTicker.PARTICLE_OPERATIONS.clear();
			AsyncTicker.tickSyncParticles();
			particles.values().forEach(q -> q.removeIf(p -> {
				if (p.isAlive()) {
					return false;
				}
				// make sure the tracked count is correct
				p.getParticleGroup().ifPresent(group -> updateCount(group, -1));
				return true;
			}));
		}

		if (!this.particlesToAdd.isEmpty()) {
			// Write like this to be compatible with e.g. Spectrum mod
			//noinspection ForLoopReplaceableByForEach
			for (Iterator<Particle> iterator = particlesToAdd.iterator(); iterator.hasNext(); ) {
				particle = iterator.next();
				if (tickAsync &&
					((ParticleAddon) particle).asyncparticles$isTickSync()) {
					AsyncTicker.recordSync(particle);
				}
				Queue<Particle> queue = this.particles.computeIfAbsent(particle.getRenderType(),
					k -> {
						Queue<Particle> queue1 = IterationSafeEvictingQueue.newInstance(
							16,
							ConfigHelper.getParticleLimit(),
							AsyncTicker::onEvicted);
						// fix the first added particle not ticked.
						if (tickAsync) {
							AsyncTicker.PARTICLE_OPERATIONS.add(() -> tickParticleList(queue1));
						}
						// fix not added to RENDER_ORDER
						// e.g. LodestoneParticleRenderType#*#withDepthFade()
						// Since 2.0:
						// We don't do it ourselves, since it may cause some particles render twice
						// Porting Lib will do this so we just do compat to them
						// though theirs still may cause the same issue :(
						//						if (!ModListHelper.IS_FORGE &&
						//							k != ParticleRenderType.NO_RENDER &&
						//							!RENDER_ORDER.contains(k)) {
						//							// holy shit, this is definitely a giant of shit
						//							asyncparticles_Neo$addToOrderList(k);
						//						}
						return queue1;
					});
				queue.add(particle);
			}
			particlesToAdd.clear();
		}
	}

	@Unique
	private void asyncparticles$tickEmitters() {
		for (TrackingEmitter emitter : this.trackingEmitters) {
			if (AsyncTicker.isCancelled() && !ConfigHelper.forceDoneParticleTick()) {
				return;
			}
			if (!emitter.isAlive()) {
				continue;
			}
			if (!RenderSystem.isOnRenderThread() &&
				((ParticleAddon) emitter).asyncparticles$isTickSync()) {
				AsyncTicker.recordSync(emitter);
				continue;
			}
			try {
				emitter.tick();
			} catch (Throwable t) {
				AsyncTicker.onTickingParticleException(emitter, t);
			}
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
		boolean enableLightCache = ConfigHelper.particleLightCache();
		for (Particle particle : collection) {
			if (AsyncTicker.isCancelled() && !ConfigHelper.forceDoneParticleTick()) {
				return;
			}
			if (!particle.isAlive()) {
				// This is to be compatible with e.g. Figura mod
				// Trust JIT
				Utils.DUMMY_ITERATOR.remove();
				continue;
			}
			if (!RenderSystem.isOnRenderThread()) {
				if (((ParticleAddon) particle).asyncparticles$isTicked()) {
					// Skip the first tick that the particle is added to the queue.
					if (enableLightCache) {
						((LightCachedParticleAddon) particle).asyncparticles$refresh();
					}
					continue;
				}
				if (((ParticleAddon) particle).asyncparticles$isTickSync()) {
					AsyncTicker.recordSync(particle);
					continue;
				}
			}
			try {
				tickParticle(particle);
				if (enableLightCache) {
					((LightCachedParticleAddon) particle).asyncparticles$refresh();
				}
				((ParticleAddon) particle).asyncparticles$setTicked();
			} catch (Throwable t) {
				AsyncTicker.onTickingParticleException(particle, t);
			}
		}
	}

	@Inject(method = "add", at = @At(value = "HEAD"))
	public void add(Particle particle, CallbackInfo ci) {
		if (!AsyncTicker.shouldTickParticles && ConfigHelper.isTickAsync()) {
			particle.remove(); // to compatible with some mods...
			// don't cancel it,
			// otherwise it may cause memory leak with some mods
		} else if (ConfigHelper.particleLightCache()) {
			((LightCachedParticleAddon) particle).asyncparticles$refresh();
		}
	}

	@Inject(method = "clearParticles", at = @At("HEAD"))
	public void onClearParticles(CallbackInfo ci) {
		particlesToAdd.forEach(AsyncTicker::onEvicted);
		particlesToAdd = BusyWaitEvictingQueue.newInstance(1024, ConfigHelper.getParticleLimit(), AsyncTicker::onEvicted);
		trackingEmitters.forEach(AsyncTicker::onEvicted);
		trackingEmitters = BusyWaitEvictingQueue.newInstance(256, ConfigHelper.getParticleLimit(), AsyncTicker::onEvicted);
		particles.values().forEach(queue -> queue.forEach(AsyncTicker::onEvicted));
		AsyncTicker.onParticleEngineClear();
	}
}

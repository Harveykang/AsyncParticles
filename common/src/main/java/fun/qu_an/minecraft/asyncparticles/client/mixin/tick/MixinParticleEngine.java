package fun.qu_an.minecraft.asyncparticles.client.mixin.tick;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.*;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import fun.qu_an.minecraft.asyncparticles.client.util.*;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Mixin(value = ParticleEngine.class, priority = 500)
public abstract class MixinParticleEngine {
	@Mutable
	@Shadow
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

	@Inject(method = "<init>", order = 9000, at = @At(value = "RETURN"))
	public void init(CallbackInfo ci) {
		trackedParticleCounts = new TrackedParticleCountsMap();
		particlesToAdd = new BusyWaitEvictingQueue<>(1024, SimplePropertiesConfig.getLimit(), AsyncTicker::onEvicted);
		trackingEmitters = new BusyWaitEvictingQueue<>(1024, SimplePropertiesConfig.getLimit(), AsyncTicker::onEvicted);
		random = new SingleThreadedRandomSource(ThreadLocalRandom.current().nextInt());
	}

	@Shadow
	public abstract void updateCount(ParticleGroup group, int count);

	@Shadow
	public abstract void tickParticle(Particle particle);

	@Shadow
	@Mutable
	public static List<ParticleRenderType> RENDER_ORDER;

	@Mutable
	@Shadow
	@Final
	private RandomSource random;

	@Inject(method = "tickParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/CrashReport;forThrowable(Ljava/lang/Throwable;Ljava/lang/String;)Lnet/minecraft/CrashReport;"))
	public void onTickParticle(Particle particle, CallbackInfo ci, @Local Throwable t) {
		if (SimplePropertiesConfig.isTickAsync()){
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
			ProfilerFiller profiler = Profiler.get();
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
				if (((ParticleAddon) emitter).asyncparticles$isTickSync()) {
					AsyncTicker.recordSync(emitter);
					continue;
				}
				try {
					emitter.tick();
				} catch (Throwable t) {
					AsyncTicker.onTickingParticleException(emitter, t);
				}
			}
		});

		if (SimplePropertiesConfig.isTickAsync()) {
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
			Particle particle;
			//noinspection ForLoopReplaceableByForEach
			for (Iterator<Particle> iterator = particlesToAdd.iterator(); iterator.hasNext(); ) {
				particle = iterator.next();
				if (((ParticleAddon) particle).asyncparticles$isTickSync()) {
					AsyncTicker.recordSync(particle);
				}
				Queue<Particle> queue = this.particles.computeIfAbsent(particle.getRenderType(),
					k -> {
						Queue<Particle> queue1 = new IterationSafeEvictingQueue<>(
							16,
							SimplePropertiesConfig.getLimit(),
							AsyncTicker::onEvicted);
						// fix the first added particle not ticked.
						if (SimplePropertiesConfig.isTickAsync()) {
							AsyncTicker.PARTICLE_OPERATIONS.add(() -> tickParticleList(queue1));
						}
						// fix not added to RENDER_ORDER
						// e.g. LodestoneParticleRenderType#*#withDepthFade()
						if (!ModListHelper.IS_FORGE &&
							k.renderType() != null &&
							!RENDER_ORDER.contains(k)) {
							RENDER_ORDER = ImmutableList.<ParticleRenderType>builder()
								.addAll(RENDER_ORDER)
								.add(k)
								.build();
						}
						return queue1;
					});
				queue.add(particle);
			}
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
				// This is to be compatible with e.g. Figura mod
				// Trust JIT
				Utils.DUMMY_ITERATOR.remove();
				continue;
			}
			if (((ParticleAddon) particle).asyncparticles$isTickSync()) {
				AsyncTicker.recordSync(particle);
				continue;
			}
			try {
				tickParticle(particle);
				if (particle instanceof LightCachedParticleAddon lightCachedParticle
					&& SimplePropertiesConfig.particleLightCache()) {
					lightCachedParticle.asyncParticles$refresh();
				}
				((ParticleAddon) particle).asyncparticles$setTicked();
			} catch (Throwable t) {
				AsyncTicker.onTickingParticleException(particle, t);
			}
		}
	}

	@Inject(method = "add", at = @At(value = "HEAD"), cancellable = true)
	public void add(Particle particle, CallbackInfo ci) {
		if (!AsyncTicker.shouldTickParticles && SimplePropertiesConfig.isTickAsync()) {
			particle.remove(); // to compatible with some mods...
			ci.cancel();
		} else if (particle instanceof LightCachedParticleAddon lightCachedParticle
				   && SimplePropertiesConfig.particleLightCache()) {
			lightCachedParticle.asyncParticles$refresh();
		}
	}

	@Inject(method = "clearParticles", at = @At("HEAD"))
	public void onClearParticles(CallbackInfo ci) {
		particlesToAdd.forEach(AsyncTicker::onEvicted);
		particlesToAdd = new BusyWaitEvictingQueue<>(1024, SimplePropertiesConfig.getLimit(), AsyncTicker::onEvicted);
		trackingEmitters.forEach(AsyncTicker::onEvicted);
		trackingEmitters = new BusyWaitEvictingQueue<>(1024, SimplePropertiesConfig.getLimit(), AsyncTicker::onEvicted);
		particles.values().forEach(queue -> queue.forEach(AsyncTicker::onEvicted));
		AsyncTicker.onParticleEngineClear();
	}
}

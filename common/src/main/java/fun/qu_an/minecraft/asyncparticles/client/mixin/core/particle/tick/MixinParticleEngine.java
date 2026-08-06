package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.tick;

import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.TaskHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.ParticleHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.TickParticleRecursiveAction;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.Utils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.function.BiConsumer;

@Mixin(value = ParticleEngine.class, priority = 500)
public abstract class MixinParticleEngine implements ParticleEngineAddon {
	@Shadow
	public Queue<Particle> particlesToAdd;
	@Shadow
	public Map<ParticleRenderType, Queue<Particle>> particles;
	@Shadow
	public Queue<TrackingEmitter> trackingEmitters;

	@Shadow
	public abstract void tickParticle(Particle particle);

	@Shadow
	public abstract void updateCount(ParticleGroup group, int count);

	@Shadow
	protected ClientLevel level;

	@Inject(method = "tickParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/CrashReport;forThrowable(Ljava/lang/Throwable;Ljava/lang/String;)Lnet/minecraft/CrashReport;"))
	public void onTickParticle(Particle particle, CallbackInfo ci, @Local Throwable t) {
		if (ConfigHelper.isAsyncParticleTick()) {
			throw ExceptionUtil.toThrowDirectly(t);
		}
	}

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public void tick() {
		// Keep local var table as they were
		Particle particle;
		AsyncTickBehavior tickBehavior = AsyncTickBehavior.getInstance();
		boolean tickAsync = ConfigHelper.isAsyncParticleTick() && tickBehavior.isParticlePhase();
		boolean asyncAll = tickAsync && !ConfigHelper.isGpuOnlyAsyncParticleTick();
		if (asyncAll) {
			TaskHelper taskHelper = tickBehavior.getTickTaskManager();
			asyncparticles$forEach(this.particles, (renderType, queue) -> {
				if (queue.isEmpty()) {
					return;
				}
				ProfilerFiller profiler = this.level.getProfiler();
				profiler.push(renderType.toString());
				if (!tickBehavior.shouldSyncRenderType(renderType.getClass())) {
					tickParticleList(tickBehavior.getSyncParticles(renderType));
					taskHelper.addTask(() -> tickParticleList(queue));
				} else {
					tickParticleList(queue);
				}
				profiler.pop();
			});
			taskHelper.groupTasks(true);
		} else {
			// forEach is an inject point (eg. ParticleCore)
			this.particles.forEach((type, queue) -> {
				ProfilerFiller profiler = this.level.getProfiler();
				tickParticleList(queue);
				profiler.pop();
			});
		}
		if (!this.trackingEmitters.isEmpty()) {
			for (TrackingEmitter trackingEmitter : this.trackingEmitters) {
				trackingEmitter.tick(); // TODO can be async-lized safely?
				// clear in AsyncTickBehavior
			}
			if (!tickAsync) {
				tickBehavior.doEmittersRemoveIf(trackingEmitters);
			}
		}

		if (!particlesToAdd.isEmpty()) {
			// Write like this to be compatible with e.g. Spectrum mod
			//noinspection ForLoopReplaceableByForEach
			for (Iterator<Particle> iterator = particlesToAdd.iterator(); iterator.hasNext(); ) {
				particle = iterator.next();
				this.particles.computeIfAbsent(particle.getRenderType(), renderType -> {
					Queue<Particle> queue1 = ParticleHelper.newParticleQueue();
					if (asyncAll && !tickBehavior.shouldSyncRenderType(renderType.getClass())) {
						AsyncTickBehavior.getInstance().getTickTaskManager()
							.addTask(() -> tickParticleList(queue1));
					}
					return queue1;
				}).add(particle);
				if (tickAsync
//					&& ConfigHelper.isAsyncTickParticle() // tested in asyncparticles$canTickAsync()
					&& tickBehavior.shouldSync(((ParticleAddon) particle).asyncparticles$getRealClass())) {
					if (GpuParticleBehavior.getInstance().canRenderFast(particle)) {
						tickBehavior.getSyncGpuParticles(particle.getRenderType()).add(particle);
					} else if (asyncAll) {
						tickBehavior.getSyncParticles(particle.getRenderType()).add(particle);
					}
				}
			}
			particlesToAdd.clear();
		}
	}

	@Unique
	private <K, V> void asyncparticles$forEach(Map<K, V> i, BiConsumer<K, V> f) {
		i.forEach(f);
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
		boolean isGpu = ParticleHelper.GPU_PARTICLE_PHASE.get();
		boolean isAsync = ThreadUtil.isOnParticleTickerThread();
		if (isAsync && ConfigHelper.isSplitParticleTick()) {
			// assert this instanceof AsyncTickableParticleGroup
			TickParticleRecursiveAction.execute((ParticleEngine) (Object) this, collection.spliterator(), isGpu);
			return;
		}
		AsyncTickBehavior tickBehavior = AsyncTickBehavior.getInstance();
		Set<Particle> syncParticles = null;
		for (Particle particle : collection) {
			if (!particle.isAlive()) {
				Utils.DUMMY_ITERATOR.remove();
				continue;
			}
			ParticleAddon particleAddon = (ParticleAddon) particle;
			boolean isSyncParticle;
			// Skip the first tick after the particle is added to the queue.
			// GPU particles don't skip the first tick, but skip the first refresh.
			// skip the first refresh will fix black destruction gpu particles.
			boolean shouldTick;
			if (!isAsync) {
				isSyncParticle = false;
				shouldTick = true;
			} else {
				if (syncParticles == null) {
					syncParticles = isGpu ? tickBehavior.getSyncGpuParticles(particle.getRenderType()) : tickBehavior.getSyncParticles(particle.getRenderType());
				}
				isSyncParticle = syncParticles.contains(particle);
				shouldTick = !isSyncParticle && (!particleAddon.asyncparticles$isTicked() || isGpu);
			}
			if (shouldTick) {
				try {
					tickParticle(particle);
				} catch (Throwable t) {
					if (AsyncTickBehavior.getInstance().getExceptionHandler().onTickParticleException(particle, t)) {
						break;
					}
				}
				particleAddon.asyncparticles$setTicked();
			}
			if (!isSyncParticle) {
				((LightCachedParticleAddon) particle).asyncparticles$tickLightCache();
			}
		}
		if (!isAsync) { // sync tick
			collection.removeIf(p -> {
				if (p.isAlive()) {
					return false;
				}
				// make sure the tracked count is correct
				p.getParticleGroup().ifPresent(group -> updateCount(group, -1));
				return true;
			});
		}
	}
}

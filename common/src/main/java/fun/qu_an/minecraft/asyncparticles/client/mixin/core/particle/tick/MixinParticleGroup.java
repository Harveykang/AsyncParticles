package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.tick;

import com.google.common.collect.EvictingQueue;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleGroupAddition;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.ParticleCullingMode;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.addon.AsyncTickableParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeEvictingQueue;
import fun.qu_an.minecraft.asyncparticles.client.core.ParticleHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.Utils;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.TrackingEmitter;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

@Mixin(ParticleGroup.class)
public abstract class MixinParticleGroup implements ParticleGroupAddition {
	@Mutable
	@Shadow
	@Final
	protected Queue<? extends Particle> particles;

	@Shadow
	protected abstract void tickParticle(Particle particle);

	@Redirect(method = "<init>", at = @At(value = "INVOKE", remap = false,
		target = "Lcom/google/common/collect/EvictingQueue;create(I)Lcom/google/common/collect/EvictingQueue;"))
	private EvictingQueue<?> redirectNewQueue(int maxSize) {
		return null;
	}

	@Inject(method = "<init>", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER,
		target = "Lnet/minecraft/client/particle/ParticleGroup;particles:Ljava/util/Queue;"))
	private void injectNewQueue(ParticleEngine engine, CallbackInfo ci) {
		particles = ParticleHelper.newParticleQueue();
	}

	@Inject(method = "add", at = @At("HEAD"))
	private void injectAdd(Particle particle, CallbackInfo ci) {
		if (this instanceof AsyncTickableParticleGroup asyncGroup
			&& ((ParticleAddon) particle).asyncparticles$isTickSync()) {
			asyncGroup.asyncparticle$recordSync(particle);
		}
	}

	/**
	 * @author Harvey_Husky
	 * @reason Too many changes, need to rewrite the entire method.
	 */
	@Overwrite
	public void tickParticles() { // TODO move to implementations
		if (particles.isEmpty()) {
			return;
		}
		boolean enableLightCache = ConfigHelper.particleLightCache();
		boolean isOnMainThread = ThreadUtil.isOnMainThread();
		boolean isGpu = (ParticleGroup<?>) (Object) this instanceof GpuParticleGroup;
		boolean forceDone = ConfigHelper.forceDoneParticleTick();
		for (Particle particle : particles) {
			if (AsyncTickBehavior.getInstance().isCancelled() && !forceDone) {
				return;
			}
			if (!particle.isAlive()) {
				// This is to be compatible with e.g. Figura mod
				// Trust JIT
				Utils.DUMMY_ITERATOR.remove();
				continue;
			}
			ParticleAddon particleAddon = (ParticleAddon) particle;
			boolean shouldTick;
			boolean shouldRefresh;
			if (isOnMainThread) {
				shouldTick = true;
				shouldRefresh = false;
			} else if (particleAddon.asyncparticles$isTicked()) {
				// Skip the first tick after enqueued that the particle is added to the queue.
				// only GPU particles don't skip the first tick, but skip the first refresh.
				// skip the first refresh will fix black destruction gpu particles.
				shouldTick = isGpu;
				shouldRefresh = !isGpu && enableLightCache;
			} else if (particleAddon.asyncparticles$isTickSync()) {
				AsyncTickBehavior.getInstance().recordSync(particle);
				continue;
			} else {
				shouldTick = true;
				shouldRefresh = enableLightCache;
			}
			if (shouldTick) {
				try {
					// We must ensure `tickParticle` method appear once in this method,
					// otherwise the other mod's mixins will not work properly.
					tickParticle(particle);
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
		}
	}

	@Override
	public void asyncparticles$removeDeadParticles() {
		if (ConfigHelper.isParallelQueueRemoval()) {
			((IterationSafeEvictingQueue<? extends Particle>) particles)
				.parallelRemoveIf(particle ->
						AsyncTickBehavior.getInstance().shouldRemove(particle, ConfigHelper.isRemoveIfMissedTick()),
					ConfigHelper.isParallelQueueEviction(),
					AsyncTickBehavior.THREADS,
					AsyncTickBehavior.getInstance().getExecutor());
		} else {
			particles.removeIf(particle ->
				AsyncTickBehavior.getInstance().shouldRemove(particle, ConfigHelper.isRemoveIfMissedTick()));
		}
	}

	@Override
	public void asyncparticles$clear() {
		particles.clear();
	}

	@Override
	public void asyncparticle$tickSyncParticles() {
		if (!ConfigHelper.isTickAsync()
			|| !(this instanceof AsyncTickableParticleGroup asyncGroup)) {
			return;
		}
		Set<Particle> syncParticles = asyncGroup.asyncparticle$getSyncParticles();
		if (syncParticles.isEmpty()) {
			return;
		}
		boolean enableLightCache = ConfigHelper.particleLightCache();
		for (Iterator<Particle> iterator = syncParticles.iterator(); iterator.hasNext(); ) {
			Particle particle = iterator.next();
			try {
				tickParticle(particle);
				if (!(particle instanceof TrackingEmitter)) {
					if (enableLightCache) {
						((LightCachedParticleAddon) particle).asyncparticles$refresh();
					}
					((ParticleAddon) particle).asyncparticles$setTicked();
				}
			} catch (Throwable e) {
				throw AsyncTickBehavior.getInstance().constructCrashReport(particle, e);
			}
			if (!particle.isAlive()) {
				// we manage the count in cleanup task
				//				particle.getParticleGroup().ifPresent((particleGroup) -> particleEngine.updateCount(particleGroup, -1));
				iterator.remove();
			}
		}
	}
}

package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.tick;

import com.google.common.collect.EvictingQueue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleGroupAddition;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.addon.AsyncTickableParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.TickParticleRecursiveAction;
import fun.qu_an.minecraft.asyncparticles.client.util.CombinedIterable;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.ParticleHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.Utils;
import net.minecraft.ReportedException;
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
import java.util.List;
import java.util.Queue;
import java.util.Set;

@Mixin(ParticleGroup.class)
public abstract class MixinParticleGroup implements ParticleGroupAddition {
	@Mutable
	@Shadow
	@Final
	protected Queue<? extends Particle> particles;
	@Unique
	private volatile boolean asyncparticles$shouldRemoveInParallel;

	@Shadow
	protected abstract void tickParticle(Particle particle);

	@Shadow
	public abstract boolean isEmpty();

	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", remap = false,
		target = "Lcom/google/common/collect/EvictingQueue;create(I)Lcom/google/common/collect/EvictingQueue;"))
	private EvictingQueue<?> redirectNewQueue(int maxSize, Operation<EvictingQueue<?>> original) {
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
			asyncGroup.asyncparticles$recordSync(particle);
		}
	}

	@Inject(method = "tickParticles", at = @At("HEAD"))
	public void injectTickParticlesHead(CallbackInfo ci) {
		this.asyncparticles$shouldRemoveInParallel = false;
	}

	@WrapOperation(method = "tickParticles", at = @At(value = "INVOKE", target = "Ljava/util/Queue;isEmpty()Z"))
	private boolean wrapIsEmpty(Queue<?> instance, Operation<Boolean> original) {
		return original.call(instance)
			&& (!(this instanceof GpuParticleGroup gpuParticleGroup) || gpuParticleGroup.asyncparticles$getGpuParticles().isEmpty());
	}

	@WrapOperation(method = "tickParticles", at = @At(value = "INVOKE", target = "Ljava/util/Queue;iterator()Ljava/util/Iterator;"))
	private Iterator<Particle> wrapIterator(Queue<Particle> instance, Operation<Iterator<Particle>> original) {
		Iterator<Particle> call = original.call(instance);
		if (this instanceof GpuParticleGroup gpuParticleGroup) {
			return CombinedIterable.of(call, (Iterator) gpuParticleGroup.asyncparticles$getGpuParticles().iterator());
		}
		return call;
	}

	@Inject(method = "tickParticles", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/particle/ParticleGroup;tickParticle(Lnet/minecraft/client/particle/Particle;)V"))
	public void injectTickParticlesTick(CallbackInfo ci, @Local(ordinal = 0) Particle particle) {
		LightCachedParticleAddon lightCachedParticle = (LightCachedParticleAddon) particle;
		if (ConfigHelper.particleLightCache() && lightCachedParticle.asyncparticles$isEnabledLightCache()) {
			lightCachedParticle.asyncparticles$refresh();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void asyncparticles$asyncTickParticles() {
		this.asyncparticles$shouldRemoveInParallel = true; // otherwise this method is overwritten and don't call super.
		if (isEmpty()) {
			return;
		}
		ThreadUtil.assertParticleTickerThread();
		if (ConfigHelper.isSplitParticleTick()) {
			// assert this instanceof AsyncTickableParticleGroup
			TickParticleRecursiveAction.execute((ParticleGroup<?>) (Object) this, particles.spliterator());
			return;
		}
		boolean enableLightCache = ConfigHelper.particleLightCache();
		CombinedIterable.CombinedIterator<? extends Particle> iterator = CombinedIterable.of(
			this.particles,
			this instanceof GpuParticleGroup gpuParticleGroup
				? (Iterable) gpuParticleGroup.asyncparticles$getGpuParticles()
				: List.of()
		).iterator(); // iterator() could be an inject point.
		while (iterator.hasNext()) {
			Particle particle = iterator.next();
			if (!particle.isAlive()) {
				continue;
			}
			ParticleAddon particleAddon = (ParticleAddon) particle;
			boolean shouldTick;
			if (particleAddon.asyncparticles$isTicked()) {
				// Skip the first tick after the particle is added to the queue.
				// GPU particles don't skip the first tick, but skip the first refresh.
				// skip the first refresh will fix black destruction gpu particles.
				shouldTick = !iterator.isLeft();
			} else if (((ParticleAddon) particle).asyncparticles$isTickSync()) {
//				assert this instanceof AsyncTickableParticleGroup;
				((AsyncTickableParticleGroup) this).asyncparticles$recordSync(particle);
				continue;
			} else {
				shouldTick = true;
			}
			if (shouldTick) {
				try {
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
			if (enableLightCache && lightCachedParticle.asyncparticles$isEnabledLightCache()) {
				lightCachedParticle.asyncparticles$refresh();
			}
		}
	}

	@Override
	public void asyncparticles$removeDeadParticles() {
		if (asyncparticles$shouldRemoveInParallel) {
			AsyncTickBehavior.getInstance().doParticlesRemoveIf(particles);
			if (this instanceof GpuParticleGroup gpuParticleGroup) {
				gpuParticleGroup.asyncparticles$removeDeadGpuParticles();
			}
		}
	}

	@Override
	public void asyncparticles$doEvictAll() {
		particles.forEach(AsyncTickBehavior.getInstance()::onEvict);
		if (this instanceof GpuParticleGroup gpuGroup) {
			gpuGroup.asyncparticles$getGpuParticles().forEach(AsyncTickBehavior.getInstance()::onEvict);
		}
	}

	@Override
	public void asyncparticles$tickSyncParticles() {
		if (!ConfigHelper.isAsyncTickParticle()
			|| !(this instanceof AsyncTickableParticleGroup asyncGroup)) {
			return;
		}
		Set<Particle> syncParticles = asyncGroup.asyncparticles$getSyncParticles();
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

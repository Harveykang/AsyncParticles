package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.tick;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.addon.*;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.ParticleHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.TickParticleRecursiveAction;
import fun.qu_an.minecraft.asyncparticles.client.util.CombinedIterable;
import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeEvictingQueue;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(ParticleGroup.class)
public abstract class MixinParticleGroup implements ParticleGroupAddition {
	@Mutable
	@Shadow
	@Final
	protected Queue<Particle> particles;
	@Unique
	private boolean asyncparticles$shouldRemoveInParallel;

	@Shadow
	public abstract void tickParticle(Particle particle);

	@Shadow
	public abstract boolean isEmpty();

	@WrapOperation(method = "<init>", at = @At(value = "NEW", remap = false,
		target = "(I)Ljava/util/ArrayDeque;"))
	private ArrayDeque<?> redirectNewQueue(int numElements, Operation<ArrayDeque<?>> original) {
		return null;
	}

	@Inject(method = "<init>", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER,
		target = "Lnet/minecraft/client/particle/ParticleGroup;particles:Ljava/util/Queue;"))
	private void injectNewQueue(ParticleEngine engine, CallbackInfo ci) {
		particles = ParticleHelper.newParticleQueue();
	}

	@Inject(method = "add", at = @At("HEAD"))
	private void injectAdd(Particle particle, CallbackInfoReturnable<Boolean> cir) {
		if (this instanceof AsyncTickableParticleGroup asyncGroup
			&& ((ParticleAddon) particle).asyncparticles$isTickSync()) {
			asyncGroup.asyncparticles$recordSync(particle);
		}
	}

	/**
	 * @author Harvey_Husky
	 * @reason Too many changes, need to rewrite the entire method.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	@Overwrite
	public void tickParticles() { // TODO move to implementations
		this.asyncparticles$shouldRemoveInParallel = true;
		if (this.particles.isEmpty() // Are there any modules that rely on this injection point?
			&& (!(this instanceof GpuParticleGroup gpuParticleGroup) || gpuParticleGroup.asyncparticles$getGpuParticles().isEmpty())) {
			return;
		}
		if (ThreadUtil.isOnParticleTickerThread() && ConfigHelper.isSplitParticleTick()) {
			TickParticleRecursiveAction.execute((ParticleGroup<?>) (Object) this, particles.spliterator());
			return;
		}
		boolean enableLightCache = ConfigHelper.particleLightCache();
		boolean isOnMainThread = ThreadUtil.isOnMainThread();
		CombinedIterable.CombinedIterator<Particle> iterator = CombinedIterable.of(
			this.particles,
			this instanceof GpuParticleGroup gpuParticleGroup
				? (Iterable<Particle>) (Iterable) gpuParticleGroup.asyncparticles$getGpuParticles()
				: List.of()
		).iterator(); // iterator() could be an inject point.
		while (iterator.hasNext()) {
			Particle particle = iterator.next();
			if (!particle.isAlive()) {
				// To be compatible with other mod
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
				// Skip the first tick after the particle is added to the queue.
				// GPU particles don't skip the first tick, but skip the first refresh.
				// skip the first refresh will fix black destruction gpu particles.
				shouldTick = !iterator.isLeft();
				shouldRefresh = !shouldTick && enableLightCache;
			} else if (((ParticleAddon) particle).asyncparticles$isTickSync()) {
//				assert this instanceof AsyncTickableParticleGroup;
				((AsyncTickableParticleGroup) this).asyncparticles$recordSync(particle);
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
				if (!isOnMainThread) {
					particleAddon.asyncparticles$setTicked();
				}
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

	@ModifyExpressionValue(method = "add", at = @At(value = "CONSTANT", args = "intValue=16384"))
	private int modifyParticleLimit1(int original) {
		return ConfigHelper.getParticleLimit();
	}

	@ModifyExpressionValue(method = "add", at = @At(value = "CONSTANT", args = "intValue=12288"))
	private int modifyParticleLimit2(int original) {
		int particleLimit = ConfigHelper.getParticleLimit();
		return (particleLimit >> 1) + (particleLimit >> 2);
	}

	@ModifyExpressionValue(method = "add", at = @At(value = "CONSTANT", args = "floatValue=4096.0"))
	private float modifyParticleLimit3(float original) {
		return ConfigHelper.getParticleLimit() >> 2;
	}

	@Override
	public void asyncparticles$removeDeadParticles() {
		if (!asyncparticles$shouldRemoveInParallel) {
			return;
		}
		if (ConfigHelper.isParallelQueueRemoval()) {
			((IterationSafeEvictingQueue<? extends Particle>) particles)
				.parallelRemoveIf(particle ->
						AsyncTickBehavior.getInstance().shouldRemove(particle),
					ConfigHelper.isParallelQueueEviction(),
					AsyncTickBehavior.THREADS,
					AsyncTickBehavior.getInstance().getExecutor());
			if (this instanceof GpuParticleGroup gpuParticleGroup) {
				((IterationSafeEvictingQueue<? extends Particle>) gpuParticleGroup.asyncparticles$getGpuParticles())
					.parallelRemoveIf(particle ->
							AsyncTickBehavior.getInstance().shouldRemove(particle),
						ConfigHelper.isParallelQueueEviction(),
						AsyncTickBehavior.THREADS,
						AsyncTickBehavior.getInstance().getExecutor());
			}
		} else {
			particles.removeIf(particle ->
				AsyncTickBehavior.getInstance().shouldRemove(particle));
			if (this instanceof GpuParticleGroup gpuParticleGroup) {
				gpuParticleGroup.asyncparticles$getGpuParticles().removeIf(particle ->
					AsyncTickBehavior.getInstance().shouldRemove(particle));
			}
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

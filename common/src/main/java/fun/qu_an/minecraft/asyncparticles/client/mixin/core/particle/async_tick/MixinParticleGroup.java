package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.async_tick;

import com.google.common.collect.EvictingQueue;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleGroupAddition;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.ParticleCullingMode;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_tick.AsyncTickableParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeEvictingQueue;
import fun.qu_an.minecraft.asyncparticles.client.core.ParticleHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.Utils;
import net.minecraft.ReportedException;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;

@Mixin(ParticleGroup.class)
public abstract class MixinParticleGroup implements ParticleGroupAddition {
	@Mutable
	@Shadow
	@Final
	protected Queue<? extends Particle> particles;
	@Unique
	private CompletableFuture<Void> asyncparticles$future;

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

	/**
	 * @author Harvey_Husky
	 * @reason Too many changes, need to rewrite the entire method.
	 */
	@Overwrite
	public void tickParticles() {
		boolean enableLightCache = ConfigHelper.particleLightCache();
		boolean forceDone = ConfigHelper.forceDoneParticleTick();
		for (Particle particle : particles) {
			if (AsyncTickBehavior.isCancelled() && !forceDone) {
				return;
			}
			if (!particle.isAlive()) {
				// To be compatible with certain mods that require this injection point.
				Utils.DUMMY_ITERATOR.remove();
				continue;
			}
			if (((ParticleAddon) particle).asyncparticles$isTicked()) {
				// Skip the first tick that the particle is added to the queue.
				if (enableLightCache) {
					((LightCachedParticleAddon) particle).asyncparticles$refresh();
				}
				continue;
			}
			if (((ParticleAddon) particle).asyncparticles$isTickSync()) {
//					AsyncTickBehavior.recordSync(particle);
				continue;
			}
			try {
				tickParticle(particle);
			} catch (Throwable t) {
				ReportedException re = AsyncTickBehavior.onTickParticleException(particle, t);
				if (re != null) {
					throw re;
				}
			}
			((ParticleAddon) particle).asyncparticles$setTicked();
			if (enableLightCache) {
				((LightCachedParticleAddon) particle).asyncparticles$refresh();
			}
		}
	}

	@Override
	public void asyncparticles$cleanUp() {
		if (ConfigHelper.isParallelQueueRemoval()) {
			((IterationSafeEvictingQueue<? extends Particle>) particles)
				.parallelRemoveIf(particle ->
						AsyncTickBehavior.shouldRemove(particle, ConfigHelper.isRemoveIfMissedTick()),
					ConfigHelper.isParallelQueueEviction(),
					AsyncTickBehavior.THREADS,
					AsyncTickBehavior.EXECUTOR);
		} else {
			particles.removeIf(particle ->
				AsyncTickBehavior.shouldRemove(particle, ConfigHelper.isRemoveIfMissedTick()));
		}
	}

	@Override
	public void asyncparticles$setFuture(CompletableFuture<Void> future) {
		this.asyncparticles$future = future;
	}

	@Override
	public CompletableFuture<Void> asyncparticles$getFuture() {
		return this.asyncparticles$future;
	}
}

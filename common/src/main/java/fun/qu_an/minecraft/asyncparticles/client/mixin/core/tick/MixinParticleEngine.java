package fun.qu_an.minecraft.asyncparticles.client.mixin.core.tick;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.ParticleCullingMode;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.particle.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.particle.ParticleHelper;
import fun.qu_an.minecraft.asyncparticles.client.particle.render.IParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.util.*;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.ParticleGroup;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;

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
		particlesToAdd = BusyWaitEvictingQueue.newInstance(AsyncParticlesConfig.MIN_PARTICLE_LIMIT, ConfigHelper.getParticleLimit(), AsyncTickBehavior.INSTANCE::onEvicted);
		trackingEmitters = BusyWaitEvictingQueue.newInstance(AsyncParticlesConfig.MIN_PARTICLE_LIMIT / 4, ConfigHelper.getParticleLimit(), AsyncTickBehavior.INSTANCE::onEvicted);
	}

	@Shadow
	public abstract void tickParticle(Particle particle);

	@Shadow
	public abstract void updateCount(ParticleGroup group, int count);

	@ModifyExpressionValue(method = "countParticles", at = @At(value = "INVOKE", target = "Ljava/util/stream/IntStream;sum()I"))
	private int modifyCount(int i) {
		return i + GpuParticleBehavior.INSTANCE.gpuParticles.values().stream().mapToInt(Collection::size).sum();
	}

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
		if (!trackingEmitters.isEmpty()) {
			AsyncTickBehavior.INSTANCE.PARTICLE_OPERATIONS.add(this::asyncparticles$tickEmitters);
		}

		// Keep local variable tables as they were
		Particle particle;

		boolean tickAsync = ConfigHelper.isTickAsync();
		if (tickAsync) {
			AsyncTickBehavior.INSTANCE.waitForCleanUp();
		} else {
			particles.forEach(this::asyncparticles$scheduleParticleTick);
			AsyncTickBehavior.INSTANCE.PARTICLE_OPERATIONS.forEach(Runnable::run);
			AsyncTickBehavior.INSTANCE.PARTICLE_OPERATIONS.clear();
			AsyncTickBehavior.INSTANCE.tickSyncParticles();
			particles.values().forEach(q -> q.removeIf(p -> {
				if (p.isAlive()) {
					return false;
				}
				// make sure the tracked count is correct
				p.getParticleGroup().ifPresent(group -> updateCount(group, -1));
				return true;
			}));
		}

		boolean gpuParticles = ConfigHelper.isGpuParticles();
		if (!particlesToAdd.isEmpty()) {
			boolean appendNewParticlesToRenderer = ConfigHelper.isAppendNewParticlesToRenderer();
			// Write like this to be compatible with e.g. Spectrum mod
			//noinspection ForLoopReplaceableByForEach
			for (Iterator<Particle> iterator = particlesToAdd.iterator(); iterator.hasNext(); ) {
				particle = iterator.next();
				boolean canComputeFast;
				if (!gpuParticles || !tickAsync) {
					canComputeFast = false;
				} else if (((ParticleAddon) particle).asyncparticles$isTickSync()) {
					AsyncTickBehavior.INSTANCE.recordSync(particle);
					canComputeFast = false;
				} else {
					canComputeFast = particle instanceof TextureSheetParticle tsp && GpuParticleBehavior.INSTANCE.canRenderFast(tsp);
				}
				Queue queue;
				ParticleRenderType renderType = particle.getRenderType();
				if (!canComputeFast) {
					queue = this.particles.computeIfAbsent(renderType, this::asyncparticles$newParticleQueue);
				} else {
					queue = GpuParticleBehavior.INSTANCE.gpuParticles.computeIfAbsent(renderType, k -> {
						GpuParticleBehavior.INSTANCE.createRenderer(k);
						return asyncparticles$newParticleQueue(k);
					});
					if (appendNewParticlesToRenderer) {
						GpuParticleBehavior.INSTANCE.getRenderer(renderType).append(GpuParticleBehavior.INSTANCE.getCameraPos(), ((TextureSheetParticle) particle));
					}
					// mark particles as gpu
					// this will not skip the first tick after enqueued, while cpu particles skip it
					((ParticleAddon) particle).asyncparticles$setGpu(true);
				}
				queue.add(particle);
			}
			particlesToAdd.clear();
		}
		if (tickAsync) {
			if (gpuParticles) {
				GpuParticleBehavior.INSTANCE.swapAllBuffers();
				GpuParticleBehavior.INSTANCE.setGpuParticleLimit(ConfigHelper.getParticleLimit());
				Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
				GpuParticleBehavior.INSTANCE.setCameraPos(camera.getPosition());
				GpuParticleBehavior.INSTANCE.gpuParticles.forEach(this::asyncparticles$scheduleGpuParticleTick);
			}
			particles.forEach(this::asyncparticles$scheduleParticleTick);
		}
	}

	/**
	 * @see fun.qu_an.minecraft.asyncparticles.client.mixin.compat.fabric.porting_lib_base.MixinMixinParticleEngine
	 */
	@Unique
	private <T extends Particle> Queue<T> asyncparticles$newParticleQueue(ParticleRenderType k) {
		return ParticleHelper.newParticleQueue();
	}

	@Unique
	private void asyncparticles$scheduleGpuParticleTick(ParticleRenderType particleRenderType, Queue<TextureSheetParticle> queue) {
		if (queue.isEmpty()) {
			return;
		}
		IParticleRenderer renderer = GpuParticleBehavior.INSTANCE.getRenderer(particleRenderType);
		renderer.mapBuffer();
		AsyncTickBehavior.INSTANCE.PARTICLE_OPERATIONS.add(() -> {
			GpuParticleBehavior.GPU_PARTICLE_PHASE.set(true);
			tickParticleList((Queue) queue);
			GpuParticleBehavior.GPU_PARTICLE_PHASE.set(false);
			AsyncTickBehavior.INSTANCE.doRemoveIf(queue);
			renderer.tick(GpuParticleBehavior.INSTANCE.getCameraPos(), queue);
		});
	}

	@Unique
	private void asyncparticles$scheduleParticleTick(ParticleRenderType particleRenderType, Queue<Particle> queue) {
		if (queue.isEmpty()) {
			return;
		}
		AsyncTickBehavior.INSTANCE.PARTICLE_OPERATIONS.add(() -> tickParticleList(queue));
	}

	@Unique
	private void asyncparticles$tickEmitters() {
		boolean forceDone = ConfigHelper.forceDoneParticleTick();
		for (TrackingEmitter emitter : this.trackingEmitters) {
			if (AsyncTickBehavior.INSTANCE.isCancelled() && !forceDone) {
				return;
			}
			if (!emitter.isAlive()) {
				continue;
			}
			if (!ThreadUtil.isOnRenderThread() &&
				((ParticleAddon) emitter).asyncparticles$isTickSync()) {
				AsyncTickBehavior.INSTANCE.recordSync(emitter);
				continue;
			}
			try {
				emitter.tick();
			} catch (Throwable t) {
				AsyncTickBehavior.INSTANCE.onTickingParticleException(emitter, t);
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
		boolean isOnMainThread = ThreadUtil.isOnRenderThread();
		ParticleCullingMode particleCullingMode = GpuParticleBehavior.GPU_PARTICLE_PHASE.get() ?
			ParticleCullingMode.DISABLED :
			ConfigHelper.getParticleCullingMode();
		boolean forceDone = ConfigHelper.forceDoneParticleTick();
		for (Particle particle : collection) {
			if (AsyncTickBehavior.INSTANCE.isCancelled() && !forceDone) {
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
				shouldRefresh = enableLightCache;
			} else if (particleAddon.asyncparticles$isTicked()) {
				// Skip the first tick after enqueued that the particle is added to the queue.
				// only GPU particles don't skip the first tick, but skip the first refresh.
				// skip the first refresh will fix black destruction gpu particles.
				shouldTick = particleAddon.asyncparticles$isGpu();
				shouldRefresh = !shouldTick && enableLightCache;
			} else if (particleAddon.asyncparticles$isTickSync()) {
				AsyncTickBehavior.INSTANCE.recordSync(particle);
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
					AsyncTickBehavior.INSTANCE.onTickingParticleException(particle, t);
				}
				particleAddon.asyncparticles$setTicked();
			}
			if (shouldRefresh) {
				((LightCachedParticleAddon) particle).asyncparticles$refresh();
			}
			switch (particleCullingMode) {
				case ASYNC_AABB -> particleAddon.asyncparticles$tickAABBCulling();
				case ASYNC_SPHERE -> particleAddon.asyncparticles$tickSphereCulling();
			}
		}
	}

	@Inject(method = "add", at = @At(value = "HEAD"))
	public void add(Particle particle, CallbackInfo ci) {
		if (!AsyncTickBehavior.INSTANCE.isShouldTickParticles() && ConfigHelper.isTickAsync()) {
			particle.remove(); // to compatible with some mods...
			// don't cancel it,
			// otherwise it may cause memory leak with some mods
		} else {
			if (ConfigHelper.particleLightCache()) {
				// Enable the light only if the particle is added to the current ParticleEngine instance.
				((LightCachedParticleAddon) particle).asyncparticles$enableLightCache();
				// refresh the light cache here since this method can run in other threads.
				// so it can avoid to slower the main thread.
				((LightCachedParticleAddon) particle).asyncparticles$refresh();
			}
			switch (ConfigHelper.getParticleCullingMode()) {
				case ASYNC_AABB -> ((ParticleAddon) particle).asyncparticles$tickAABBCulling();
				case ASYNC_SPHERE -> ((ParticleAddon) particle).asyncparticles$tickSphereCulling();
			}
		}
	}

	@Inject(method = "clearParticles", at = @At("HEAD"))
	public void onClearParticles(CallbackInfo ci) {
		particlesToAdd.forEach(AsyncTickBehavior.INSTANCE::onEvicted);
		particlesToAdd = BusyWaitEvictingQueue.newInstance(AsyncParticlesConfig.MIN_PARTICLE_LIMIT, ConfigHelper.getParticleLimit(), AsyncTickBehavior.INSTANCE::onEvicted);
		trackingEmitters.forEach(AsyncTickBehavior.INSTANCE::onEvicted);
		trackingEmitters = BusyWaitEvictingQueue.newInstance(AsyncParticlesConfig.MIN_PARTICLE_LIMIT / 4, ConfigHelper.getParticleLimit(), AsyncTickBehavior.INSTANCE::onEvicted);
		particles.values().forEach(queue -> queue.forEach(AsyncTickBehavior.INSTANCE::onEvicted));
		GpuParticleBehavior.INSTANCE.gpuParticles.values().forEach(queue -> queue.forEach(AsyncTickBehavior.INSTANCE::onEvicted));
		GpuParticleBehavior.INSTANCE.gpuParticles.clear();
		AsyncTickBehavior.INSTANCE.onParticleEngineClear();
	}
}

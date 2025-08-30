package fun.qu_an.minecraft.asyncparticles.client.mixin.core.tick;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.particle.GpuParticles;
import fun.qu_an.minecraft.asyncparticles.client.particle.ParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.ParticleCullingMode;
import fun.qu_an.minecraft.asyncparticles.client.util.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.ParticleGroup;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(value = ParticleEngine.class, priority = 500)
public abstract class MixinParticleEngine implements ParticleEngineAddon {
	@Unique
	private static final ParticleThreadLocal<Boolean> GPU_PARTICLE_PHASE = ParticleThreadLocal.withInitial(() -> false);
	@Shadow
	public Queue<Particle> particlesToAdd;
	@Shadow
	public Map<ParticleRenderType, Queue<Particle>> particles;
	@Shadow
	protected ClientLevel level;
	@Mutable
	@Shadow
	public Queue<TrackingEmitter> trackingEmitters;

	@Shadow
	public abstract void tickParticle(Particle particle);

	@Shadow
	public abstract void updateCount(ParticleGroup group, int count);

	@ModifyExpressionValue(method = "countParticles", at = @At(value = "INVOKE", target = "Ljava/util/stream/IntStream;sum()I"))
	private int modifyCount(int i) {
		return i + GpuParticles.gpuParticles.values().stream().mapToInt(Collection::size).sum();
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
			AsyncTicker.PARTICLE_OPERATIONS.add(this::asyncparticles$tickEmitters);
		}

		// Keep local variable tables as they were
		Particle particle;

		boolean tickAsync = ConfigHelper.isTickAsync();
		if (tickAsync) {
			AsyncTicker.waitForCleanUp();
		} else {
			particles.forEach(this::asyncparticles$scheduleParticleTick);
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
					AsyncTicker.recordSync(particle);
					canComputeFast = false;
				} else {
					canComputeFast = particle instanceof TextureSheetParticle tsp && GpuParticles.canRenderFast(tsp);
				}
				Queue<Particle> queue;
				ParticleRenderType renderType = particle.getRenderType();
				if (canComputeFast) {
					queue = (Queue) GpuParticles.gpuParticles.computeIfAbsent(renderType, k -> {
						this.particles.computeIfAbsent(k, this::asyncparticles$newQueue);
						GpuParticles.createRenderer(k);
						return asyncparticles$newQueue(k);
					});
					if (appendNewParticlesToRenderer) {
						GpuParticles.getRenderer(renderType).append(GpuParticles.getCameraPos(), ((TextureSheetParticle) particle));
					}
				} else {
					queue = this.particles.computeIfAbsent(renderType, this::asyncparticles$newQueue);
					((ParticleAddon) particle).asyncparticles$setTicked();
				}
				queue.add(particle);
			}
			particlesToAdd.clear();
		}
		if (tickAsync) {
			if (gpuParticles) {
				GpuParticles.swapAllBuffers();
				GpuParticles.setInternalParticleLimit(ConfigHelper.getParticleLimit());
				Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
				GpuParticles.setCameraPos(camera.getPosition());
				GpuParticles.gpuParticles.forEach(this::asyncparticles$scheduleGpuParticleTick);
			}
			particles.forEach((particleRenderType, queue) -> {
				if (queue.isEmpty()) {
					return;
				}
				asyncparticles$scheduleParticleTick(particleRenderType, queue);
			});
		}
	}

	@Unique
	private void asyncparticles$scheduleGpuParticleTick(ParticleRenderType particleRenderType, Queue<TextureSheetParticle> queue) {
		if (queue.isEmpty()) {
			return;
		}
		ParticleRenderer renderer = GpuParticles.getRenderer(particleRenderType);
		renderer.mapBuffer();
		AsyncTicker.PARTICLE_OPERATIONS.add(() -> {
			GPU_PARTICLE_PHASE.set(true);
			tickParticleList((Queue) queue);
			GPU_PARTICLE_PHASE.set(false);
			AsyncTicker.doRemoveIf(queue);
			renderer.tick(GpuParticles.getCameraPos(), queue);
		});
	}

	@Unique
	private void asyncparticles$scheduleParticleTick(ParticleRenderType particleRenderType, Queue<Particle> queue) {
		AsyncTicker.PARTICLE_OPERATIONS.add(() -> tickParticleList(queue));
	}

	@Unique
	private <T extends Particle> @NotNull Queue<T> asyncparticles$newQueue(ParticleRenderType k) {
		return GameUtil.newParticleQueue();
	}

	@Unique
	private void asyncparticles$tickEmitters() {
		boolean forceDone = ConfigHelper.forceDoneParticleTick();
		for (TrackingEmitter emitter : this.trackingEmitters) {
			if (AsyncTicker.isCancelled() && !forceDone) {
				return;
			}
			if (!emitter.isAlive()) {
				continue;
			}
			if (!ThreadUtil.isOnMainThread() &&
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
		boolean isNotOnMainThread = !ThreadUtil.isOnMainThread();
		ParticleCullingMode particleCullingMode = GPU_PARTICLE_PHASE.get() ?
			ParticleCullingMode.DISABLED :
			ConfigHelper.getParticleCullingMode();
		boolean forceDone = ConfigHelper.forceDoneParticleTick();
		for (Particle particle : collection) {
			if (AsyncTicker.isCancelled() && !forceDone) {
				return;
			}
			if (!particle.isAlive()) {
				// This is to be compatible with e.g. Figura mod
				// Trust JIT
				Utils.DUMMY_ITERATOR.remove();
				continue;
			}
			if (isNotOnMainThread) {
				if (((ParticleAddon) particle).asyncparticles$isTicked()) {
					// Skip the first tick that the particle is added to the queue.
					if (enableLightCache) {
						((LightCachedParticleAddon) particle).asyncparticles$refresh();
					}
					switch (particleCullingMode) {
						case ASYNC_AABB -> ((ParticleAddon) particle).asyncparticles$tickAABBCulling();
						case ASYNC_SPHERE -> ((ParticleAddon) particle).asyncparticles$tickSphereCulling();
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
			} catch (Throwable t) {
				AsyncTicker.onTickingParticleException(particle, t);
			}
			((ParticleAddon) particle).asyncparticles$setTicked();
			if (enableLightCache) {
				((LightCachedParticleAddon) particle).asyncparticles$refresh();
			}
			switch (particleCullingMode) {
				case ASYNC_AABB -> ((ParticleAddon) particle).asyncparticles$tickAABBCulling();
				case ASYNC_SPHERE -> ((ParticleAddon) particle).asyncparticles$tickSphereCulling();
			}
		}
	}

	@Inject(method = "add", at = @At(value = "HEAD"))
	public void add(Particle particle, CallbackInfo ci) {
		if (!AsyncTicker.shouldTickParticles && ConfigHelper.isTickAsync()) {
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
		particlesToAdd.forEach(AsyncTicker::onEvicted);
		particlesToAdd = BusyWaitEvictingQueue.newInstance(1024, ConfigHelper.getParticleLimit(), AsyncTicker::onEvicted);
		trackingEmitters.forEach(AsyncTicker::onEvicted);
		trackingEmitters = BusyWaitEvictingQueue.newInstance(256, ConfigHelper.getParticleLimit(), AsyncTicker::onEvicted);
		particles.values().forEach(queue -> queue.forEach(AsyncTicker::onEvicted));
		GpuParticles.gpuParticles.values().forEach(queue -> queue.forEach(AsyncTicker::onEvicted));
		GpuParticles.gpuParticles.clear();
		AsyncTicker.onParticleEngineClear();
	}

	//	@Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
	//	public void onCreateParticle(ParticleOptions particleType, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, CallbackInfoReturnable<Particle> cir) {
	//		if (!ModListHelper.CREATE_LOADED && !ModListHelper.VS_LOADED) {
	//			return;
	//		}
	//		ResourceLocation key = BuiltInRegistries.PARTICLE_TYPE.getKey(particleType.getType());
	//		if (!SimplePropertiesConfig.getWeatherParticles().contains(key)) {
	//			return;
	//		}
	//		if (ModListHelper.CREATE_LOADED && !CreateCompat.canSpawnWeatherParticle(level, x, y, z)) {
	//			cir.setReturnValue(null);
	//		}
	//		if (ModListHelper.VS_LOADED && !VSCompat.canCreateWeatherParticle(level, x, y, z)) {
	//			cir.setReturnValue(null);
	//		}
	//	}
}

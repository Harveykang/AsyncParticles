package fun.qu_an.minecraft.asyncparticles.client.particle;

import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.Diagnostic;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.a_good_place.AGoodPlaceCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.ParticleCullingMode;
import fun.qu_an.minecraft.asyncparticles.client.task.EndTickEvent;
import fun.qu_an.minecraft.asyncparticles.client.task.EndTickOperation;
import fun.qu_an.minecraft.asyncparticles.client.util.*;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil.toThrowDirectly;

@Environment(EnvType.CLIENT)
public class AsyncTickBehavior {
	public static final Logger LOGGER = LogManager.getLogger();
	public static final int THREADS = Mth.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, 6);
	public static final String THREAD_PREFIX = "AsyncParticleTicker";
	public static final AsyncTickBehavior INSTANCE = new AsyncTickBehavior();
	//	public final Map<ParticleRenderType, ByteBuffer> UNUPLOADED_BUFFERS = new ConcurrentHashMap<>();
	private final Set<Class<?>> syncParticleTypes = Collections.newSetFromMap(new IdentityHashMap<>());
	private final List<ParticleRenderType> PARALLELLED_RENDER_TYPES = new ReferenceArrayList<>(List.of(
		ParticleRenderType.TERRAIN_SHEET,
		ParticleRenderType.PARTICLE_SHEET_OPAQUE,
		ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT,
		ParticleRenderType.PARTICLE_SHEET_LIT,
		ParticleRenderType.NO_RENDER
	));
	private final Set<Particle> syncParticles = Collections.newSetFromMap(new IdentityHashMap<>());
	public final List<Runnable> particleOperations = new ReferenceArrayList<>();
	private final AtomicBoolean cancelled = new AtomicBoolean(false);
	private boolean shouldTickParticles = false;
	private final List<EndTickEvent> sequencedEndTickEvents = new ReferenceArrayList<>();
	private final List<EndTickEvent> parallelEndTickEvents = new ReferenceArrayList<>();
	private final List<EndTickOperation> sequencedEndTickOperations = new ReferenceArrayList<>();
	private final List<EndTickOperation> parallelEndTickOperations = new ReferenceArrayList<>();
	private boolean debug_cancelled = false;
	private Consumer<String> debugConsumer;
	private boolean shouldReload;
	public final ForkJoinPool executor;
	public final ExceptionTracker<Object> exceptionTracker = new ExceptionTracker<>(
		() -> 5000,
		ConfigHelper::getTickFailurePerSecondThreshold
	);
	private final AtomicLong timeUsageNano = new AtomicLong(0L);
	private boolean particlePhase;

	{
		AtomicInteger workerCount = new AtomicInteger(1);
		executor = new ForkJoinPool(THREADS, (forkJoinPool) -> {
			ForkJoinWorkerThread forkJoinWorkerThread = new AsyncTickerThread(forkJoinPool);
			forkJoinWorkerThread.setName(THREAD_PREFIX + "-" + workerCount.getAndIncrement());
			forkJoinWorkerThread.setDaemon(true);
			return forkJoinWorkerThread;
		}, Util::onThreadException, true);
	}

	private final TaskHelper cleanupTasks = new TaskHelper(executor, ExceptionUtil::toThrowDirectly);
	private final TaskHelper tickTasks = new TaskHelper(executor, this::tickExceptionally);

	public void addTickInParallel(ParticleRenderType particleRenderType) {
		synchronized (PARALLELLED_RENDER_TYPES) {
			PARALLELLED_RENDER_TYPES.add(particleRenderType);
		}
	}

	public boolean canTickInParallel(ParticleRenderType particleRenderType) {
		return PARALLELLED_RENDER_TYPES.contains(particleRenderType);
	}

	/* Ticker */

	public boolean isCancelled() {
		if (!cancelled.getOpaque()) {
			return false;
		}
		debug_cancelled = true;
		return true;
	}

	/**
	 * @param i  Current index of tick loop
	 * @param to Count of ticks to run
	 */
	public void preTick(int i, int to) {
		if (!ConfigHelper.isTickAsync()) {
			return;
		}
		// assert i < to;
		ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
		profiler.push("async_particles");
		Minecraft mc = Minecraft.getInstance();
		boolean levelRunning = mc.level != null && mc.player != null && !mc.isPaused();
		if (i != 0) {
			// tick non-zero, do nothing
			shouldTickParticles = i == to - 1 && levelRunning; // tick particles only on last tick
		} else {
			// head tick, wait for async tasks to complete, cleanup
			if (tickTasks.isRunning()) {
				cancelled.setOpaque(true);
				debug_cancelled = false;
				tickTasks.waitForCompletion();
				cancelled.setOpaque(false);
			}
			shouldTickParticles = i == to - 1 && levelRunning;
			if (levelRunning) {
				ParticleEngine particleEngine = mc.particleEngine;
				Queue<TrackingEmitter> trackingEmitters = particleEngine.trackingEmitters;
				if (!trackingEmitters.isEmpty()) {
					cleanupTasks.addTask(() -> doEmittersRemoveIf(trackingEmitters));
				}
				for (Queue<Particle> particles : particleEngine.particles.values()) {
					if (particles.isEmpty()) {
						continue;
					}
					cleanupTasks.addTask(() -> doRemoveIf(particles));
				}
				cleanupTasks.groupTasks(true);
				cleanupTasks.submitAll();
			}
		}
		profiler.pop();
	}

	public void doRemoveIf(Queue<? extends Particle> queue) {
		if (ConfigHelper.isParallelQueueRemoval()) {
			((IterationSafeEvictingQueue<? extends Particle>) queue)
				.parallelRemoveIf(particle -> this.shouldRemove(particle, ConfigHelper.isRemoveIfMissedTick()),
					ConfigHelper.isParallelQueueEviction(),
					THREADS,
					this.executor);
		} else {
			queue.removeIf(particle -> this.shouldRemove(particle, ConfigHelper.isRemoveIfMissedTick()));
		}
	}

	public void doEmittersRemoveIf(Queue<? extends TrackingEmitter> queue) {
		if (ConfigHelper.isParallelQueueRemoval()) {
			((IterationSafeEvictingQueue<? extends TrackingEmitter>) queue)
				.parallelRemoveIf(particle -> !particle.isAlive(),
					ConfigHelper.isParallelQueueEviction(),
					THREADS,
					this.executor);
		} else {
			queue.removeIf(particle -> !particle.isAlive());
		}
	}

	public boolean shouldRemove(Particle particle1, boolean removeIfMissedTick) {
		if (!particle1.isAlive()) {
			return true;
		}
		ParticleAddon particleAddon = (ParticleAddon) particle1;
		if (particleAddon.asyncparticles$isTickSync()) {
			return false;
		}
		if (particleAddon.asyncparticles$isTicked()) {
			particleAddon.asyncparticles$resetTicked();
			return false;
		}
		return removeIfMissedTick;
	}

	/**
	 * @param i  Current index of tick loop
	 * @param to Count of ticks to run
	 */
	public void postTick(int i, int to) {
		Minecraft mc = Minecraft.getInstance();
		boolean levelRunning = mc.level != null && mc.player != null && !mc.isPaused();
		if (!ConfigHelper.isTickAsync()) {
			tryReload();
			tryDebug();
			if (levelRunning) {
				sequencedEndTickEvents.forEach(Runnable::run);
				parallelEndTickEvents.forEach(Runnable::run);
			}
			if (!sequencedEndTickOperations.isEmpty()) {
				sequencedEndTickOperations.forEach(Runnable::run);
				sequencedEndTickOperations.clear();
			}
			if (!parallelEndTickOperations.isEmpty()) {
				parallelEndTickOperations.forEach(Runnable::run);
				parallelEndTickOperations.clear();
			}
			return;
		}
		// assert i < to;
		ProfilerFiller profiler = mc.getProfiler();
		profiler.push("async_particles");
		if (levelRunning) {
			profiler.push("particle_tick");
			if (i == to - 1) {
				particlePhase = true;
				mc.particleEngine.tick();
				particlePhase = false;
			} else {
				waitForCleanUp();
			}
			profiler.pop();
		}
		if (i == to - 1) {// tail tick, schedule async tasks
			tryReload();
			tryDebug();

			tickTasks.addTask(() -> timeUsageNano.setRelease(System.nanoTime()));
			tickTasks.groupTasks(false);

			List<EndTickOperation> sequencedEndTickOperations = new ReferenceArrayList<>(this.sequencedEndTickOperations);
			this.sequencedEndTickOperations.clear();
			tickTasks.addTask(() -> {
				// 每 tick 结束时都要执行的固定事件
				if (levelRunning) {
					if (!this.sequencedEndTickEvents.isEmpty()) {
						for (Runnable r : this.sequencedEndTickEvents) {
							r.run();
						}
					}
				}
				if (!sequencedEndTickOperations.isEmpty()) {
					for (EndTickOperation o : sequencedEndTickOperations) {
						o.run();
					}
				}
			});
			if (!this.parallelEndTickEvents.isEmpty()) {
				if (levelRunning) {
					tickTasks.addTasks(this.parallelEndTickEvents);
				}
			}
			if (!parallelEndTickOperations.isEmpty()) {
				tickTasks.addTasks(parallelEndTickOperations);
				parallelEndTickOperations.clear();
			}
			tickTasks.groupTasks(true);

			if (!this.particleOperations.isEmpty()) {
				if (levelRunning) {
					List<Runnable> particleOperations = new ReferenceArrayList<>(this.particleOperations);
					tickTasks.addTasks(particleOperations);
					tickTasks.groupTasks(true);
				}
				this.particleOperations.clear();
			}

			tickTasks.addTask(() -> timeUsageNano.setRelease(System.nanoTime() - timeUsageNano.getAcquire()));
			tickTasks.submitAll();
		}
		profiler.pop();
	}

	private void tickExceptionally(Throwable t) {
		if (!(t instanceof Exception e)) {
			throw toThrowDirectly(t);
		}
		Minecraft mc = Minecraft.getInstance();
		if (isTolerable(e) &&
			(mc.level == null || mc.player == null)) {
			LOGGER.warn("Exception while executing tick tasks.", e);
			return;
		}
		StringWriter out = new StringWriter();
		t.printStackTrace(new PrintWriter(out));
		if (out.toString().contains("asyncparticles_animateTick")) {
			Diagnostic.errorDuringAsyncAnimateTick(e);
			LOGGER.warn("Exception while executing tick tasks.", e);
			return;
		}
		throw toThrowDirectly(t);
	}

	public boolean isTolerable(@NotNull Throwable e) {
		if (!(e instanceof Exception)) {
			return false;
		}
		Throwable rootCause = ExceptionUtil.getRootCause(e);
		return rootCause instanceof MissingPaletteEntryException
			|| rootCause instanceof NullPointerException
			|| rootCause instanceof IndexOutOfBoundsException
			|| rootCause instanceof ArrayIndexOutOfBoundsException
			|| (rootCause instanceof ConcurrentModificationException && ConfigHelper.suppressCME());
	}

	public void onTickingParticleException(Particle particle, Throwable t) {
		if (ThreadUtil.isOnRenderThread()) {
			throw constructCrashReport(particle, t);
		}
		boolean tolerable = isTolerable(t);
		Class<? extends Particle> particleClass = ((ParticleAddon) particle).asyncparticles$getRealClass();
		if (tolerable && !exceptionTracker.addException(particleClass, t)) {
			return;
		}
		if (ConfigHelper.markSyncIfTickFailed()) {
			((ParticleAddon) particle).asyncparticles$setTickSync();
			if (!shouldSync(particleClass)) {
				if (!tolerable) {
					LOGGER.warn("Exception while ticking particle {}, marking as sync", particle, t);
				} else {
					LOGGER.warn("Exception {} thrown while ticking particle {} exceeds the threshold, please contact the author: {}",
						t.getClass().getName(),
						particle,
						AsyncParticlesClient.ISSUE_URL,
						t);
				}
				markAsSync(particleClass);
			}
			recordSync(particle);
		} else if (tolerable) {
			throw constructCrashReport(particle, new RuntimeException(
				"Exception %s thrown while ticking particle %s, exceeds the threshold, please contact the author: %s"
					.formatted(
						t.getClass().getName(),
						particle,
						AsyncParticlesClient.ISSUE_URL),
				t));
		} else {
			throw constructCrashReport(particle, t);
		}
	}

	public void onParticleEngineClear() {
		// fix a good placement mod block invisible
		if (ModListHelper.A_GOOD_PLACE_LOADED) {
			AGoodPlaceCompat.onParticleEngineClear();
		}
		// fix particlerain's particle count management
		if (ModListHelper.PARTICLERAIN_LOADED) {
			ParticleRainCompat.particleCount.set(0);
		}
	}

	public void waitForCleanUp() {
		if (cleanupTasks.isRunning()) {
			cleanupTasks.waitForCompletion();
		}
	}

	public ReportedException constructCrashReport(Particle particle, Throwable t) {
		debugLater(LOGGER::info);
		tryDebug();
		AsyncRenderBehavior.INSTANCE.debugLater(LOGGER::info);
		AsyncRenderBehavior.INSTANCE.tryDebug();
		CrashReport crashReport = CrashReport.forThrowable(t, "Ticking Particle");
		CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being ticked");
		crashReportCategory.setDetail("Particle", particle::toString);
		crashReportCategory.setDetail("Particle Type", particle.getRenderType()::toString);
		return new ReportedException(crashReport);
	}

	/* Sync Ticking */

	public void tickSyncParticles() {
		if ((!isShouldTickParticles() && ConfigHelper.isTickAsync()) || syncParticles.isEmpty()) {
			return;
		}
		ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
		ParticleCullingMode particleCullingMode = ConfigHelper.getParticleCullingMode();
		for (Iterator<Particle> iterator = syncParticles.iterator(); iterator.hasNext(); ) {
			Particle particle = iterator.next();
			try {
				particleEngine.tickParticle(particle);
				if (!(particle instanceof TrackingEmitter)) {
					((LightCachedParticleAddon) particle).asyncparticles$tickLightCache();
					switch (particleCullingMode) {
						case ASYNC_AABB -> ((ParticleAddon) particle).asyncparticles$tickAABBCulling();
						case ASYNC_SPHERE -> ((ParticleAddon) particle).asyncparticles$tickSphereCulling();
					}
					((ParticleAddon) particle).asyncparticles$setTicked();
				}
			} catch (Throwable e) {
				throw constructCrashReport(particle, e);
			}
			if (!particle.isAlive()) {
				// we manage the count in cleanup task
				//				particle.getParticleGroup().ifPresent((particleGroup) -> particleEngine.updateCount(particleGroup, -1));
				iterator.remove();
			}
		}
	}

	public void markAsSync(Class<? extends Particle> aClass) {
		synchronized (syncParticleTypes) {
			syncParticleTypes.add(aClass);
		}
	}

	public boolean shouldSync(Class<?> aClass) {
		return syncParticleTypes.contains(aClass);
	}

	public void recordSync(Particle particle) {
		synchronized (syncParticles) {
			syncParticles.add(particle);
		}
	}

	public void onEvicted(Particle particle) {
		particle.getParticleGroup().ifPresent(g -> Minecraft.getInstance().particleEngine.updateCount(g, -1));
		if (particle.isAlive()) {
			particle.remove();
		}
	}

	/* Debug/Reload */

	public void tryDebug() {
		if (debugConsumer == null) {
			return;
		}
		debugConsumer.accept(String.format("""
			[Debug AsyncTicker]
			last tick duration: %.1f ms,
			interrupted: %s,
			particle operations: %d,
			end tick events: %s,
			end tick operations: %s,
			max particles queue size: %d,
			particles queue size/allocated: %s,
			GPU particles size/allocated: %s,
			particles to add size: %d
			sync particle count: %d,
			sync particle types: %s,"""
			.formatted(ConfigHelper.isTickAsync() ? timeUsageNano.getAcquire() / 1000000d : Double.NaN,
				debug_cancelled,
				particleOperations.size(),
				sequencedEndTickEvents + parallelEndTickEvents.toString(),
				sequencedEndTickOperations,
				ConfigHelper.getParticleLimit(),
				Minecraft.getInstance().particleEngine.particles.entrySet()
					.stream().map(e -> {
						Queue<Particle> queue = e.getValue();
						return e.getKey() + ": " + queue.size() + "/" + ((IterationSafeEvictingQueue<Particle>) queue).arraySize();
					}).toList(),
				GpuParticleBehavior.INSTANCE.gpuParticles.entrySet()
					.stream().map(e -> {
						Queue<TextureSheetParticle> queue = e.getValue();
						return e.getKey() + ": " + queue.size() + "/" + ((IterationSafeEvictingQueue<TextureSheetParticle>) queue).arraySize();
					}).toList(),
				Minecraft.getInstance().particleEngine.particlesToAdd.size(),
				syncParticles.size(),
				syncParticleTypes.stream().map(Class::getName).toList())));
		debugConsumer = null;
	}

	public void debugLater(Consumer<String> consumer) {
		debugConsumer = consumer;
	}

	public void dumpParticles() {
		ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
		LOGGER.info((ModListHelper.IS_FORGE ? particleEngine.particles.keySet() : ParticleEngine.RENDER_ORDER)
			.stream()
			.collect(Collectors.toMap(ParticleRenderType::getClass, Object::toString)));
		LOGGER.info(particleEngine.particles);
		LOGGER.info(GpuParticleBehavior.INSTANCE.gpuParticles);
	}

	public void reloadLater() {
		shouldReload = true;
	}

	private void tryReload() {
		if (shouldReload) {
			reload(false);
			shouldReload = false;
		}
	}

	public void reload(boolean clearParticles) {
		AsyncRenderBehavior.INSTANCE.reset();
		ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
		if (clearParticles) {
			reset();
			particleEngine.clearParticles();
		} else {
			Queue<Particle> newToAdd = BusyWaitEvictingQueue.newInstance(AsyncParticlesConfig.MIN_PARTICLE_LIMIT, ConfigHelper.getParticleLimit(), this::onEvicted);
			newToAdd.addAll(particleEngine.particlesToAdd);
			particleEngine.particlesToAdd = newToAdd;
			Queue<TrackingEmitter> newEmitters = BusyWaitEvictingQueue.newInstance(AsyncParticlesConfig.MIN_PARTICLE_LIMIT / 4, ConfigHelper.getParticleLimit(), this::onEvicted);
			newEmitters.addAll(particleEngine.trackingEmitters);
			particleEngine.trackingEmitters = newEmitters;
			boolean enableLightCache = ConfigHelper.particleLightCache();
			ParticleCullingMode particleCullingMode = ConfigHelper.getParticleCullingMode();
			Map<ParticleRenderType, Queue<Particle>> newCpuParticleMap = new Reference2ObjectOpenHashMap<>();
			Map<ParticleRenderType, Queue<TextureSheetParticle>> newGpuParticleMap = new Reference2ObjectOpenHashMap<>();
			boolean tickAsync = ConfigHelper.isTickAsync();
			boolean enableGpuAcceleration = ConfigHelper.isGpuParticles();
			particleEngine.particles.forEach((key, oldCpuQueue) -> {
				Queue<Particle> newCpuQueue = ParticleHelper.newParticleQueue();
				oldCpuQueue.forEach(p -> {
					if (enableLightCache) {
						ParticleHelper.doFirstRefresh(p);
					} else {
						((LightCachedParticleAddon) p).asyncparticles$disableLightCache();
					}
					switch (particleCullingMode) {
						case ASYNC_AABB -> ((ParticleAddon) p).asyncparticles$tickAABBCulling();
						case ASYNC_SPHERE -> ((ParticleAddon) p).asyncparticles$tickSphereCulling();
					}
					if (tickAsync && enableGpuAcceleration &&
						p instanceof TextureSheetParticle tsp && GpuParticleBehavior.INSTANCE.canRenderFast(tsp)) {
						newGpuParticleMap.computeIfAbsent(key, k -> {
							GpuParticleBehavior.INSTANCE.createRenderer(k);
							return ParticleHelper.newParticleQueue();
						}).add(tsp);
					} else {
						newCpuQueue.add(p);
					}
				});
				newCpuParticleMap.put(key, newCpuQueue);
			});
			particleEngine.particles.putAll(newCpuParticleMap);

			if (!tickAsync || !enableGpuAcceleration) {
				GpuParticleBehavior.INSTANCE.gpuParticles.forEach((t, oldGpuQueue) -> {
					Queue<Particle> cpuQueue = newCpuParticleMap.computeIfAbsent(t, k -> ParticleHelper.newParticleQueue());
					cpuQueue.addAll(oldGpuQueue);
				});
				GpuParticleBehavior.INSTANCE.gpuParticles.clear();
			} else {
				GpuParticleBehavior.INSTANCE.gpuParticles.forEach((key, oldGpuQueue) -> {
					oldGpuQueue.forEach(p -> {
						if (enableLightCache) {
							ParticleHelper.doFirstRefresh(p);
						} else {
							((LightCachedParticleAddon) p).asyncparticles$disableLightCache();
						}
						switch (particleCullingMode) {
							case ASYNC_AABB -> ((ParticleAddon) p).asyncparticles$tickAABBCulling();
							case ASYNC_SPHERE -> ((ParticleAddon) p).asyncparticles$tickSphereCulling();
						}
					});
					newGpuParticleMap.computeIfAbsent(key, k -> {
						GpuParticleBehavior.INSTANCE.createRenderer(k);
						return ParticleHelper.newParticleQueue();
					}).addAll(oldGpuQueue);
				});
				GpuParticleBehavior.INSTANCE.gpuParticles.putAll(newGpuParticleMap);
			}
		}
	}

	public void reset() {
		try {
			waitForCleanUp();
		} catch (Exception e) {
			LOGGER.error("Error wating for cleanup task while resetting async ticker", e);
		}
		if (tickTasks.isRunning()) {
			cancelled.setOpaque(true);
			tickTasks.waitForCompletion();
		}
		cancelled.setOpaque(false);
		timeUsageNano.set(0L);
		particleOperations.clear();
		sequencedEndTickOperations.clear();
		syncParticles.clear();
		syncParticleTypes.clear();
		syncParticleTypes.addAll(ConfigHelper.getTickSyncParticleClasses());
	}

	/* Events */

	@ApiStatus.Internal
	public void registerEvent(EndTickEvent task) {
		if (task.isParallel()) {
			synchronized (parallelEndTickEvents) {
				parallelEndTickEvents.add(task);
				// also sort the unordered events. To determine the order of submission of asynchronous tasks
				parallelEndTickEvents.sort(Comparator.comparingInt(EndTickEvent::getPriority));
			}
		} else {
			synchronized (sequencedEndTickEvents) {
				sequencedEndTickEvents.add(task);
				sequencedEndTickEvents.sort(Comparator.comparingInt(EndTickEvent::getPriority));
			}
		}
	}

	@ApiStatus.Internal
	public void scheduleOperation(EndTickOperation task) {
		if (!isShouldTickParticles() && ConfigHelper.isTickAsync()) {
			return;
		}
		if (ThreadUtil.isOnRenderThread()) {
			if (task.isParallel()) {
				parallelEndTickOperations.add(task);
			} else {
				sequencedEndTickOperations.add(task);
			}
		} else {
			ThreadUtil.enqueueClientTask(() -> {
				if (task.isParallel()) {
					parallelEndTickOperations.add(task);
				} else {
					sequencedEndTickOperations.add(task);
				}
			});
		}
	}

	public boolean isShouldTickParticles() {
		return shouldTickParticles;
	}

	public boolean isParticlePhase() {
		return particlePhase;
	}

	public boolean shouldTickParticleEngine() {
		if (isParticlePhase()) {
			return true;
		}
		if (!ConfigHelper.isTickAsync()) {
			return !ModListHelper.IMMERSIVE_PORTALS_LOADED
				|| Minecraft.getInstance().player.level() == Minecraft.getInstance().level;
		}
		if (ModListHelper.IMMERSIVE_PORTALS_LOADED) {
			return false;
		}
		throw new IllegalStateException("ParticleEngine.tick() called outside the particle phase unexpectedly.");
	}

	public static class AsyncTickerThread extends AsyncParticleWorkerThread {
		public AsyncTickerThread(ForkJoinPool forkJoinPool) {
			super(forkJoinPool);
		}

		protected void onTermination(Throwable throwable) {
			if (throwable != null) {
				LOGGER.warn("{} died", this.getName(), throwable);
			} else {
				LOGGER.debug("{} shutdown", this.getName());
			}

			super.onTermination(throwable);
		}
	}
}

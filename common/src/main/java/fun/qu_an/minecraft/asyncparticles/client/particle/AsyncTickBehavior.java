package fun.qu_an.minecraft.asyncparticles.client.particle;

import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.a_good_place.AGoodPlaceCompat;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.ParticleCullingMode;
import fun.qu_an.minecraft.asyncparticles.client.task.EndTickEvent;
import fun.qu_an.minecraft.asyncparticles.client.task.EndTickOperation;
import fun.qu_an.minecraft.asyncparticles.client.util.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil.toThrowDirectly;

// TODO: Organize this shit
@Environment(EnvType.CLIENT)
public class AsyncTickBehavior {
	public static final AsyncTickBehavior INSTANCE = new AsyncTickBehavior();
	public static final Logger LOGGER = LogManager.getLogger();
	private  final Set<Class<?>> SYNC_PARTICLE_TYPES = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Set<Particle> SYNC_PARTICLES = Collections.newSetFromMap(new IdentityHashMap<>());
	public final List<Runnable> PARTICLE_OPERATIONS = new ArrayList<>();
	private final AtomicBoolean cancelled = new AtomicBoolean(false);
	public boolean shouldTickParticles = false;
	public CompletableFuture<Void> particleCleanup;
	private final List<EndTickEvent> SEQUENCED_END_TICK_EVENTS = new ArrayList<>();
	private final List<EndTickEvent> PARALLEL_END_TICK_EVENTS = new ArrayList<>();
	private final List<EndTickOperation> END_TICK_OPERATIONS = new ArrayList<>();
	private CompletableFuture<Void> particleFuture;
	private boolean debug_cancelled = false;
	private Consumer<String> debugConsumer;
	private boolean shouldReload;
	public final ForkJoinPool EXECUTOR;
	public final String THREAD_PREFIX = "AsyncParticleTicker";
	private final ExceptionTracker<Object> EXCEPTION_TRACKER = new ExceptionTracker<>(
		() -> 5000,
		ConfigHelper::getTickFailurePerSecondThreshold
	);
	private final AtomicLong timeUsageNano = new AtomicLong(0L);

	{
		AtomicInteger workerCount = new AtomicInteger(1);
		int clamp = Mth.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, 6);
		EXECUTOR = new ForkJoinPool(clamp, (forkJoinPool) -> {
			ForkJoinWorkerThread forkJoinWorkerThread = new AsyncTickerThread(forkJoinPool);
			forkJoinWorkerThread.setName(THREAD_PREFIX + "-" + workerCount.getAndIncrement());
			forkJoinWorkerThread.setDaemon(true);
			return forkJoinWorkerThread;
		}, Util::onThreadException, true);
	}

	private void addSyncByClassName(String className) {
		try {
			SYNC_PARTICLE_TYPES.add((Class<? extends Particle>) Class.forName(className));
		} catch (Exception e) {
			LOGGER.warn("", e);
		}
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
	public void onTickBefore(int i, int to) {
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
			// tick zero, wait for async tasks to complete, cleanup
			if (particleFuture != null) {
				cancelled.setOpaque(true);
				debug_cancelled = false;
				particleFuture.join();
				particleFuture = null;
				cancelled.setOpaque(false);
			}
			shouldTickParticles = i == to - 1 && levelRunning;
			if (levelRunning) {
				ParticleEngine particleEngine = mc.particleEngine;
				Collection<Queue<Particle>> values = particleEngine.particles.values();
				CompletableFuture<?>[] futures = new CompletableFuture[values.size() + 1];
				Queue<TrackingEmitter> trackingEmitters = particleEngine.trackingEmitters;
				if (trackingEmitters.isEmpty()) {
					futures[0] = Utils.NULL_FUTURE;
				} else {
					futures[0] = CompletableFuture.runAsync(() ->
						trackingEmitters.removeIf(trackingEmitter -> !trackingEmitter.isAlive()), EXECUTOR);
				}
				int k = 1;
				boolean removeIfMissedTick = ConfigHelper.isRemoveIfMissedTick();
				for (Queue<Particle> particles : values) {
					if (particles.isEmpty()) {
						futures[k++] = Utils.NULL_FUTURE;
						continue;
					}
					futures[k++] = CompletableFuture.runAsync(() -> particles.removeIf(particle1 -> {
						if (!particle1.isAlive()) {
							// make sure the tracked count is correct
							particle1.getParticleGroup().ifPresent(
								group -> particleEngine.updateCount(group, -1));
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
						if (removeIfMissedTick) {
							particle1.remove();
							// make sure the tracked count is correct
							particle1.getParticleGroup().ifPresent(
								group -> particleEngine.updateCount(group, -1));
							return true;
						}
						return false;
					}), EXECUTOR);
				}
				particleCleanup = CompletableFuture.allOf(futures);
			}
		}
		profiler.pop();
	}

	/**
	 * @param i  Current index of tick loop
	 * @param to Count of ticks to run
	 */
	public void onTickAfter(int i, int to) {
		Minecraft mc = Minecraft.getInstance();
		boolean levelRunning = mc.level != null && mc.player != null && !mc.isPaused();
		if (!ConfigHelper.isTickAsync()) {
			tryReload();
			tryDebug();
			END_TICK_OPERATIONS.forEach(Runnable::run);
			END_TICK_OPERATIONS.clear();
			if (levelRunning) {
				SEQUENCED_END_TICK_EVENTS.forEach(Runnable::run);
				PARALLEL_END_TICK_EVENTS.forEach(Runnable::run);
			}
			return;
		}
		// assert i < to;
		ProfilerFiller profiler = mc.getProfiler();
		profiler.push("async_particles");
		if (levelRunning) {
			profiler.push("particle_tick");
			if (i == to - 1) {
				mc.particleEngine.tick();
			} else {
				waitForCleanUp();
			}
			profiler.pop();
		}
		if (i != to - 1) {
			return;
		}
		// tick last, schedule async tasks
		tryReload();
		tryDebug();
		CompletableFuture<Void> particleFuture = CompletableFuture.runAsync(() -> timeUsageNano.setRelease(System.nanoTime()), EXECUTOR);
		CompletableFuture<Void> sequencedTaskFuture = particleFuture;
		CompletableFuture<?> parallelEventsFuture = Utils.NULL_FUTURE;
		CompletableFuture<?> parallelOperationsFuture = Utils.NULL_FUTURE;
		// end tick events
		if (levelRunning) {
			sequencedTaskFuture = sequencedTaskFuture.thenRun(() -> {
				// 每 tick 结束时都要执行的固定事件
				for (Runnable endTickEvent : SEQUENCED_END_TICK_EVENTS) {
					try {
						endTickEvent.run();
					} catch (Exception e) {
						if (!isTolerable(e) || EXCEPTION_TRACKER.addException(endTickEvent, e)) {
							throw e;
						}
					}
				}
			}).exceptionally(this::tickExceptionally);
			parallelEventsFuture = particleFuture.thenCompose(v -> {
				// 每 tick 结束时都要执行的固定事件，可在 tick 间的任意时刻执行
				@SuppressWarnings("rawtypes")
				CompletableFuture[] completableFutures = new CompletableFuture[PARALLEL_END_TICK_EVENTS.size()];
				int j = 0;
				for (Runnable endTickEvent : PARALLEL_END_TICK_EVENTS) {
					completableFutures[j++] = CompletableFuture.runAsync(endTickEvent, EXECUTOR)
						.exceptionally(e -> {
							if (!isTolerable(e) || EXCEPTION_TRACKER.addException(endTickEvent, e)) {
								throw toThrowDirectly(e);
							}
							return null;
						});
				}
				return CompletableFuture.allOf(completableFutures);
			}).exceptionally(this::tickExceptionally);
		}

		// end tick operations
		List<EndTickOperation> endTickOperations = END_TICK_OPERATIONS;
		if (!endTickOperations.isEmpty()) {
			EndTickOperation[] endTickTasks = endTickOperations.toArray(new EndTickOperation[0]);
			endTickOperations.clear();
			sequencedTaskFuture = sequencedTaskFuture.thenRun(() -> {
				// 每 tick 添加的不固定操作
				for (EndTickOperation endTickTask : endTickTasks) {
					if (!endTickTask.isParallel()) {
						try {
							endTickTask.run();
						} catch (Exception e) {
							if (!isTolerable(e) || EXCEPTION_TRACKER.addException(endTickTask.getId(), e)) {
								throw e;
							}
						}
					}
				}
			}).exceptionally(this::tickExceptionally);
			parallelOperationsFuture = particleFuture.thenCompose(v -> {
				// 每 tick 结束时都要执行的固定事件，可在 tick 间的任意时刻执行
				@SuppressWarnings("rawtypes")
				CompletableFuture[] futures = new CompletableFuture[endTickTasks.length];
				int j = 0;
				for (EndTickOperation endTickTask : endTickTasks) {
					if (endTickTask.isParallel()) {
						futures[j++] = CompletableFuture.runAsync(endTickTask, EXECUTOR)
							.exceptionally(e -> {
								if (!isTolerable(e) || EXCEPTION_TRACKER.addException(endTickTask.getId(), e)) {
									throw toThrowDirectly(e);
								}
								return null;
							});
					}
				}
				return j == 0 ? Utils.nullFuture() : CompletableFuture.allOf(Arrays.copyOf(futures, j));
			}).exceptionally(this::tickExceptionally);
		}
		sequencedTaskFuture = CompletableFuture.allOf(sequencedTaskFuture, parallelEventsFuture, parallelOperationsFuture);

		// tick particles
		List<Runnable> particleOperations = PARTICLE_OPERATIONS;
		if (!particleOperations.isEmpty()) {
			if (!levelRunning) {
				particleOperations.clear();
			} else {
				Runnable[] particleTasks = particleOperations.toArray(new Runnable[0]);
				particleOperations.clear();
				Function<Void, CompletableFuture<Void>> function = v -> CompletableFuture.allOf(Arrays.stream(particleTasks)
					.map(runnable -> CompletableFuture.runAsync(runnable, EXECUTOR))
					.toArray(CompletableFuture[]::new)).exceptionally(this::tickExceptionally);
				sequencedTaskFuture = sequencedTaskFuture.thenCompose(function);
			}
		}

		this.particleFuture = sequencedTaskFuture
			.thenRunAsync(() -> timeUsageNano.setRelease(System.nanoTime() - timeUsageNano.getAcquire()), EXECUTOR);

		profiler.pop();
	}

	private Void tickExceptionally(Throwable e) {
		if (!(e instanceof Exception)) {
			throw toThrowDirectly(e);
		}
		Minecraft mc = Minecraft.getInstance();
		if (!isTolerable(e) ||
			(mc.level != null && mc.player != null && mc.getCameraEntity() != null)) {
			ReportedException reportedException = GameUtil.getReportedException(e);
			throw ExceptionUtil.toThrowDirectly(reportedException == null ? e : reportedException);
		}
		LOGGER.warn("Exception while executing tick tasks.", e);
		return null;
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
		if (tolerable && !EXCEPTION_TRACKER.addException(particleClass, t)) {
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
		// this fix a good placement mod block invisible
		if (ModListHelper.A_GOOD_PLACE_LOADED) {
			AGoodPlaceCompat.onParticleEngineClear();
		}
	}

	public void waitForCleanUp() {
		if (this.particleCleanup != null) {
			this.particleCleanup.join();
			this.particleCleanup = null;
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
		if ((!shouldTickParticles && ConfigHelper.isTickAsync()) || SYNC_PARTICLES.isEmpty()) {
			return;
		}
		ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
		boolean enableLightCache = ConfigHelper.particleLightCache();
		ParticleCullingMode particleCullingMode = ConfigHelper.getParticleCullingMode();
		for (Iterator<Particle> iterator = SYNC_PARTICLES.iterator(); iterator.hasNext(); ) {
			Particle particle = iterator.next();
			try {
				particleEngine.tickParticle(particle);
				if (!(particle instanceof TrackingEmitter)) {
					if (enableLightCache) {
						((LightCachedParticleAddon) particle).asyncparticles$refresh();
					}
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
		synchronized (SYNC_PARTICLE_TYPES) {
			SYNC_PARTICLE_TYPES.add(aClass);
		}
	}

	public boolean shouldSync(Class<?> aClass) {
		return SYNC_PARTICLE_TYPES.contains(aClass);
	}

	public void recordSync(Particle particle) {
		synchronized (SYNC_PARTICLES) {
			SYNC_PARTICLES.add(particle);
		}
	}

	public void onEvicted(Particle particle) {
		particle.getParticleGroup().ifPresent(g -> Minecraft.getInstance().particleEngine.updateCount(g, -1));
		if (particle.isAlive()) {
			particle.remove();
		}
	}

	/* Debug/Reload */

	void tryDebug() {
		if (debugConsumer == null) {
			return;
		}
		debugConsumer.accept(String.format("""
			[Debug AsyncTicker]
			last tick duration: %.1f ms,
			interrupted: %s,
			particle operations: %d,
			end tick events: %d,
			end tick operations: %d,
			max particles queue size: %d,
			particles queue size/allocated: %s,
			particles to add size: %d
			sync particle count: %d,
			sync particle types: %s,"""
			.formatted(ConfigHelper.isTickAsync() ? timeUsageNano.getAcquire() / 1000000d : Double.NaN,
				debug_cancelled,
				PARTICLE_OPERATIONS.size(),
				SEQUENCED_END_TICK_EVENTS.size() + PARALLEL_END_TICK_EVENTS.size(),
				END_TICK_OPERATIONS.size(),
				ConfigHelper.getParticleLimit(),
				Minecraft.getInstance().particleEngine.particles.entrySet()
					.stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
						Queue<Particle> queue = e.getValue();
						return queue.size() + "/" + ((IterationSafeEvictingQueue<Particle>) queue).arraySize();
					})),
				Minecraft.getInstance().particleEngine.particlesToAdd.size(),
				SYNC_PARTICLES.size(),
				SYNC_PARTICLE_TYPES.stream().map(Class::getName).toList())));
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
			Queue<Particle> newToAdd = BusyWaitEvictingQueue.newInstance(1024, ConfigHelper.getParticleLimit(), this::onEvicted);
			newToAdd.addAll(particleEngine.particlesToAdd);
			particleEngine.particlesToAdd = newToAdd;
			Queue<TrackingEmitter> newEmitters = BusyWaitEvictingQueue.newInstance(256, ConfigHelper.getParticleLimit(), this::onEvicted);
			newEmitters.addAll(particleEngine.trackingEmitters);
			particleEngine.trackingEmitters = newEmitters;
			boolean enableLightCache = ConfigHelper.particleLightCache();
			ParticleCullingMode particleCullingMode = ConfigHelper.getParticleCullingMode();
			particleEngine.particles.entrySet().forEach(entry -> {
				Queue<Particle> queue = entry.getValue();
				Queue<Particle> newQueue = IterationSafeEvictingQueue.newInstance(queue.size(), ConfigHelper.getParticleLimit(), this::onEvicted);
				newQueue.addAll(queue);
				newQueue.forEach(p -> {
					if (enableLightCache) {
						((LightCachedParticleAddon) p).asyncparticles$enableLightCache();
						((LightCachedParticleAddon) p).asyncparticles$refresh();
					} else {
						((LightCachedParticleAddon) p).asyncparticles$disableLightCache();
					}
					switch (particleCullingMode) {
						case ASYNC_AABB -> ((ParticleAddon) p).asyncparticles$tickAABBCulling();
						case ASYNC_SPHERE -> ((ParticleAddon) p).asyncparticles$tickSphereCulling();
					}
				});
				entry.setValue(newQueue);
			});
		}
	}

	public void reset() {
		try {
			waitForCleanUp();
		} catch (Exception e) {
			LOGGER.error("Error wating for cleanup task while resetting async ticker", e);
		}
		if (particleFuture != null) {
			cancelled.setOpaque(true);
			try {
				particleFuture.join();
			} catch (Exception e) {
				LOGGER.error("Error wating for particle future while resetting async ticker", e);
			}
			particleFuture = null;
		}
		cancelled.setOpaque(false);
		timeUsageNano.set(0L);
		PARTICLE_OPERATIONS.clear();
		END_TICK_OPERATIONS.clear();
		SYNC_PARTICLES.clear();
		SYNC_PARTICLE_TYPES.clear();
		SYNC_PARTICLE_TYPES.addAll(ConfigHelper.getTickSyncParticleClasses());
	}

	/* Events */

	@ApiStatus.Internal
	public void registerEvent(EndTickEvent task) {
		if (task.isParallel()) {
			synchronized (PARALLEL_END_TICK_EVENTS) {
				PARALLEL_END_TICK_EVENTS.add(task);
				// also sort the unordered events. To determine the order of submission of asynchronous tasks
				PARALLEL_END_TICK_EVENTS.sort(Comparator.comparingInt(EndTickEvent::getPriority));
			}
		} else {
			synchronized (SEQUENCED_END_TICK_EVENTS) {
				SEQUENCED_END_TICK_EVENTS.add(task);
				SEQUENCED_END_TICK_EVENTS.sort(Comparator.comparingInt(EndTickEvent::getPriority));
			}
		}
	}

	@ApiStatus.Internal
	public void scheduleOperation(EndTickOperation task) {
		if (!shouldTickParticles && ConfigHelper.isTickAsync()) {
			return;
		}
		if (ThreadUtil.isOnRenderThread()) {
			END_TICK_OPERATIONS.add(task);
		} else {
			// FIXME: This should not happen.
			ThreadUtil.enqueueClientTask(() -> END_TICK_OPERATIONS.add(task));
		}
	}

	public class AsyncTickerThread extends AsyncParticleWorkerThread {
		public AsyncTickerThread(ForkJoinPool forkJoinPool) {
			super(forkJoinPool);
		}

		@Override
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

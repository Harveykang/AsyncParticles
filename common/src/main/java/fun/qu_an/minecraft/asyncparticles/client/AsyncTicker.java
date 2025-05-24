package fun.qu_an.minecraft.asyncparticles.client;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.api.EndTickEvent;
import fun.qu_an.minecraft.asyncparticles.client.api.EndTickOperation;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.a_good_place.AGoodPlaceCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.*;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil.toThrowDirectly;

// TODO: 整理这一坨
public class AsyncTicker {
	public static final Logger LOGGER = LogManager.getLogger();
	private static final Set<Class<? extends Particle>> SYNC_PARTICLE_TYPES = Collections.newSetFromMap(new IdentityHashMap<>());
	private static final Set<Particle> SYNC_PARTICLES = Collections.newSetFromMap(new IdentityHashMap<>());
	public static final List<Runnable> PARTICLE_OPERATIONS = new ArrayList<>();
	private static boolean cancelled = false;
	public static boolean shouldTickParticles = false;
	public static CompletableFuture<Void> particleCleanup;
	private final static List<EndTickEvent> ORDERED_END_TICK_EVENTS = new ArrayList<>();
	private static final List<EndTickEvent> UNORDERED_END_TICK_EVENTS = new ArrayList<>();
	private static final List<EndTickOperation> END_TICK_OPERATIONS = new ArrayList<>();
	private static CompletableFuture<Void> particleFuture;
	private static boolean debug_cancelled = false;
	private static Consumer<String> debugConsumer;
	private static boolean shouldReload;
	public static final ExecutorService EXECUTOR;
	public static final String THREAD_PREFIX = "AsyncParticleTicker";
	public static final ExceptionTracker<Object> EXCEPTION_TRACKER = new ExceptionTracker<>(
		() -> 5000,
		ConfigHelper::getTickFailurePerSecondThreshold
	);
	private static final LongRef timeUsageNano = new LongRef(0L);

	static {
		AtomicInteger workerCount = new AtomicInteger(1);
		int clamp = Mth.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, 6);
		EXECUTOR = new ForkJoinPool(clamp, (forkJoinPool) -> {
			ForkJoinWorkerThread forkJoinWorkerThread = new ForkJoinWorkerThread(forkJoinPool) {
				protected void onTermination(Throwable throwable) {
					if (throwable != null) {
						LOGGER.warn("{} died", this.getName(), throwable);
					} else {
						LOGGER.debug("{} shutdown", this.getName());
					}

					super.onTermination(throwable);
				}
			};
			forkJoinWorkerThread.setName(THREAD_PREFIX + "-" + workerCount.getAndIncrement());
			forkJoinWorkerThread.setDaemon(true);
			return forkJoinWorkerThread;
		}, Util::onThreadException, true);
	}

	private static void addSyncByClassName(String className) {
		try {
			SYNC_PARTICLE_TYPES.add((Class<? extends Particle>) Class.forName(className));
		} catch (Exception e) {
			LOGGER.warn("", e);
		}
	}

	/* Ticker */

	public static boolean isCancelled() {
		if (!cancelled) {
			return false;
		}
		debug_cancelled = true;
		return true;
	}

	/**
	 * @param i  Current index of tick loop
	 * @param to Count of ticks to run
	 */
	public static void onTickBefore(int i, int to) {
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
			cancelled = true;
			debug_cancelled = false;
			if (particleFuture != null) {
				particleFuture.join();
				particleFuture = null;
			}
			cancelled = false;
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
	public static void onTickAfter(int i, int to) {
		Minecraft mc = Minecraft.getInstance();
		boolean levelRunning = mc.level != null && mc.player != null && !mc.isPaused();
		if (!ConfigHelper.isTickAsync()) {
			tryReload();
			tryDebug();
			END_TICK_OPERATIONS.forEach(Runnable::run);
			END_TICK_OPERATIONS.clear();
			if (levelRunning) {
				ORDERED_END_TICK_EVENTS.forEach(Runnable::run);
				UNORDERED_END_TICK_EVENTS.forEach(Runnable::run);
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
		CompletableFuture<Void> particleFuture = CompletableFuture.runAsync(() -> timeUsageNano.set(System.nanoTime()), EXECUTOR);
		CompletableFuture<Void> sequencedTaskFuture = particleFuture;
		CompletableFuture<?> parallelEventsFuture = Utils.NULL_FUTURE;
		CompletableFuture<?> parallelOperationsFuture = Utils.NULL_FUTURE;
		// end tick events
		if (levelRunning) {
			sequencedTaskFuture = sequencedTaskFuture.thenRun(() -> {
				// 每 tick 结束时都要执行的固定事件
				for (Runnable endTickEvent : ORDERED_END_TICK_EVENTS) {
					try {
						endTickEvent.run();
					} catch (Exception e) {
						if (!isTolerable(e) || EXCEPTION_TRACKER.addException(endTickEvent, e)) {
							throw e;
						}
					}
				}
			}).exceptionally(AsyncTicker::tickExceptionally);
			parallelEventsFuture = particleFuture.thenCompose(v -> {
				// 每 tick 结束时都要执行的固定事件，可在 tick 间的任意时刻执行
				@SuppressWarnings("rawtypes")
				CompletableFuture[] completableFutures = new CompletableFuture[UNORDERED_END_TICK_EVENTS.size()];
				int j = 0;
				for (Runnable endTickEvent : UNORDERED_END_TICK_EVENTS) {
					completableFutures[j++] = CompletableFuture.runAsync(endTickEvent, EXECUTOR)
						.exceptionally(e -> {
							if (!isTolerable(e) || EXCEPTION_TRACKER.addException(endTickEvent, e)) {
								throw toThrowDirectly(e);
							}
							return null;
						});
				}
				return CompletableFuture.allOf(completableFutures);
			}).exceptionally(AsyncTicker::tickExceptionally);
		}

		// end tick operations
		List<EndTickOperation> endTickOperations = END_TICK_OPERATIONS;
		if (!endTickOperations.isEmpty()) {
			EndTickOperation[] endTickTasks = endTickOperations.toArray(new EndTickOperation[0]);
			endTickOperations.clear();
			sequencedTaskFuture = sequencedTaskFuture.thenRun(() -> {
				// 每 tick 添加的不固定操作
				for (EndTickOperation endTickTask : endTickTasks) {
					if (!endTickTask.isOrdered()) {
						continue;
					}
					try {
						endTickTask.run();
					} catch (Exception e) {
						if (!isTolerable(e) || EXCEPTION_TRACKER.addException(endTickTask.getId(), e)) {
							throw e;
						}
					}
				}
			}).exceptionally(AsyncTicker::tickExceptionally);
			parallelOperationsFuture = particleFuture.thenCompose(v -> {
				// 每 tick 结束时都要执行的固定事件，可在 tick 间的任意时刻执行
				@SuppressWarnings("rawtypes")
				CompletableFuture[] futures = new CompletableFuture[endTickTasks.length];
				int j = 0;
				for (EndTickOperation endTickTask : endTickTasks) {
					if (endTickTask.isOrdered()) {
						continue;
					}
					futures[j++] = CompletableFuture.runAsync(endTickTask, EXECUTOR)
						.exceptionally(e -> {
							if (!isTolerable(e) || EXCEPTION_TRACKER.addException(endTickTask.getId(), e)) {
								throw toThrowDirectly(e);
							}
							return null;
						});
				}
				return CompletableFuture.allOf(futures);
			}).exceptionally(AsyncTicker::tickExceptionally);
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
					.map(runnable -> CompletableFuture
						.runAsync(runnable, EXECUTOR)
						.exceptionally(e -> {
							if (!ConfigHelper.markSyncIfTickFailed()
								&& isTolerable(e)) {
								LOGGER.warn("Exception while executing particle operation, you can ignore it if it doesn't happen frequently.", e);
								return null;
							}
							throw toThrowDirectly(e);
						}))
					.toArray(CompletableFuture[]::new));
				sequencedTaskFuture = sequencedTaskFuture.thenCompose(function).exceptionally(AsyncTicker::tickExceptionally);
			}
		}

		AsyncTicker.particleFuture = sequencedTaskFuture
			.thenRunAsync(() -> timeUsageNano.set(System.nanoTime() - timeUsageNano.get()), EXECUTOR);

		profiler.pop();
	}

	private static Void tickExceptionally(Throwable e) {
		if (!(e instanceof Exception)) {
			throw toThrowDirectly(e);
		}
		Minecraft mc = Minecraft.getInstance();
		if (!isTolerable(e) &&
			mc.level != null && mc.player != null) {
			throw toThrowDirectly(e);
		}
		LOGGER.warn("Exception while executing before particle operation", e);
		return null;
	}

	public static boolean isTolerable(@NotNull Throwable e) {
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

	public static void onTickingParticleException(Particle particle, Throwable t) {
		if (RenderSystem.isOnRenderThread()) {
			throw constructCrashReport(particle, t);
		}
		boolean tolerable = isTolerable(t);
		if (tolerable && !EXCEPTION_TRACKER.addException(particle.getClass(), t)) {
			return;
		}
		if (ConfigHelper.markSyncIfTickFailed()) {
			((ParticleAddon) particle).asyncparticles$setTickSync();
			if (!shouldSync(particle.getClass())) {
				if (!tolerable) {
					LOGGER.warn("Exception while ticking particle {}, marking as sync", particle, t);
				} else {
					LOGGER.warn("Exception {} thrown while ticking particle {} exceeds the threshold, please contact the author: {}",
						t.getClass().getSimpleName(),
						particle,
						AsyncParticlesClient.ISSUE_URL,
						t);
				}
				markAsSync(particle.getClass());
			}
			recordSync(particle);
		} else if (tolerable) {
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null) {
				player.sendSystemMessage(Component.literal(
						"Exception %s thrown while ticking particle %s exceeds the threshold, please contact the author: "
							.formatted(t.getClass().getSimpleName(), particle.getClass()))
					.append(Component.literal(AsyncParticlesClient.ISSUE_URL)
						.setStyle(Style.EMPTY
							.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, AsyncParticlesClient.ISSUE_URL))
							.withUnderlined(true))));
			}
			LOGGER.warn("Exception {} thrown while ticking particle {} exceeds the threshold, please contact the author: {}",
				t.getClass().getSimpleName(),
				particle,
				AsyncParticlesClient.ISSUE_URL,
				t);
		} else {
			throw constructCrashReport(particle, t);
		}
	}

	public static void onParticleEngineClear() {
		// this fix a good placement mod block invisible
		if (ModListHelper.A_GOOD_PLACE_LOADED) {
			AGoodPlaceCompat.onParticleEngineClear();
		}
		// this fix particlerain's particle count management bug
		if (ModListHelper.PARTICLERAIN_LOADED) {
			ParticleRainCompat.clearCounters();
		}
	}

	public static void waitForCleanUp() {
		if (AsyncTicker.particleCleanup != null) {
			AsyncTicker.particleCleanup.join();
			AsyncTicker.particleCleanup = null;
		}
	}

	public static ReportedException constructCrashReport(Particle particle, Throwable t) {
		debugLater(LOGGER::info);
		tryDebug();
		AsyncRenderer.debugLater(LOGGER::info);
		AsyncRenderer.tryDebug();
		CrashReport crashReport = CrashReport.forThrowable(t, "Ticking Particle");
		CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being ticked");
		crashReportCategory.setDetail("Particle", particle::toString);
		crashReportCategory.setDetail("Particle Type", particle.getRenderType()::toString);
		return new ReportedException(crashReport);
	}

	/* Sync Ticking */

	public static void tickSyncParticles() {
		if ((!shouldTickParticles && ConfigHelper.isTickAsync()) || SYNC_PARTICLES.isEmpty()) {
			return;
		}
		ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
		boolean enableLightCache = ConfigHelper.particleLightCache();
		for (Iterator<Particle> iterator = SYNC_PARTICLES.iterator(); iterator.hasNext(); ) {
			Particle particle = iterator.next();
			try {
				particleEngine.tickParticle(particle);
				if (!(particle instanceof TrackingEmitter)) {
					if (enableLightCache) {
						((LightCachedParticleAddon) particle).asyncparticles$refresh();
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

	public static void markAsSync(Class<? extends Particle> aClass) {
		synchronized (SYNC_PARTICLE_TYPES) {
			SYNC_PARTICLE_TYPES.add(aClass);
		}
	}

	public static boolean shouldSync(Class<?> aClass) {
		return SYNC_PARTICLE_TYPES.contains(aClass);
	}

	public static void recordSync(Particle particle) {
		synchronized (SYNC_PARTICLES) {
			SYNC_PARTICLES.add(particle);
		}
	}

	public static void onEvicted(Particle particle) {
		particle.getParticleGroup().ifPresent(g -> Minecraft.getInstance().particleEngine.updateCount(g, -1));
		if (particle.isAlive()) {
			particle.remove();
		}
	}

	/* Debug/Reload */

	static void tryDebug() {
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
			.formatted(ConfigHelper.isTickAsync() ? timeUsageNano.get() / 1000000d : Double.NaN,
				debug_cancelled,
				PARTICLE_OPERATIONS.size(),
				ORDERED_END_TICK_EVENTS.size() + UNORDERED_END_TICK_EVENTS.size(),
				END_TICK_OPERATIONS.size()
//				+ UNORDERED_END_TICK_OPERATIONS.size()
				,
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

	public static void debugLater(Consumer<String> consumer) {
		debugConsumer = consumer;
	}

	public static void dumpParticles() {
		LOGGER.info(Minecraft.getInstance().particleEngine.particles);
	}

	public static void reloadLater() {
		shouldReload = true;
	}

	private static void tryReload() {
		if (shouldReload) {
			reload(false);
			shouldReload = false;
		}
	}

	public static void reload(boolean clearParticles) {
		AsyncRenderer.reset();
		ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
		if (clearParticles) {
			reset();
			particleEngine.clearParticles();
		} else {
			Queue<Particle> newToAdd = BusyWaitEvictingQueue.newInstance(1024, ConfigHelper.getParticleLimit(), AsyncTicker::onEvicted);
			newToAdd.addAll(particleEngine.particlesToAdd);
			particleEngine.particlesToAdd = newToAdd;
			Queue<TrackingEmitter> newEmitters = BusyWaitEvictingQueue.newInstance(256, ConfigHelper.getParticleLimit(), AsyncTicker::onEvicted);
			newEmitters.addAll(particleEngine.trackingEmitters);
			particleEngine.trackingEmitters = newEmitters;
			particleEngine.particles.entrySet().forEach(entry -> {
				Queue<Particle> queue = entry.getValue();
				Queue<Particle> newQueue = IterationSafeEvictingQueue.newInstance(queue.size(), ConfigHelper.getParticleLimit(), AsyncTicker::onEvicted);
				newQueue.addAll(queue);
				entry.setValue(newQueue);
			});
		}
	}

	public static void reset() {
		cancelled = true;
		waitForCleanUp();
		if (particleFuture != null) {
			particleFuture.join();
			particleFuture = null;
		}
		PARTICLE_OPERATIONS.clear();
		END_TICK_OPERATIONS.clear();
		SYNC_PARTICLES.clear();
		cancelled = false;
	}

	/* Events */

	@ApiStatus.Internal
	public static void registerEvent(EndTickEvent task) {
		if (task.isOrdered()) {
			ORDERED_END_TICK_EVENTS.add(task);
			ORDERED_END_TICK_EVENTS.sort(Comparator.comparingInt(EndTickEvent::getPriority));
		} else {
			UNORDERED_END_TICK_EVENTS.add(task);
			// also sort the unordered events. To determine the order of submission of asynchronous tasks
			UNORDERED_END_TICK_EVENTS.sort(Comparator.comparingInt(EndTickEvent::getPriority));
		}
	}

	@ApiStatus.Internal
	public static void scheduleOperation(EndTickOperation task) {
		END_TICK_OPERATIONS.add(task);
	}
}

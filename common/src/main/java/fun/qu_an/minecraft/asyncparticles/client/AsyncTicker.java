package fun.qu_an.minecraft.asyncparticles.client;

import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.a_good_place.AGoodPlaceCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSCompat;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import fun.qu_an.minecraft.asyncparticles.client.util.*;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static fun.qu_an.minecraft.asyncparticles.client.util.Utils.toThrowDirectly;

// TODO: 整理这一坨
public class AsyncTicker {
	public static final Logger LOGGER = LogManager.getLogger();
	private static final Set<Class<? extends Particle>> SYNC_PARTICLE_TYPES = Collections.newSetFromMap(new IdentityHashMap<>());
	private static final Set<Particle> SYNC_PARTICLES = Collections.newSetFromMap(new IdentityHashMap<>());
	public static final List<Runnable> BLOCK_ENTITY_OPERATIONS = new ArrayList<>();
	public static final List<Runnable> PARTICLE_OPERATIONS = new ArrayList<>();
	private static boolean cancelled = false;
	public static boolean shouldTickParticles = false;
	public static CompletableFuture<Void> particleCleanup;
	private final static List<Runnable> END_TICK_EVENTS = new ArrayList<>();
	private final static List<Runnable> END_TICK_OPERATIONS = new ArrayList<>();
	private static CompletableFuture<Void> particleFuture;
	private static CompletableFuture<Void> blockEntityTickFuture;
	private static boolean debug_cancelled = false;
	private static Consumer<String> debugConsumer;
	private static boolean shouldReload;
	public static final ExecutorService EXECUTOR;
	public static final String THREAD_PREFIX = "AsyncParticleTicker";
	public static final ExceptionTracker<Class<? extends Particle>> EXCEPTION_TRACKER = new ExceptionTracker<>(
		() -> 5000,
		() -> SimplePropertiesConfig.tickFailurePerSecondThreshold
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

	// called per frame
	public static void onRunAllTasks() {
		if (!SimplePropertiesConfig.isTickAsync()) {
			return;
		}
		// join before runAllTasks
		if (blockEntityTickFuture != null && !SimplePropertiesConfig.greedyAsyncClientBlockEntityTick()) {
			blockEntityTickFuture.join();
			blockEntityTickFuture = null;
		}
	}

	/**
	 * @param i Current index of tick loop
	 * @param to Count of ticks to run
	 */
	public static void onTickBefore(int i, int to) {
		if (!SimplePropertiesConfig.isTickAsync()) {
			return;
		}
		// assert i < to;
		ProfilerFiller profiler = Profiler.get();
		profiler.push("async_particles");
		if (blockEntityTickFuture != null &&
			(i != 0 || SimplePropertiesConfig.greedyAsyncClientBlockEntityTick())) {
			blockEntityTickFuture.join();
			blockEntityTickFuture = null;
		}
		if (i != 0) {
			// tick non-zero, do nothing
			shouldTickParticles = i == to - 1; // tick particles only on last tick
		} else {
			// tick zero, wait for async tasks to complete, cleanup
			cancelled = true;
			debug_cancelled = false;
			if (particleFuture != null) {
				particleFuture.join();
				particleFuture = null;
			}
			cancelled = false;
			shouldTickParticles = i == to - 1;
			Minecraft mc = Minecraft.getInstance();
			boolean levelRunning = mc.level != null && mc.player != null && !mc.isPaused();
			if (levelRunning) {
				ParticleEngine particleEngine = mc.particleEngine;
				Collection<Queue<Particle>> values = particleEngine.particles.values();
				CompletableFuture<?>[] futures = new CompletableFuture[values.size() + 1];
				int k = 0;
				Queue<TrackingEmitter> trackingEmitters = particleEngine.trackingEmitters;
				if (trackingEmitters.isEmpty()) {
					futures[k++] = CompletableFuture.completedFuture(null);
				} else {
					futures[k++] = CompletableFuture.runAsync(() ->
						trackingEmitters.removeIf(trackingEmitter -> !trackingEmitter.isAlive()), EXECUTOR);
				}
				for (Queue<Particle> particles : values) {
					if (particles.isEmpty()) {
						futures[k++] = CompletableFuture.completedFuture(null);
						continue;
					}
					futures[k++] = CompletableFuture.runAsync(() -> {
						particles.removeIf(particle1 -> {
							// JDK 并没有定义这个判断会对每个对象执行多少次，但目前没遇到例外情况
							// use ArrayDeque's removeIf to improve performance
							boolean b = ((ParticleAddon) particle1).asyncedParticles$isTickSync()
								? !particle1.isAlive()
								: ((ParticleAddon) particle1).asyncParticles$shouldRemove();
							if (b) {
								// make sure the tracked count is correct
								particle1.getParticleGroup().ifPresent(
									group -> particleEngine.updateCount(group, -1));
								return true;
							}
							return false;
						});
					}, EXECUTOR);
				}
				particleCleanup = CompletableFuture.allOf(futures);
			}
		}
		profiler.pop();
	}

	/**
	 * @param i Current index of tick loop
	 * @param to Count of ticks to run
	 */
	public static void onTickAfter(int i, int to) {
		if (!SimplePropertiesConfig.isTickAsync()) {
			tryReload();
			tryDebug();
			END_TICK_OPERATIONS.forEach(Runnable::run);
			END_TICK_OPERATIONS.clear();
			END_TICK_EVENTS.forEach(Runnable::run);
			return;
		}
		// assert i < to;
		Minecraft mc = Minecraft.getInstance();
		ProfilerFiller profiler = Profiler.get();
		profiler.push("async_particles");
		boolean levelRunning = mc.level != null && mc.player != null && !mc.isPaused();
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
		CompletableFuture<Void> particleFuture;
		List<Runnable> blockEntityOperations = BLOCK_ENTITY_OPERATIONS;
		if (!levelRunning || !SimplePropertiesConfig.asyncBlockEntityTick()) {
			particleFuture = CompletableFuture.runAsync(() -> {
				timeUsageNano.set(System.nanoTime());
			}, EXECUTOR);
			if (!blockEntityOperations.isEmpty()) {
				blockEntityOperations.clear();
			}
		} else {
			Runnable[] blockEntityTasks = blockEntityOperations.toArray(new Runnable[0]);
			blockEntityOperations.clear();
			particleFuture = CompletableFuture.runAsync(() -> {
				timeUsageNano.set(System.nanoTime());
				for (Runnable blockEntityTask : blockEntityTasks) {
					blockEntityTask.run();
				}
			}, EXECUTOR).exceptionally(AsyncTicker::tickBeforeExceptionally);
			AsyncTicker.blockEntityTickFuture = particleFuture;
		}

		// end tick events
		if (levelRunning) {
			particleFuture = particleFuture.thenRun(() -> {
				// 每 tick 结束时都要执行的固定事件
				for (Runnable endTickEvent : END_TICK_EVENTS) {
					endTickEvent.run();
				}
			}).exceptionally(AsyncTicker::tickBeforeExceptionally);
		}

		// end tick operations
		List<Runnable> endTickOperations = END_TICK_OPERATIONS;
		if (!endTickOperations.isEmpty()) {
			Runnable[] endTickTasks = endTickOperations.toArray(new Runnable[0]);
			endTickOperations.clear();
			particleFuture = particleFuture.thenRun(() -> {
				// 每 tick 添加的不固定操作
				for (Runnable endTickTask : endTickTasks) {
					endTickTask.run();
				}
			}).exceptionally(AsyncTicker::tickBeforeExceptionally);
		}

		// particle ticking
		List<Runnable> particleOperations = PARTICLE_OPERATIONS;
		if (!particleOperations.isEmpty()) {
			Runnable[] particleTasks = particleOperations.toArray(new Runnable[0]);
			particleOperations.clear();
			particleFuture = particleFuture.thenCompose(v -> CompletableFuture.allOf(Arrays.stream(particleTasks)
					.map(runnable -> CompletableFuture
						.runAsync(runnable, EXECUTOR)
						.exceptionally(e -> {
							if (!SimplePropertiesConfig.markSyncIfTickFailed()
								&& isTolerable(e)) {
								LOGGER.warn("Exception while executing particle operation, you can ignore it if it doesn't happen frequently.", e);
								return null;
							}
							throw toThrowDirectly(e);
						}))
					.toArray(CompletableFuture[]::new)))
				.thenRun(() -> timeUsageNano.set(System.nanoTime() - timeUsageNano.get()));
		}

		AsyncTicker.particleFuture = particleFuture;
		profiler.pop();
	}

	private static Void tickBeforeExceptionally(Throwable e) {
		if (!(e instanceof Exception)) {
			throw toThrowDirectly(e);
		}
		Minecraft mc = Minecraft.getInstance();
		if (!isTolerable(e) &&
			mc.level != null && mc.player != null) {
			// FIXME: 更好的异常处理方案
			throw toThrowDirectly(e);
		}
		LOGGER.warn("Exception while executing before particle operation", e);
		return null;
	}

	public static boolean isTolerable(@NotNull Throwable e) {
		if (!(e instanceof Exception)) {
			return false;
		}
		return Utils.getRootCause(e) instanceof MissingPaletteEntryException
			   || e instanceof NullPointerException
			   || e instanceof IndexOutOfBoundsException
			   || (SimplePropertiesConfig.suppressCME() && e instanceof ConcurrentModificationException);
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
		ReportedException re = Utils.getReportedException(t);
		if (re != null) {
			return re;
		}
		CrashReport crashReport = CrashReport.forThrowable(t, "Ticking Particle");
		CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being ticked");
		crashReportCategory.setDetail("Particle", particle::toString);
		crashReportCategory.setDetail("Particle Type", particle.getRenderType()::toString);
		return new ReportedException(crashReport);
	}

	/* Sync Ticking */

	public static void tickSyncParticles() {
		if (!shouldTickParticles || SYNC_PARTICLES.isEmpty()) {
			return;
		}
		ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
		for (Iterator<Particle> iterator = SYNC_PARTICLES.iterator(); iterator.hasNext(); ) {
			Particle particle = iterator.next();
			try {
				particleEngine.tickParticle(particle);
				if (!(particle instanceof TrackingEmitter)) {
					if (particle instanceof LightCachedParticleAddon lightCachedParticle
						&& SimplePropertiesConfig.particleLightCache()) {
						lightCachedParticle.asyncParticles$refresh();
					}
					((ParticleAddon) particle).asyncParticles$setTicked();
				}
				if (ModListHelper.VS_LOADED) {
					VSCompat.removeIfOutSight(particle);
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

	public static boolean shouldSync(Class<? extends Particle> aClass) {
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

	private static void tryDebug() {
		if (debugConsumer == null) {
			return;
		}
		debugConsumer.accept(String.format("""
			[Debug AsyncTicker]
			last tick duration: %.1f ms,
			interrupted: %s,
			block entity operations: %d,
			particle operations: %d,
			end tick events: %d,
			end tick operations: %d,
			max particles queue size: %d,
			particles queue size/allocated: %s,
			particles to add size: %d
			sync particle count: %d,
			sync particle types: %s,"""
			.formatted(timeUsageNano.get() / 1000000d,
				debug_cancelled,
				BLOCK_ENTITY_OPERATIONS.size(),
				PARTICLE_OPERATIONS.size(),
				END_TICK_EVENTS.size(),
				END_TICK_OPERATIONS.size(),
				SimplePropertiesConfig.limit,
				Minecraft.getInstance().particleEngine.particles.entrySet()
					.stream().collect(Collectors.toMap(e -> e.getKey().name(),
						e -> {
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
			Queue<Particle> toAdd = particleEngine.particlesToAdd;
			BusyWaitEvictingQueue<Particle> newToAdd = new BusyWaitEvictingQueue<>(1024, SimplePropertiesConfig.limit, AsyncTicker::onEvicted);
			newToAdd.addAll(toAdd);
			particleEngine.particlesToAdd = newToAdd;
			particleEngine.particles.entrySet().forEach(entry -> {
				Queue<Particle> queue = entry.getValue();
				Queue<Particle> newQueue = new BusyWaitEvictingQueue<>(1024, SimplePropertiesConfig.limit, AsyncTicker::onEvicted);
				newQueue.addAll(queue);
				entry.setValue(newQueue);
			});
		}
	}

	public static void reset() {
		cancelled = true;
		waitForCleanUp();
		if (blockEntityTickFuture != null) {
			blockEntityTickFuture.join();
			blockEntityTickFuture = null;
		}
		if (particleFuture != null) {
			particleFuture.join();
			particleFuture = null;
		}
		BLOCK_ENTITY_OPERATIONS.clear();
		PARTICLE_OPERATIONS.clear();
		END_TICK_OPERATIONS.clear();
		SYNC_PARTICLES.clear();
		cancelled = false;
	}

	/* Events */

	public static void registerEndTickEvent(MinecraftConsumer consumer) {
		registerEndTickEvent(() -> consumer.accept(Minecraft.getInstance()));
	}

	public static void registerEndTickEvent(ClientLevelConsumer consumer) {
		registerEndTickEvent(() -> consumer.accept(Minecraft.getInstance().level));
	}

	public static void registerEndTickEvent(Runnable operation) {
		AsyncTicker.END_TICK_EVENTS.add(operation);
	}

	public static void addEndTickTask(MinecraftConsumer consumer) {
		addEndTickTask(() -> consumer.accept(Minecraft.getInstance()));
	}

	public static void addEndTickTask(ClientLevelConsumer consumer) {
		addEndTickTask(() -> consumer.accept(Minecraft.getInstance().level));
	}

	public static void addEndTickTask(Runnable operation) {
		if (shouldTickParticles || !SimplePropertiesConfig.isTickAsync()) {
			AsyncTicker.END_TICK_OPERATIONS.add(operation);
		}
	}

	@FunctionalInterface
	public interface MinecraftConsumer {
		void accept(Minecraft mc);
	}

	@FunctionalInterface
	public interface ClientLevelConsumer {
		void accept(ClientLevel level);
	}
}

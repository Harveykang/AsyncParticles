package fun.qu_an.minecraft.asyncparticles.client;

import com.google.common.collect.EvictingQueue;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.a_good_place.AGoodPlaceCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSCompat;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import fun.qu_an.minecraft.asyncparticles.client.mixin.a_good_place.InvokerBlocksParticlesManager;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import nl.enjarai.a_good_place.particles.BlocksParticlesManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static fun.qu_an.minecraft.asyncparticles.client.util.Utils.toThrowDirectly;

// TODO: 整理这一坨
public class AsyncTicker {
	public static final Logger LOGGER = LogManager.getLogger();
	private static final VarHandle EvictingQueue$delegate;

	static {
		try {
			EvictingQueue$delegate = MethodHandles.privateLookupIn(EvictingQueue.class, MethodHandles.lookup())
				.findVarHandle(EvictingQueue.class, "delegate", Queue.class);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private static final Set<Class<? extends Particle>> SYNC_PARTICLE_TYPES = Collections.newSetFromMap(new IdentityHashMap<>());

	static {
		if (ModListHelper.PHYSICSMOD_LOADED) {
			addSyncByClassName("net.diebuddies.minecraft.weather.RainParticle");
			addSyncByClassName("net.diebuddies.minecraft.weather.DustParticle");
			addSyncByClassName("net.diebuddies.minecraft.weather.SnowParticle");
			addSyncByClassName("net.diebuddies.physics.ocean.RainParticle");
		}
	}

	private static final Set<Particle> SYNC_PARTICLES = Collections.newSetFromMap(new IdentityHashMap<>());

	public static final List<Runnable> BLOCK_ENTITY_OPERATIONS = new ArrayList<>();
	public static final List<Runnable> PARTICLE_OPERATIONS = new ArrayList<>();
	private static boolean cancelled = false;
	private static boolean tickingSync;
	public static boolean shouldTickParticles = true;
	public static CompletableFuture<Void> particleCleanup;
	private final static List<Runnable> END_TICK_EVENTS = new ArrayList<>();
	public final static List<Runnable> END_TICK_OPERATIONS = new ArrayList<>();
	private static CompletableFuture<Void> particleFuture;
	private static CompletableFuture<Void> blockEntityTickFuture;
	private static boolean debug_cancelled = false;
	private static Consumer<String> debugConsumer;
	private static boolean shouldReload;
	public static final ExecutorService EXECUTOR;
	public static final String THREAD_PREFIX = "AsyncParticleTicker";

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
			LOGGER.error("", e);
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
		// join before runAllTasks
		if (blockEntityTickFuture != null && !SimplePropertiesConfig.greedyAsyncClientBlockEntityTick()) {
			blockEntityTickFuture.join();
			blockEntityTickFuture = null;
		}
	}

	public static void onTickBefore(int j) {
		ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
		profiler.push("async_particles");
		if (j != 0) {
			if (blockEntityTickFuture != null) {
				blockEntityTickFuture.join();
				blockEntityTickFuture = null;
			}
			shouldTickParticles = false;
		} else {
			if (blockEntityTickFuture != null && SimplePropertiesConfig.greedyAsyncClientBlockEntityTick()) {
				blockEntityTickFuture.join();
				blockEntityTickFuture = null;
			}
			cancelled = true;
			debug_cancelled = false;
			if (particleFuture != null) {
				particleFuture.join();
				particleFuture = null;
			}
			cancelled = false;
			shouldTickParticles = true;

			Minecraft mc = Minecraft.getInstance();
			ParticleEngine particleEngine = mc.particleEngine;

			if (!mc.isPaused()) {
				Collection<Queue<Particle>> values = particleEngine.particles.values();
				var futures = new CompletableFuture[values.size()];
				int i = 0;
				for (Queue<Particle> particles1 : values) {
					if (particles1.isEmpty()) {
						futures[i++] = CompletableFuture.completedFuture(null);
						continue;
					}
					futures[i++] = CompletableFuture.runAsync(() -> {
						Queue<Particle> particles = (Queue<Particle>) EvictingQueue$delegate.get(particles1);
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

	public static void onTickAfter(int j) {
		Minecraft mc = Minecraft.getInstance();
		ProfilerFiller profiler = mc.getProfiler();
		profiler.push("async_particles");
		if (!mc.isPaused()){
			profiler.push("particle_tick");
			try {
				mc.particleEngine.tick();
			} catch (Exception e) {
				if (mc.level != null && mc.player != null) {
					throw e;
				}
			}
			profiler.pop();
		}
		if (j != 0) {
			return;
		}
		tryReload();
		tryDebug();
		CompletableFuture<Void> blockEntityTickFuture;
		if (!SimplePropertiesConfig.asyncBlockEntityTick()) {
			blockEntityTickFuture = CompletableFuture.runAsync(() -> {
			}, EXECUTOR);
		} else {
			List<Runnable> blockEntityOperations = BLOCK_ENTITY_OPERATIONS;
			Runnable[] blockEntityTasks = blockEntityOperations.toArray(new Runnable[0]);
			blockEntityOperations.clear();
			blockEntityTickFuture = CompletableFuture.runAsync(() -> {
				for (Runnable blockEntityTask : blockEntityTasks) {
					blockEntityTask.run();
				}
			}, EXECUTOR).exceptionally(AsyncTicker::tickBeforeExceptionally);
			AsyncTicker.blockEntityTickFuture = blockEntityTickFuture;
		}

		List<Runnable> endTickOperations = END_TICK_OPERATIONS;
		Runnable[] endTickTasks = endTickOperations.toArray(new Runnable[0]);
		endTickOperations.clear();
		List<Runnable> particleOperations = PARTICLE_OPERATIONS;
		Runnable[] particleTasks = particleOperations.toArray(new Runnable[0]);
		particleOperations.clear();
		particleFuture = blockEntityTickFuture
			.thenRun(() -> {
				// 每 tick 添加的不固定操作
				for (Runnable endTickTask : endTickTasks) {
					endTickTask.run();
				}
				// 每 tick 结束时都要执行的固定事件
				for (Runnable endTickEvent : END_TICK_EVENTS) {
					// FIXME 这个应该可以取消，防止卡死主线程
					endTickEvent.run();
				}
			}).exceptionally(AsyncTicker::tickBeforeExceptionally)
			.thenCompose(v -> CompletableFuture.allOf(Arrays.stream(particleTasks)
				.map(runnable -> CompletableFuture.runAsync(runnable, EXECUTOR)
					.exceptionally(e -> {
						if (!SimplePropertiesConfig.markSyncIfTickFailed()
							&& isTolerable(e)) {
							LOGGER.warn("Exception while executing particle operation, you can ignore it if it doesn't happen frequently.", e);
							return null;
						}
						throw toThrowDirectly(e);
					}))
				.toArray(CompletableFuture[]::new)));
		profiler.pop();
	}

	private static Void tickBeforeExceptionally(Throwable e) {
		if (!(e instanceof Exception)) {
			throw toThrowDirectly(e);
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.player != null) {
			// FIXME: 更好的异常处理方案
			throw toThrowDirectly(e);
		}
		LOGGER.warn("Exception while executing before particle operation", e);
		return null;
	}

	public static boolean isTolerable(@NotNull Throwable e) {
		if (SimplePropertiesConfig.ignoreParticleTickExceptions()) {
			return true;
		}
		if (e instanceof ReportedException
			|| e instanceof CompletionException
			|| e.getClass() == RuntimeException.class) {
			Throwable t = e.getCause();
			if (t == null) {
				return false;
			}
			return isTolerable(t);
		}
		return e instanceof MissingPaletteEntryException
			   || e instanceof NullPointerException
			   || e instanceof ArrayIndexOutOfBoundsException;
	}

	/* Sync Ticking */

	public static boolean isTickingSync() {
		return tickingSync;
	}

	public static void tickSync() {
		if (SYNC_PARTICLES.isEmpty()) {
			return;
		}
		tickingSync = true;
		ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
		for (Iterator<Particle> iterator = SYNC_PARTICLES.iterator(); iterator.hasNext(); ) {
			Particle particle = iterator.next();
			particleEngine.tickParticle(particle);
			// refresh light cache asynchronously
//					if (particle instanceof LightCachedParticleAddon lightCachedParticle
//						&& SimplePropertiesConfig.particleLightCache()) {
//						lightCachedParticle.asyncParticles$refresh();
//					}
			if (!(particle instanceof TrackingEmitter)) {
				((ParticleAddon) particle).asyncParticles$setTicked();
			}
			if (ModListHelper.VS_LOADED) {
				VSCompat.removeIfOutSight(particle);
			}
			if (!particle.isAlive()) {
				// we manage the count in cleanup task
//				particle.getParticleGroup().ifPresent((particleGroup) -> particleEngine.updateCount(particleGroup, -1));
				iterator.remove();
			}
		}
		tickingSync = false;
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
		if (SYNC_PARTICLES.contains(particle)) {
			return;
		}
		synchronized (SYNC_PARTICLES) {
			SYNC_PARTICLES.add(particle);
		}
	}

	/* Debug/Reload */

	private static void tryDebug() {
		if (debugConsumer == null) {
			return;
		}
		debugConsumer.accept(String.format("""
			[Debug AsyncTicker]
			interrupted: %s,
			block entity operations: %d,
			particle operations: %d,
			end tick events: %d,
			end tick operations: %d,
			particles queue size: %s,
			particles to add size: %d
			sync particle count: %d,
			sync particle types: %s,"""
			.formatted(debug_cancelled,
				BLOCK_ENTITY_OPERATIONS.size(),
				PARTICLE_OPERATIONS.size(),
				END_TICK_EVENTS.size(),
				END_TICK_OPERATIONS.size(),
				Minecraft.getInstance().particleEngine.particles.entrySet()
					.stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size())),
				Minecraft.getInstance().particleEngine.particlesToAdd.size(),
				SYNC_PARTICLES.size(),
				SYNC_PARTICLE_TYPES.stream().map(Class::getName).toList())));
		debugConsumer = null;
	}

	public static void debugLater(Consumer<String> consumer) {
		debugConsumer = consumer;
	}

	public static void reloadLater() {
		shouldReload = true;
	}

	private static void tryReload() {
		if (shouldReload) {
			destroy();
			AsyncRenderer.destroy();
			Minecraft.getInstance().particleEngine.clearParticles();
			shouldReload = false;
		}
	}

	/* Events */

	public static void registerEndTickEvent(MinecraftConsumer consumer) {
		registerEndTickEvent(() -> {
			Minecraft mc = Minecraft.getInstance();
			if (AsyncTicker.shouldTickParticles && mc.level != null && mc.player != null) {
				consumer.accept(mc);
			}
		});
	}

	public static void registerEndTickEvent(ClientLevelConsumer consumer) {
		registerEndTickEvent(() -> {
			Minecraft mc = Minecraft.getInstance();
			if (AsyncTicker.shouldTickParticles && mc.level != null && mc.player != null) {
				consumer.accept(mc.level);
			}
		});
	}

	public static void registerEndTickEvent(Runnable operation) {
		AsyncTicker.END_TICK_EVENTS.add(operation);
	}

	public static void destroy() {
		cancelled = true;
		if (particleCleanup != null) {
			particleCleanup.join();
			particleCleanup = null;
		}
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

	@FunctionalInterface
	public interface MinecraftConsumer {
		void accept(Minecraft mc);
	}

	@FunctionalInterface
	public interface ClientLevelConsumer {
		void accept(ClientLevel level);
	}
}

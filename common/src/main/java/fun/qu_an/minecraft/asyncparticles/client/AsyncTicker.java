package fun.qu_an.minecraft.asyncparticles.client;

import com.google.common.collect.EvictingQueue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
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
//		SYNC_PARTICLE_TYPES.add(ItemPickupParticle.class);
		if (ModListHelper.PHYSICSMOD_LOADED) {
			try {
				addSyncByClassName("net.diebuddies.minecraft.weather.RainParticle");
				addSyncByClassName("net.diebuddies.minecraft.weather.DustParticle");
				addSyncByClassName("net.diebuddies.minecraft.weather.SnowParticle");
				addSyncByClassName("net.diebuddies.physics.ocean.RainParticle");
			} catch (Exception e) {
				LOGGER.error("", e);
			}
		}
	}

	private static void addSyncByClassName(String className) throws Exception {
		SYNC_PARTICLE_TYPES.add((Class<? extends Particle>) Class.forName(className));
	}

	private static final List<Particle> SYNC_PARTICLES = new ArrayList<>();

	public static final List<Runnable> BLOCK_ENTITY_OPERATIONS = new ArrayList<>();
	public static final List<Runnable> PARTICLE_OPERATIONS = new ArrayList<>();
	private static boolean cancelled = false;
	public static boolean shouldTickParticles = true;
	public static CompletableFuture<Void> particleCleanup;
	public static Operation<Void> tickParticleEngine;
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
		if (blockEntityTickFuture != null && !greedyAsyncClientBlockEntityTick()) {
			blockEntityTickFuture.join();
			blockEntityTickFuture = null;
		}
	}

	public static void onTickBefore(int j) {
		if (j != 0) {
			if (blockEntityTickFuture != null) {
				blockEntityTickFuture.join();
				blockEntityTickFuture = null;
			}
			shouldTickParticles = false;
		} else {
			if (blockEntityTickFuture != null && greedyAsyncClientBlockEntityTick()) {
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

			List<? extends Particle> syncList = AsyncTicker.getSync();
			if (!syncList.isEmpty()) {
				for (Particle particle : syncList) {
					particleEngine.tickParticle(particle);
					if (!(particle instanceof TrackingEmitter)) {
						((ParticleAddon) particle).asyncParticles$setTicked();
					}
					if (ModListHelper.VS_LOADED) {
						if (VSClientUtils.isOutOfSight(particle)) {
							particle.remove();
						}
					}
				}
			}

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
							boolean b = ((ParticleAddon) particle1).asyncParticles$shouldRemove();
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
	}

	public static void onTickAfter(int j) {
		Minecraft mc = Minecraft.getInstance();
		if (tickParticleEngine != null) {
			ProfilerFiller profiler = mc.getProfiler();
			profiler.push("particles");
			try {
				tickParticleEngine.call();
			} catch (Exception e) {
				if (mc.level != null && mc.player != null) {
					throw e;
				}
			}
			tickParticleEngine = null;
			profiler.pop();
		}
		if (j != 0) {
			return;
		}
		tryReload();
		tryDebug();
		clearSync();
		CompletableFuture<Void> blockEntityTickFuture;
		if (!asyncBlockEntityTick()) {
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
					endTickEvent.run();
				}
			}).exceptionally(AsyncTicker::tickBeforeExceptionally)
			.thenCompose(v -> CompletableFuture.allOf(Arrays.stream(particleTasks)
				.map(runnable -> CompletableFuture.runAsync(runnable, EXECUTOR)
					.exceptionally(e -> {
						throwIfNotTolerable(e);
						return null;
					}))
				.toArray(CompletableFuture[]::new)));
	}

	private static void throwIfNotTolerable(@NotNull Throwable e) {
		if (markSyncIfTickFailed()) {
			throw new RuntimeException(e);
		}
		if (ignoreParticleTickExceptions()) {
			return;
		}
		if (e instanceof ReportedException
			|| e instanceof CompletionException
			|| e.getClass() == RuntimeException.class) {
			Throwable t = e.getCause();
			if (t == null) {
				throw new RuntimeException(e);
			}
			throwIfNotTolerable(t);
			return;
		}
		if (e instanceof MissingPaletteEntryException
			|| e instanceof NullPointerException
			|| e instanceof ArrayIndexOutOfBoundsException) {
			LOGGER.warn("Exception while executing particle operation, you can ignore it if it doesn't happen frequently.", e);
			return;
		}
		throw new RuntimeException(e);
	}

	private static Void tickBeforeExceptionally(Throwable e) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.player != null) {
			// FIXME: 更好的异常处理方案
			throw new RuntimeException(e);
		}
		LOGGER.warn("Exception while executing before particle operation", e);
		return null;
	}

	/* Config */

	public static boolean asyncBlockEntityTick() {
		return SimplePropertiesConfig.asyncClientBlockEntityTick;
	}

	public static boolean greedyAsyncClientBlockEntityTick() {
		return SimplePropertiesConfig.greedyAsyncClientBlockEntityTick;
	}

	public static boolean asyncBlockEntityAnimate() {
		return !ModListHelper.PHYSICSMOD_LOADED && SimplePropertiesConfig.asyncClientBlockEntityAnimate;
	}

	public static boolean forceDoneBlockAnimateTick() {
		return SimplePropertiesConfig.forceDoneBlockAnimateTick;
	}

	public static boolean forceDoneParticleTick() {
		return SimplePropertiesConfig.forceDoneParticleTick;
	}

	public static boolean forceDoneTextureTick() {
		return SimplePropertiesConfig.forceDoneTextureTick;
	}

	public static boolean markSyncIfTickFailed() {
		return SimplePropertiesConfig.markSyncIfTickFailed;
	}

	public static boolean ignoreParticleTickExceptions() {
		return SimplePropertiesConfig.ignoreParticleTickExceptions;
	}

	/* Sync Ticking */

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

	public static List<? extends Particle> getSync() {
		return SYNC_PARTICLES;
	}

	public static void clearSync() {
		if (!SYNC_PARTICLES.isEmpty()) {
			SYNC_PARTICLES.clear();
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
		clearSync();
		cancelled = false;
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

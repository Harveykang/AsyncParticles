package fun.qu_an.minecraft.asyncparticles.client;

import com.google.common.collect.EvictingQueue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.lointain.cosmos.procedures.SkyboxshapeProcedure;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

// TODO: 整理这一坨
public class AsyncTicker {
	private static final VarHandle EvictingQueue$delegate;

	static {
		try {
			EvictingQueue$delegate = MethodHandles.privateLookupIn(EvictingQueue.class, MethodHandles.lookup())
				.findVarHandle(EvictingQueue.class, "delegate", Queue.class);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public static final Logger LOGGER = LogManager.getLogger();
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
	//	private static final AtomicInteger WORKER_COUNT = new AtomicInteger(1);
//	public static final ExecutorService SCHEDULING_POOL = Util.makeExecutor("ParticleTick");
	public static final ExecutorService EXECUTOR;
	private static boolean debug_cancelled = false;
	private static Consumer<String> debugConsumer;
	private static boolean shouldReload;

	static {
		AtomicInteger workerCount = new AtomicInteger(1);
		int clamp = Mth.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, 5);
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
			forkJoinWorkerThread.setName("AsyncTicker-" + workerCount.getAndIncrement());
			forkJoinWorkerThread.setDaemon(true);
			return forkJoinWorkerThread;
		}, Util::onThreadException, true);
	}

	public static void onRunAllTasks() {
		// Make sure not concurrently tick block entities
		if (blockEntityTickFuture != null) {
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
			cancelled = true;
			debug_cancelled = false;
			if (particleFuture != null) {
				particleFuture.join();
				particleFuture = null;
			}
			Minecraft mc = Minecraft.getInstance();
			if (!mc.isPaused()) {
				Collection<Queue<Particle>> values = mc.particleEngine.particles.values();
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
							// use ArrayDeque's removeIf to improve performance
							boolean b = ((ParticleAddon) particle1).asyncParticles$shouldRemove();
							if (b) {
								// make sure the tracked count is correct
								particle1.getParticleGroup().ifPresent(
									group -> mc.particleEngine.updateCount(group, -1));
								return true;
							}
							return false;
						});
					}, EXECUTOR);
				}
				particleCleanup = CompletableFuture.allOf(futures);
			}
			cancelled = false;
			shouldTickParticles = true;
		}
	}

	public static boolean shouldAsyncBlockEntityTick() {
		return SimplePropertiesConfig.asyncClientBlockEntityTick;
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
		CompletableFuture<Void> blockEntityTickFuture;
		if (!shouldAsyncBlockEntityTick()) {
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
						LOGGER.error("Error executing particle operation", e);
						return null;
					}))
				.toArray(CompletableFuture[]::new)));
	}

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
			particles to add size: %d"""
			.formatted(debug_cancelled,
				BLOCK_ENTITY_OPERATIONS.size(),
				PARTICLE_OPERATIONS.size(),
				END_TICK_EVENTS.size(),
				END_TICK_OPERATIONS.size(),
				Minecraft.getInstance().particleEngine.particles.values()
					.stream().map(particles -> String.valueOf(particles.size())).toList(),
				Minecraft.getInstance().particleEngine.particlesToAdd.size())));
		debugConsumer = null;
	}

	private static Void tickBeforeExceptionally(Throwable e) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.player != null) {
			// FIXME: 更好的异常处理方案
			throw new RuntimeException(e);
		}
		LOGGER.error("Error executing before particle operation", e);
		return null;
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

	public static void debugLater(Consumer<String> consumer) {
		debugConsumer = consumer;
	}

	public static boolean isCancelled() {
		if (!cancelled) {
			return false;
		}
		debug_cancelled = true;
		return true;
	}

	public static void reloadLater() {
		shouldReload = true;
	}

	private static void tryReload() {
		if (shouldReload) {
			Minecraft.getInstance().particleEngine.clearParticles();
			shouldReload = false;
		}
	}

	public static void registerEndTickEvent(ClientTickEvents.EndTick endTick) {
		AsyncTicker.END_TICK_EVENTS.add(() -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level != null && mc.player != null) {
				endTick.onEndTick(mc);
			}
		});
	}

	public static void registerEndTickEvent(Runnable operation) {
		AsyncTicker.END_TICK_EVENTS.add(operation);
	}

	public static void destroy() {
		cancelled = true;
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
		cancelled = false;
	}
}

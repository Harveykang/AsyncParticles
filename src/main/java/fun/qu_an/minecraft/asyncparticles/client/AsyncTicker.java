package fun.qu_an.minecraft.asyncparticles.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

// TODO: 整理这一坨
public class AsyncTicker {
	public static final Logger LOGGER = LogManager.getLogger();
	public static final List<Runnable> blockEntityOperations = new ArrayList<>();
	public static final List<Runnable> particleOperations = new ArrayList<>();
	private static boolean cancelled = false;
	public static boolean shouldTickParticles = true;
	public static CompletableFuture<Void> particleCleanup;
	public static Operation<Void> tickParticleEngine;
	public final static ArrayList<Runnable> endTickEvents = new ArrayList<>();
	public final static ArrayList<Runnable> endTickOperations = new ArrayList<>();
	private static CompletableFuture<Void> particleFuture = CompletableFuture.completedFuture(null);
	private static CompletableFuture<Void> blockEntityTickFuture;
	//	private static final AtomicInteger WORKER_COUNT = new AtomicInteger(1);
//	public static final ExecutorService SCHEDULING_POOL = Util.makeExecutor("ParticleTick");
	public static final ExecutorService EXECUTOR;
	private static boolean debug_cancelled = false;
	private static Consumer<String> debugConsumer;

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

	public static void onTickBefore(int j) {
		if (blockEntityTickFuture != null) {
			blockEntityTickFuture.join();
			blockEntityTickFuture = null;
		}
		if (j != 0) {
			shouldTickParticles = false;
		} else {
			cancelled = true;
			debug_cancelled = false;
			particleFuture.join();
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
					futures[i++] = CompletableFuture.runAsync(() -> particles1.removeIf(particle1 -> {
						boolean b = ((ParticleAddon) particle1).asyncParticles$shouldRemove();
						if (b) {
							// make sure the tracked count is correct
							particle1.getParticleGroup().ifPresent(
								group -> mc.particleEngine.updateCount(group, -1));
							return true;
						}
						return false;
					}), EXECUTOR);
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
		if (j == 0 && debugConsumer != null) {
			debugConsumer.accept(String.format("""
				[Debug AsyncTicker]
				interrupted: %s,
				block entity operations: %d,
				particle operations: %d,
				end tick events: %d,
				end tick operations: %d,
				particles queue size: [%s],
				particles to add size: %d"""
				.formatted(debug_cancelled,
					blockEntityOperations.size(),
					particleOperations.size(),
					endTickEvents.size(),
					endTickOperations.size(),
					String.join(", ", Minecraft.getInstance().particleEngine.particles.values()
						.stream().map(particles -> String.valueOf(particles.size())).toList()),
					Minecraft.getInstance().particleEngine.particlesToAdd.size())));
			debugConsumer = null;
		}
		if (tickParticleEngine != null) {
			ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
			profiler.push("particles");
			tickParticleEngine.call();
			tickParticleEngine = null;
			profiler.pop();
		}
		if (j == 0) {
			CompletableFuture<Void> blockEntityTickFuture;
			if (!shouldAsyncBlockEntityTick()) {
				blockEntityTickFuture = CompletableFuture.runAsync(() -> {
				}, EXECUTOR);
			} else {
				List<Runnable> blockEntityOperations = AsyncTicker.blockEntityOperations;
				Runnable[] blockEntityTasks = blockEntityOperations.toArray(new Runnable[0]);
				blockEntityOperations.clear();
				blockEntityTickFuture = CompletableFuture.runAsync(() -> {
					for (Runnable blockEntityTask : blockEntityTasks) {
						blockEntityTask.run();
					}
				}, EXECUTOR).exceptionally(AsyncTicker::tickBeforeExceptionally);
				AsyncTicker.blockEntityTickFuture = blockEntityTickFuture;
			}

			List<Runnable> endTickOperations = AsyncTicker.endTickOperations;
			Runnable[] endTickTasks = endTickOperations.toArray(new Runnable[0]);
			endTickOperations.clear();
			List<Runnable> particleOperations = AsyncTicker.particleOperations;
			Runnable[] particleTasks = particleOperations.toArray(new Runnable[0]);
			particleOperations.clear();
			particleFuture = blockEntityTickFuture
				.thenRun(() -> {
					// 每 tick 添加的不固定操作
					for (Runnable endTickTask : endTickTasks) {
						endTickTask.run();
					}
					// 每 tick 结束时都要执行的固定事件
					for (Runnable endTickEvent : endTickEvents) {
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

	public static void reload() {
		Minecraft.getInstance().particleEngine.clearParticles();
	}
}

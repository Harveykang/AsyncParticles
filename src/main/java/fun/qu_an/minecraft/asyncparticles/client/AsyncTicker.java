package fun.qu_an.minecraft.asyncparticles.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;

// TODO: 整理这一坨
public class AsyncTicker {
	public static final Logger LOGGER = LogManager.getLogger();
	public static final List<Runnable> particleOperations = new ArrayList<>();
	public static final List<Runnable> blockEntityOperations = new ArrayList<>();
	public static boolean cancelled = false;
	public static boolean shouldTickParticles = true;
	public static CompletableFuture<Void> particleCleanup;
	public static Operation<Void> tickParticleEngine;
	public final static ArrayList<Runnable> endTickEvents = new ArrayList<>();
	public final static ArrayList<Runnable> endTickOperations = new ArrayList<>();
	private static CompletableFuture<Void> particleFuture = CompletableFuture.completedFuture(null);
	private static CompletableFuture<Void> beforeParticleFuture;
	//	private static final AtomicInteger WORKER_COUNT = new AtomicInteger(1);
//	public static final ExecutorService SCHEDULING_POOL = Util.makeExecutor("ParticleTick");
	public static final ExecutorService SCHEDULING_POOL = Util.BACKGROUND_EXECUTOR;;

	public static void onTickBefore(int j) {
		if (beforeParticleFuture != null) {
			beforeParticleFuture.join();
			beforeParticleFuture = null;
		}
		if (j != 0) {
			shouldTickParticles = false;
		} else {
			cancelled = true;
			particleFuture.join();
			Minecraft mc = Minecraft.getInstance();
			if (!mc.isPaused()) {
				Collection<Queue<Particle>> values = mc.particleEngine.particles.values();
				var futures = new CompletableFuture[values.size()];
				int i = 0;
				for (Queue<Particle> particles1 : values) {
					futures[i++] = CompletableFuture.runAsync(() -> {
						if (particles1.isEmpty()) {
							return;
						}
						particles1.removeIf(particle1 -> ((ParticleAddon) particle1).asyncParticles$shouldRemove());
					}, SCHEDULING_POOL);
				}
				particleCleanup = CompletableFuture.allOf(futures);
			}
			cancelled = false;
			shouldTickParticles = true;
		}
	}

	public static void onTickAfter(int j) {
		if (tickParticleEngine != null) {
			ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
			profiler.push("particles");
			tickParticleEngine.call();
			tickParticleEngine = null;
			profiler.pop();
		}
		if (j != 0) {
			return;
		}
		List<Runnable> blockEntityOperations = AsyncTicker.blockEntityOperations;
		Runnable[] blockEntityTasks = blockEntityOperations.toArray(new Runnable[0]);
		blockEntityOperations.clear();
		CompletableFuture<Void> beforeParticleFuture = CompletableFuture.runAsync(() -> {
			for (Runnable blockEntityTask : blockEntityTasks) {
				blockEntityTask.run();
			}
		}, SCHEDULING_POOL).exceptionally(AsyncTicker::tickBeforeExceptionally);
		AsyncTicker.beforeParticleFuture = beforeParticleFuture;

		List<Runnable> endTickOperations = AsyncTicker.endTickOperations;
		Runnable[] endTickTasks = endTickOperations.toArray(new Runnable[0]);
		endTickOperations.clear();
		List<Runnable> particleOperations = AsyncTicker.particleOperations;
		Runnable[] particleTasks = particleOperations.toArray(new Runnable[0]);
		particleOperations.clear();

		particleFuture = beforeParticleFuture
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
				.map(runnable -> CompletableFuture.runAsync(runnable, SCHEDULING_POOL)
					.exceptionally(e -> {
						LOGGER.error("Error executing particle operation", e);
						return null;
					}))
				.toArray(CompletableFuture[]::new)));
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
}

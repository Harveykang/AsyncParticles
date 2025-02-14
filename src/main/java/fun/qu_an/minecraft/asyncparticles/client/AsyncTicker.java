package fun.qu_an.minecraft.asyncparticles.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.mixin.MixinParticleEngine;
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

public class AsyncTicker {
	public static final Logger LOGGER = LogManager.getLogger();
	public static final List<Runnable> particleOperations = new ArrayList<>();
	public static final List<Runnable> beforeParticleOperations = new ArrayList<>();
	public static boolean cancelled = false;
	public static boolean shouldTickParticles = true;
	public static CompletableFuture<Void> particleCleanup;
	public static Operation<Void> tickParticleEngine;
	public final static ArrayList<Runnable> endTickEvents = new ArrayList<>();
	private static CompletableFuture<Void> taskAll = CompletableFuture.completedFuture(null);
	//	private static final AtomicInteger WORKER_COUNT = new AtomicInteger(1);
//	public static final ExecutorService SCHEDULING_POOL = Util.makeExecutor("ParticleTick");
	public static final ExecutorService SCHEDULING_POOL = Util.BACKGROUND_EXECUTOR;

	public static void onTickBefore(int j) {
		if (j != 0) {
			shouldTickParticles = false;
		} else {
			cancelled = true;
			taskAll.join();
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
		List<Runnable> particleOperations = AsyncTicker.particleOperations;
		Runnable[] particleTasks = particleOperations.toArray(new Runnable[0]);
		particleOperations.clear();
		List<Runnable> blockEntityOperations = beforeParticleOperations;
		Runnable[] blockEntityTasks = blockEntityOperations.toArray(new Runnable[0]);
		blockEntityOperations.clear();
		taskAll = CompletableFuture.runAsync(() -> {
				for (Runnable blockEntityTask : blockEntityTasks) {
					blockEntityTask.run();
				}
				for (Runnable endTickEvent : endTickEvents) {
					endTickEvent.run();
				}
			}, SCHEDULING_POOL)
			.thenCompose(v -> CompletableFuture.allOf(Arrays.stream(particleTasks)
				.map(runnable -> CompletableFuture.runAsync(runnable, SCHEDULING_POOL)
					.exceptionally(e -> {
						LOGGER.error("Error executing particle operation", e);
						return null;
					}))
				.toArray(CompletableFuture[]::new)));
	}
}

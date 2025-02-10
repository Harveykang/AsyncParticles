package fun.qu_an.minecraft.asyncparticles.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AsyncTicker {
	public static final Logger LOGGER = LogManager.getLogger();
	public static final List<Runnable> particleOperations = new ArrayList<>();
	public static final List<Runnable> beforeParticleOperations = new ArrayList<>();
	public static boolean cancelled = false;
	public static boolean shouldTickParticles = true;
	public static CompletableFuture<Void> particleCleanup;
	public static Operation<Void> tickParticleEngine;
	private static CompletableFuture<Void> taskAll = CompletableFuture.completedFuture(null);

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
						particles1.removeIf(particle1 -> ((TickedParticle) particle1).asyncParticles$shouldRemove());
					}, Util.BACKGROUND_EXECUTOR);
				}
				particleCleanup = CompletableFuture.allOf(futures);
			}
			cancelled = false;
			shouldTickParticles = true;
		}
	}

	public static void onTickAfter(int j) {
		if (tickParticleEngine != null){
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
			}, Util.BACKGROUND_EXECUTOR)
			.thenCompose(v -> CompletableFuture.allOf(Arrays.stream(particleTasks)
				.map(runnable -> CompletableFuture.runAsync(runnable, Util.BACKGROUND_EXECUTOR)
					.exceptionally(e -> {
						LOGGER.error("Error executing particle operation", e);
						return null;
					}))
				.toArray(CompletableFuture[]::new)));
	}
}

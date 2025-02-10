package fun.qu_an.minecraft.asyncparticles.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncRenderer {
	private static final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
	private static final AtomicInteger WORKER_COUNT = new AtomicInteger(1);
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final ExecutorService executor;

	static {
		int clamp = Mth.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, Util.getMaxThreads());
		executor = new ForkJoinPool(Math.max(1, clamp), (forkJoinPool) -> {
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
			forkJoinWorkerThread.setName("AsyncParticle-" + WORKER_COUNT.getAndIncrement());
			return forkJoinWorkerThread;
		}, Util::onThreadException, true);
	}

	private static final Map<ParticleRenderType, BufferBuilder> BUFFER_BUILDERS = new ConcurrentHashMap<>();
	public static boolean isStart;
	private static CompletableFuture<Void> task;

	public static void add(ParticleRenderType particleRenderType, Runnable task) {
		tasks.add(task);
	}

	public static void start(PoseStack poseStack, float f, Camera camera, LightTexture lightTexture) {
		ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
		profiler.popPush("async_particles");
		isStart = true;
		Minecraft.getInstance().particleEngine.render(poseStack, null, lightTexture, camera, f);
		Runnable poll;
		var futures = new CompletableFuture[tasks.size()];
		int i = 0;
		while ((poll = tasks.poll()) != null) {
			futures[i++] = CompletableFuture.runAsync(poll, executor)
				.exceptionally(e -> {
					LOGGER.error("Exception while rendering particle", e);
					return null;
				});
		}
		task = CompletableFuture.allOf(futures);
//		profiler.pop();
	}

	public static void join(PoseStack poseStack, float f, Camera camera, LightTexture lightTexture) {
		ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
		profiler.popPush("async_particles");
		task.join();
		isStart = false;
//		LevelRenderer levelRenderer = var1.worldRenderer();
//		if (levelRenderer.transparencyChain != null){
////			levelRenderer.getTranslucentTarget().clear(Minecraft.ON_OSX);
////			levelRenderer.getTranslucentTarget().copyDepthFrom(mc.getMainRenderTarget());
//			RenderTarget particlesTarget = levelRenderer.getParticlesTarget();
//			particlesTarget.clear(Minecraft.ON_OSX);
//			particlesTarget.copyDepthFrom(Minecraft.getInstance().getMainRenderTarget());
//			RenderStateShard.PARTICLES_TARGET.setupRenderState();
//		} else {
////			if (levelRenderer.getTranslucentTarget() != null) {
////				levelRenderer.getTranslucentTarget().clear(Minecraft.ON_OSX);
////			}
//		}
		Minecraft.getInstance().particleEngine.render(poseStack, null, lightTexture, camera, f);
//		if (levelRenderer.transparencyChain != null){
//			RenderStateShard.PARTICLES_TARGET.clearRenderState();
//		}
//		profiler.pop();
	}

	public static BufferBuilder getBufferBuilder(ParticleRenderType particleRenderType) {
		return BUFFER_BUILDERS.computeIfAbsent(particleRenderType,
			k -> {
//				if (k != ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT) {
//					return new ThreadLocalBufferBuilder(RenderType.SMALL_BUFFER_SIZE, ForkJoinPool.getCommonPoolParallelism());
//				}
				return new BufferBuilder(RenderType.SMALL_BUFFER_SIZE);
			}); // 给多大好？
	}
}

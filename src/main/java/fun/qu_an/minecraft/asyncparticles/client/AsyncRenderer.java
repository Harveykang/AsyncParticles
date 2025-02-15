package fun.qu_an.minecraft.asyncparticles.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.properties.ParticleRenderingSettings;
import net.mehvahdjukaar.dummmmmmy.client.DamageNumberParticle;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.*;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncRenderer {
	public static final Set<Class<? extends Particle>> SYNC_PARTICLE_TYPES = Collections.newSetFromMap(new IdentityHashMap<>());

	static {
		SYNC_PARTICLE_TYPES.add(ItemPickupParticle.class);
		SYNC_PARTICLE_TYPES.add(MobAppearanceParticle.class);
		if (ModListHelper.DUMMMMMMY_LOADED) {
			SYNC_PARTICLE_TYPES.add(DamageNumberParticle.class);
		}
		// TODO: configure this set
	}

	public static final Map<ParticleRenderType, List<Particle>> SYNC_PARTICLES = new ConcurrentHashMap<>();
	private static final ArrayDeque<Runnable> ASYNC_QUEUE = new ArrayDeque<>();
	private static final AtomicInteger WORKER_COUNT = new AtomicInteger(1);
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final ForkJoinPool executor;

	static {
		int clamp = Mth.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, 5);
		executor = new ForkJoinPool(clamp, (forkJoinPool) -> {
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
	private static CompletableFuture<Void> asyncTask;

	public static void add(Runnable task) {
		ASYNC_QUEUE.add(task);
	}

	public static void start(PoseStack poseStack, float f, Camera camera, LightTexture lightTexture) {
		Minecraft mc = Minecraft.getInstance();
		ProfilerFiller profiler = mc.getProfiler();
		profiler.popPush("async_particles");
		isStart = true;
		MultiBufferSource.BufferSource bufferSource = mc.levelRenderer.renderBuffers.bufferSource();
		ParticleEngine particleEngine = mc.particleEngine;
		if (ModListHelper.IRIS_LOADED) {
			((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.EVERYTHING);
		}
		particleEngine.render(poseStack, bufferSource, lightTexture, camera, f);
		Runnable poll;
		var futures = new CompletableFuture[ASYNC_QUEUE.size()];
		int i = 0;
		while ((poll = ASYNC_QUEUE.poll()) != null) {
			futures[i++] = CompletableFuture.runAsync(poll, executor)
				.exceptionally(e -> {
					LOGGER.error("Exception while rendering particle", e);
					return null;
				});
		}
		asyncTask = CompletableFuture.allOf(futures);
		profiler.pop();
	}

	public static void irisOpaque(PoseStack poseStack, float f, Camera camera, LightTexture lightTexture) {
		if (!ModListHelper.IRIS_LOADED || !Iris.isPackInUseQuick() || getRenderingSettings() != ParticleRenderingSettings.MIXED) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("async_particles");
		LevelRenderer levelRenderer = mc.levelRenderer;
		if (levelRenderer.transparencyChain != null) {
			RenderTarget particlesTarget = levelRenderer.getParticlesTarget();
			particlesTarget.clear(Minecraft.ON_OSX);
			particlesTarget.copyDepthFrom(mc.getMainRenderTarget());
			RenderStateShard.PARTICLES_TARGET.setupRenderState();
		}
		asyncTask.join();
		isStart = false;
		MultiBufferSource.BufferSource bufferSource = levelRenderer.renderBuffers.bufferSource();

//		if (ModListHelper.IRIS_LOADED) {
//			if (getRenderingSettings() == ParticleRenderingSettings.MIXED) {

		ParticleEngine particleEngine = mc.particleEngine;
		((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.OPAQUE);
		particleEngine.render(poseStack, bufferSource, lightTexture, camera, f);

//			}
//		}
	}

	public static void irisTranslucent(PoseStack poseStack, float f, Camera camera, LightTexture lightTexture) {
		if (!ModListHelper.IRIS_LOADED || !Iris.isPackInUseQuick() || getRenderingSettings() != ParticleRenderingSettings.MIXED) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("async_particles");
		LevelRenderer levelRenderer = mc.levelRenderer;
		MultiBufferSource.BufferSource bufferSource = levelRenderer.renderBuffers.bufferSource();

//		if (ModListHelper.IRIS_LOADED) {
//			if (getRenderingSettings() == ParticleRenderingSettings.MIXED) {

		ParticleEngine particleEngine = mc.particleEngine;
		((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.TRANSLUCENT);
		particleEngine.render(poseStack, bufferSource, lightTexture, camera, f);

//			} else {
//				((PhasedParticleEngine) mc.particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.EVERYTHING);
//				mc.particleEngine.render(poseStack, bufferSource, lightTexture, camera, f);
//			}
//		} else {
//			mc.particleEngine.render(poseStack, bufferSource, lightTexture, camera, f);
//		}

		if (levelRenderer.transparencyChain != null) {
			RenderStateShard.PARTICLES_TARGET.clearRenderState();
		}
	}

	public static void join(PoseStack poseStack, float f, Camera camera, LightTexture lightTexture) {
		if (ModListHelper.IRIS_LOADED && Iris.isPackInUseQuick() && getRenderingSettings() == ParticleRenderingSettings.MIXED) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("async_particles");
		LevelRenderer levelRenderer = mc.levelRenderer;
		if (levelRenderer.transparencyChain != null) {
			RenderTarget particlesTarget = levelRenderer.getParticlesTarget();
			particlesTarget.clear(Minecraft.ON_OSX);
			particlesTarget.copyDepthFrom(mc.getMainRenderTarget());
			RenderStateShard.PARTICLES_TARGET.setupRenderState();
		}
		asyncTask.join();
		isStart = false;
		MultiBufferSource.BufferSource bufferSource = mc.levelRenderer.renderBuffers.bufferSource();

//		if (ModListHelper.IRIS_LOADED) {
//			if (getRenderingSettings() == ParticleRenderingSettings.MIXED) {
//				((PhasedParticleEngine) mc.particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.OPAQUE);
//				mc.particleEngine.render(poseStack, bufferSource, lightTexture, camera, f);
//				((PhasedParticleEngine) mc.particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.TRANSLUCENT);
//				mc.particleEngine.render(poseStack, bufferSource, lightTexture, camera, f);
//			} else {

		ParticleEngine particleEngine = mc.particleEngine;
		((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.EVERYTHING);
		particleEngine.render(poseStack, bufferSource, lightTexture, camera, f);

//			}
//		} else {
//			mc.particleEngine.render(poseStack, bufferSource, lightTexture, camera, f);
//		}

		if (levelRenderer.transparencyChain != null) {
			RenderStateShard.PARTICLES_TARGET.clearRenderState();
		}
	}

	public static ParticleRenderingSettings getRenderingSettings() {
		return Iris.getPipelineManager().getPipeline().map(WorldRenderingPipeline::getParticleRenderingSettings).orElse(ParticleRenderingSettings.MIXED);
	}

	public static BufferBuilder getBufferBuilder(ParticleRenderType particleRenderType) {
		return BUFFER_BUILDERS.computeIfAbsent(particleRenderType,
			k -> {
//				if (k != ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT) {
//					return new ThreadLocalBufferBuilder(RenderType.SMALL_BUFFER_SIZE, ForkJoinPool.getCommonPoolParallelism());
//				}
				return new BufferBuilder(RenderType.SMALL_BUFFER_SIZE / 2);
			}); // 给多大好？
	}

	public static void markAsSync(Class<? extends Particle> aClass) {
		synchronized (SYNC_PARTICLE_TYPES) {
			SYNC_PARTICLE_TYPES.add(aClass);
		}
	}

	public static boolean shouldSync(Class<? extends Particle> aClass) {
		return SYNC_PARTICLE_TYPES.contains(aClass);
	}

	public static void recordSync(ParticleRenderType particleRenderType, Particle particle) {
		List<Particle> particles = SYNC_PARTICLES.computeIfAbsent(particleRenderType, k -> new ArrayList<>());
		synchronized (particles) {
			particles.add(particle);
		}
	}

	public static List<? extends Particle> pollSync(ParticleRenderType particleRenderType) {
		List<Particle> list = SYNC_PARTICLES.put(particleRenderType, new ArrayList<>());
		return list == null ? List.of() : list;
	}
}

package fun.qu_an.minecraft.asyncparticles.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import fun.qu_an.minecraft.asyncparticles.client.util.FakeBeginBufferBuilder;
import fun.qu_an.minecraft.asyncparticles.client.util.FakeBufferBuilder;
import fun.qu_an.minecraft.asyncparticles.client.util.FakeTesselator;
import it.unimi.dsi.fastutil.Pair;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.properties.ParticleRenderingSettings;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

// TODO: 整理这一坨
public class AsyncRenderer {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Set<Class<? extends Particle>> SYNC_PARTICLE_TYPES = Collections.newSetFromMap(new IdentityHashMap<>());

	static {
		SYNC_PARTICLE_TYPES.add(ItemPickupParticle.class);
		SYNC_PARTICLE_TYPES.add(MobAppearanceParticle.class);
		if (ModListHelper.DUMMMMMMY_LOADED) {
			try {
				addSyncByClassName("net.mehvahdjukaar.dummmmmmy.client.DamageNumberParticle");
			} catch (Exception e) {
				LOGGER.error("", e);
			}
		}
		if (ModListHelper.FABRIC_EFFECTIVE_LOADED) {
			try {
				addSyncByClassName("org.ladysnake.effective.core.particle.SplashParticle");
			} catch (Exception e) {
				LOGGER.error("", e);
			}
		}
		if (ModListHelper.FORGE_EFFECTIVE_LOADED) {
			try {
				addSyncByClassName("concerrox.effective.particle.SplashParticle");
			} catch (Exception e) {
				LOGGER.error("", e);
			}
		}
		if (ModListHelper.TOMBSTONE_LOADED) {
			// tomestone may have duplicate ids with other mods, so we need to check if these classes are present
			try {
				addSyncByClassName("ovh.corail.tombstone.particle.ParticleCasting");
				addSyncByClassName("ovh.corail.tombstone.particle.ParticleGhost");
				addSyncByClassName("ovh.corail.tombstone.particle.ParticleGraveSoul");
				addSyncByClassName("ovh.corail.tombstone.particle.ParticleMagicCircle");
				addSyncByClassName("ovh.corail.tombstone.particle.ParticleMarker");
				addSyncByClassName("ovh.corail.tombstone.particle.ParticleRounding");
			} catch (Exception e) {
				LOGGER.error("", e);
			}
		}
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
		// TODO: configure this set
	}

	private static void addSyncByClassName(String className) throws Exception {
		SYNC_PARTICLE_TYPES.add((Class<? extends Particle>) Class.forName(className));
	}

	//	private static final Map<ParticleRenderType, List<Particle>> SYNC_PARTICLES = new ConcurrentHashMap<>();
	// some mod use zero content record class as particle render type, so we need to use IdentityHashMap to get rid of duplicated hashcode...
	private static final Map<ParticleRenderType, List<Particle>> SYNC_PARTICLES = Collections.synchronizedMap(new IdentityHashMap<>());
	private static final List<Runnable> ASYNC_QUEUE = new ArrayList<>();
	public static final ForkJoinPool EXECUTOR;
	public static final String THREAD_PREFIX = "AsyncParticleRenderer";
	public static final Set<Thread> PARTICLE_THREADS = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

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
			PARTICLE_THREADS.add(forkJoinWorkerThread);
			return forkJoinWorkerThread;
		}, Util::onThreadException, true);
	}

	//	private static final Map<ParticleRenderType, BufferBuilder> BUFFER_BUILDERS = new ConcurrentHashMap<>();
// some mod use zero content record class as particle render type, so we need to use IdentityHashMap to get rid of duplicated hashcode...
	private static final Map<ParticleRenderType, BufferBuilder> BUFFER_BUILDERS = new IdentityHashMap<>();
	public static Frustum frustum;
	private static Consumer<String> debugConsumer;
	public static boolean isStart;
	private static CompletableFuture<Void> asyncTask;

	/* Renderer */

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
		tryDebug();
		clearSync();
		var futures = new CompletableFuture[ASYNC_QUEUE.size()];
		for (int i = 0, asyncQueueSize = ASYNC_QUEUE.size(); i < asyncQueueSize; i++) {
			Runnable runnable = ASYNC_QUEUE.get(i);
			futures[i] = CompletableFuture.runAsync(runnable, EXECUTOR)
				.exceptionally(e -> {
					LOGGER.error("Error rendering particle", e);
					if (mc.level != null && mc.player != null){
						throw new RuntimeException(e);
					}
					return null;
				});
		}
		ASYNC_QUEUE.clear();
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

		ParticleEngine particleEngine = mc.particleEngine;
		((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.OPAQUE);
		particleEngine.render(poseStack, bufferSource, lightTexture, camera, f);
	}

	public static void irisTranslucent(PoseStack poseStack, float f, Camera camera, LightTexture lightTexture) {
		if (!ModListHelper.IRIS_LOADED || !Iris.isPackInUseQuick() || getRenderingSettings() != ParticleRenderingSettings.MIXED) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("async_particles");
		LevelRenderer levelRenderer = mc.levelRenderer;
		MultiBufferSource.BufferSource bufferSource = levelRenderer.renderBuffers.bufferSource();

		ParticleEngine particleEngine = mc.particleEngine;
		((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.TRANSLUCENT);
		particleEngine.render(poseStack, bufferSource, lightTexture, camera, f);

		if (levelRenderer.transparencyChain != null) {
			RenderStateShard.PARTICLES_TARGET.clearRenderState();
		}
	}

	// TODO: 是否需要在transparencyChain.process(partialTick)前调用？
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
		ParticleEngine particleEngine = mc.particleEngine;
		if (ModListHelper.IRIS_LOADED) {
			((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.EVERYTHING);
		}
		particleEngine.render(poseStack, bufferSource, lightTexture, camera, f);

		if (levelRenderer.transparencyChain != null) {
			RenderStateShard.PARTICLES_TARGET.clearRenderState();
		}
	}

	private static ParticleRenderingSettings getRenderingSettings() {
		return Iris.getPipelineManager().getPipeline().map(WorldRenderingPipeline::getParticleRenderingSettings).orElse(ParticleRenderingSettings.MIXED);
	}

	private static BufferBuilder getBufferBuilder(ParticleRenderType particleRenderType) {
		return BUFFER_BUILDERS.computeIfAbsent(particleRenderType,
			k -> new BufferBuilder(RenderType.SMALL_BUFFER_SIZE / 2)); // 给多大好？
	}

	/* BufferBuilder */

	private static final Map<ParticleRenderType, Pair<VertexFormat.Mode, VertexFormat>> FORMATS = new IdentityHashMap<>();
	private static final Pair<VertexFormat.Mode, VertexFormat> EMPTY_FORMAT = Pair.of(null, null);

	public static BufferBuilder beginBufferBuilder(ParticleRenderType particleRenderType, TextureManager textureManager) {
		BufferBuilder builder = getBufferBuilder(particleRenderType);
		if (builder.building()) {
			return builder;
		}
		Pair<VertexFormat.Mode, VertexFormat> pair = FORMATS.computeIfAbsent(particleRenderType, k -> computeVertexFormatPair(k, textureManager));
		if (pair == EMPTY_FORMAT) {
			return FakeBeginBufferBuilder.INSTANCE;
		}
		builder.begin(pair.first(), pair.second());
		return builder;
	}

	public static boolean begin(ParticleRenderType particleRenderType, TextureManager textureManager, BufferBuilder builder) {
		if (builder.building()) {
			return true;
		}
		Pair<VertexFormat.Mode, VertexFormat> pair = FORMATS.computeIfAbsent(particleRenderType, k -> computeVertexFormatPair(k, textureManager));
		if (pair == EMPTY_FORMAT) {
			return false;
		}
		builder.begin(pair.first(), pair.second());
		return true;
	}

	private static @NotNull Pair<VertexFormat.Mode, VertexFormat> computeVertexFormatPair(ParticleRenderType k, TextureManager textureManager) {
		// we try and store the vertex format/mode to avoid call begin() twice per frame...
		FakeBufferBuilder fakeBuilder = new FakeBufferBuilder();
		// compatibility shit...
		k.begin(fakeBuilder, textureManager);
		k.end(FakeTesselator.INSTANCE); // we must call end since some mod may reset something in this method
		VertexFormat.Mode mode = fakeBuilder.getVertexFormatMode();
		VertexFormat format = fakeBuilder.getFormat();
		if (mode == null || format == null) {
			return EMPTY_FORMAT;
		}
		return Pair.of(mode, format);
	}

	/* Sync Rendering */

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

	public static List<? extends Particle> getSync(ParticleRenderType particleRenderType) {
		List<Particle> list = SYNC_PARTICLES.get(particleRenderType);
		return list == null ? List.of() : list;
	}

	private static void clearSync() {
		SYNC_PARTICLES.values().forEach(List::clear);
	}

	/* Debug */

	public static void debugLater(Consumer<String> consumer) {
		debugConsumer = consumer;
	}

	private static void tryDebug() {
		if (debugConsumer != null) {
			debugConsumer.accept("""
				[Debug AsyncRenderer]
				async queue size: %d,
				sync particle count: %d,
				sync particle types: %s,
				iris particle state: %s"""
				.formatted(ASYNC_QUEUE.size(),
					SYNC_PARTICLES.values().stream().mapToInt(List::size).sum(),
					SYNC_PARTICLE_TYPES.stream().map(Class::getName).toList(),
					ModListHelper.IRIS_LOADED && Iris.isPackInUseQuick() ? getRenderingSettings().name() : "disabled"));
			debugConsumer = null;
		}
	}

	/* Config */

	public static boolean forceSyncLevelRenderMarkDirty() {
		return ModListHelper.SODIUM_LOADED // can't mark dirty asynchronously in sodium
			   || SimplePropertiesConfig.forceSyncLevelRenderMarkDirty;
	}

	/* Destroy */

	public static void destroy() {
		if (asyncTask != null) {
			// 应该不会到这里
			asyncTask.join();
//			asyncTask = null;
		}
		ASYNC_QUEUE.clear();
		FORMATS.clear();
		clearSync();
	}
}

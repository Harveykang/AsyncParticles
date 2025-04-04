package fun.qu_an.minecraft.asyncparticles.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import fun.qu_an.minecraft.asyncparticles.client.util.*;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.properties.ParticleRenderingSettings;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

// TODO: 整理这一坨
@Environment(EnvType.CLIENT)
public class AsyncRenderer {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Set<Class<? extends Particle>> SYNC_PARTICLE_TYPES = Collections.newSetFromMap(new IdentityHashMap<>());

	static {
		SYNC_PARTICLE_TYPES.add(ItemPickupParticle.class);
		SYNC_PARTICLE_TYPES.add(MobAppearanceParticle.class);
		if (ModListHelper.DUMMMMMMY_LOADED) {
			addSyncByClassName("net.mehvahdjukaar.dummmmmmy.client.DamageNumberParticle");
		}
		if (ModListHelper.FABRIC_EFFECTIVE_LOADED) {
			addSyncByClassName("org.ladysnake.effective.core.particle.SplashParticle");
		}
		if (ModListHelper.FORGE_EFFECTICULARITY_LOADED) {
			addSyncByClassName("concerrox.effective.particle.SplashParticle");
		}
		if (ModListHelper.TOMBSTONE_LOADED) {
			// tomestone may have duplicate ids with other mods, so we need to check if these classes are present
			addSyncByClassName("ovh.corail.tombstone.particle.ParticleCasting");
			addSyncByClassName("ovh.corail.tombstone.particle.ParticleGhost");
			addSyncByClassName("ovh.corail.tombstone.particle.ParticleGraveSoul");
			addSyncByClassName("ovh.corail.tombstone.particle.ParticleMagicCircle");
			addSyncByClassName("ovh.corail.tombstone.particle.ParticleMarker");
			addSyncByClassName("ovh.corail.tombstone.particle.ParticleRounding");
		}
		if (ModListHelper.PHYSICSMOD_LOADED) {
			addSyncByClassName("net.diebuddies.minecraft.weather.RainParticle");
			addSyncByClassName("net.diebuddies.minecraft.weather.DustParticle");
			addSyncByClassName("net.diebuddies.minecraft.weather.SnowParticle");
			addSyncByClassName("net.diebuddies.physics.ocean.RainParticle");
		}
		// TODO: configure this set
	}

	private static void addSyncByClassName(String className) {
		try {
			SYNC_PARTICLE_TYPES.add((Class<? extends Particle>) Class.forName(className));
		} catch (Exception e) {
			LOGGER.error("", e);
		}
	}

	//	private static final Map<ParticleRenderType, List<Particle>> SYNC_PARTICLES = new ConcurrentHashMap<>();
	// some mod use zero content record class as particle render type, so we need to use IdentityHashMap to get rid of duplicated hashcode...
	private static final Map<ParticleRenderType, Set<Particle>> SYNC_PARTICLES = Collections.synchronizedMap(new IdentityHashMap<>());
	public static final ForkJoinPool EXECUTOR;
	public static final String THREAD_PREFIX = "AsyncParticleRenderer";

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

	//	private static final Map<ParticleRenderType, BufferBuilder> BUFFER_BUILDERS = new ConcurrentHashMap<>();
// some mod use zero content record class as particle render type, so we need to use IdentityHashMap to get rid of duplicated hashcode...
	private static final Map<ParticleRenderType, BufferBuilder> BUFFER_BUILDERS = new IdentityHashMap<>();
	public static Frustum frustum;
	private static Consumer<String> debugConsumer;
	private static CompletableFuture<Void> asyncTask;
	private static boolean mixedParticleRenderingSetting = false;
	private static int asyncTasksSize;
	private static final ExceptionTracker<Class<? extends Particle>> EXCEPTION_TRACKER = new ExceptionTracker<>(
		() -> 5000,
		() -> SimplePropertiesConfig.renderFailurePerSecondThreshold
	);

	/* Renderer */

	public static void start(float f, Camera camera) {
		Minecraft mc = Minecraft.getInstance();
		ProfilerFiller profiler = mc.getProfiler();
		profiler.popPush("async_particles");
		ParticleEngine particleEngine = mc.particleEngine;
		if (ModListHelper.IRIS_LIKE_LOADED) {
			mixedParticleRenderingSetting = IrisApi.getInstance().isShaderPackInUse() &&
											getRenderingSettings() == ParticleRenderingSettings.MIXED;
//			((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.EVERYTHING);
		}
		profiler.push("render_async");
		TextureManager textureManager = particleEngine.textureManager;
		ObjectArrayList<CompletableFuture<Void>> asyncTasks = new ObjectArrayList<>(asyncTasksSize);
		for (ParticleRenderType particleRenderType
			: ModListHelper.IS_FORGE ? particleEngine.particles.keySet() : ParticleEngine.RENDER_ORDER) {
			if (particleRenderType == ParticleRenderType.NO_RENDER) {
				continue;
			}
			Queue<Particle> queue = particleEngine.particles.get(particleRenderType);
			if (queue == null || queue.isEmpty()) {
				continue;
			}
			BufferBuilder bufferBuilder = beginBufferBuilder(particleRenderType, textureManager);
			if (bufferBuilder == FakeBufferBuilder.INSTANCE) {
				continue;
			}
			asyncTasks.add(CompletableFuture.runAsync(() -> queue.forEach(particle ->
					renderParticle(f, camera, particle, particleRenderType, bufferBuilder)), EXECUTOR)
				.exceptionally(AsyncRenderer::renderAsyncExceptionally));
		}
		int size = asyncTasksSize = asyncTasks.size();
		asyncTask = CompletableFuture.allOf(asyncTasks.toArray(new CompletableFuture[size]));
		profiler.pop();
		tryDebug();
		clearSync();
	}

	private static void renderParticle(float f, Camera camera, Particle particle, ParticleRenderType particleRenderType, BufferBuilder bufferBuilder) {
		if (!particle.isAlive()) {
			return;
		}
		if (((ParticleAddon) particle).shouldCull() && !frustum.isVisible(particle.getBoundingBox())) {
			return;
		}
		if (((ParticleAddon) particle).asyncedParticles$isRenderSync()) {
			recordSync(particleRenderType, particle);
			return;
		}
		float g = ((ParticleAddon) particle).asyncParticles$isTicked() ? f : f + 1f;
		try {
			particle.render(bufferBuilder, camera, g);
		} catch (Throwable t) {
			boolean tolerable = AsyncTicker.isTolerable(t);
            if (tolerable && !EXCEPTION_TRACKER.addException(particle.getClass(), t)) {
                return;
            }
            ((ParticleAddon) particle).asyncedParticles$setRenderSync();
            if (!shouldSync(particle.getClass())) {
                if (!tolerable) {
                    LOGGER.warn("Exception while rendering particle {}, marking as sync", particle, t);
                } else {
                    LOGGER.warn("Exception {} thrown while rendering particle {} exceeds the threshold, please contact the author: {}",
                        t.getClass().getSimpleName(),
                        particle,
                        AsyncparticlesClient.ISSUE_URL,
                        t);
                }
                markAsSync(particle.getClass());
            }
            recordSync(particleRenderType, particle);
        }
	}

	private static Void renderAsyncExceptionally(Throwable e) {
		LOGGER.error("Error rendering particle", e);
		Minecraft mc1 = Minecraft.getInstance();
		if (mc1.level != null && mc1.player != null) {
			throw Utils.toThrowDirectly(e);
		}
		return null;
	}

	public static void join(PoseStack poseStack, float f, Camera camera, LightTexture lightTexture) {
		if (isMixedParticleRenderingSetting()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		ProfilerFiller profiler = mc.getProfiler();
		profiler.popPush("async_particles");
		LevelRenderer levelRenderer = mc.levelRenderer;
		if (levelRenderer.transparencyChain != null) {
			RenderTarget particlesTarget = levelRenderer.getParticlesTarget();
			particlesTarget.clear(Minecraft.ON_OSX);
			particlesTarget.copyDepthFrom(mc.getMainRenderTarget());
			RenderStateShard.PARTICLES_TARGET.setupRenderState();
		}
		profiler.push("wait_for_async_tasks");
		asyncTask.join();
		profiler.pop();

		MultiBufferSource.BufferSource bufferSource = mc.levelRenderer.renderBuffers.bufferSource();
		ParticleEngine particleEngine = mc.particleEngine;
		if (ModListHelper.IRIS_LIKE_LOADED) {
			((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.EVERYTHING);
		}
		particleEngine.render(poseStack, bufferSource, lightTexture, camera, f);

		if (levelRenderer.transparencyChain != null) {
			RenderStateShard.PARTICLES_TARGET.clearRenderState();
		}
	}

	public static void irisOpaque(PoseStack poseStack, float f, Camera camera, LightTexture lightTexture) {
		if (!isMixedParticleRenderingSetting()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		ProfilerFiller profiler = mc.getProfiler();
		profiler.popPush("async_particles");

		profiler.push("wait_for_async_tasks");
		asyncTask.join();
		profiler.pop();
		LevelRenderer levelRenderer = mc.levelRenderer;
		MultiBufferSource.BufferSource bufferSource = levelRenderer.renderBuffers.bufferSource();

		ParticleEngine particleEngine = mc.particleEngine;
		((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.OPAQUE);
		particleEngine.render(poseStack, bufferSource, lightTexture, camera, f);

//		poseStack1.last().pose().set(pose);
//		RenderSystem.applyModelViewMatrix();
	}

	public static void irisTranslucent(PoseStack poseStack, float f, Camera camera, LightTexture lightTexture) {
		if (!isMixedParticleRenderingSetting()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("async_particles");
		LevelRenderer levelRenderer = mc.levelRenderer;
		MultiBufferSource.BufferSource bufferSource = levelRenderer.renderBuffers.bufferSource();

		if (levelRenderer.transparencyChain != null) {
			RenderTarget particlesTarget = levelRenderer.getParticlesTarget();
			particlesTarget.clear(Minecraft.ON_OSX);
			particlesTarget.copyDepthFrom(mc.getMainRenderTarget());
			RenderStateShard.PARTICLES_TARGET.setupRenderState();
		}

		ParticleEngine particleEngine = mc.particleEngine;
		((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.TRANSLUCENT);
		particleEngine.render(poseStack, bufferSource, lightTexture, camera, f);

		if (levelRenderer.transparencyChain != null) {
			RenderStateShard.PARTICLES_TARGET.clearRenderState();
		}
	}

	public static void irisSync(PoseStack poseStack, float f, Camera camera, LightTexture lightTexture) {
		if (!isMixedParticleRenderingSetting()) {
			return;
		}
		PoseStack poseStack2 = null;
		Minecraft mc = Minecraft.getInstance();
		ParticleEngine particleEngine = mc.particleEngine;
		for (Map.Entry<ParticleRenderType, Queue<Particle>> entry : particleEngine.particles.entrySet()) {
			ParticleRenderType particleRenderType = entry.getKey();
			if (particleRenderType == ParticleRenderType.NO_RENDER) {
				continue;
			}
			Queue<Particle> queue = entry.getValue();
			if (queue.isEmpty()
				|| FORMATS.get(particleRenderType) != EMPTY_FORMAT) {
				continue;
			}
			if (poseStack2 == null) {
				lightTexture.turnOnLightLayer();
				RenderSystem.enableDepthTest();
				if (ModListHelper.IS_FORGE) {
					RenderSystem.activeTexture(33986);
					RenderSystem.activeTexture(33984);
				}
				poseStack2 = RenderSystem.getModelViewStack();
				poseStack2.pushPose();
				poseStack2.mulPoseMatrix(poseStack.last().pose());
				RenderSystem.applyModelViewMatrix();
			}
			RenderSystem.setShader(GameRenderer::getParticleShader);
			Tesselator tesselator = Tesselator.getInstance();
			// Some mod begin/end the bufferBuilder in render() method...
			// we need to provide one.
			BufferBuilder bufferBuilder = tesselator.getBuilder();
			boolean began = false;
			for (Particle particle : queue) {
				if (!particle.isAlive()) {
					continue;
				}
				if (((ParticleAddon) particle).shouldCull() && !frustum.isVisible(particle.getBoundingBox())) {
					continue;
				}
				if (!began) {
					particleRenderType.begin(bufferBuilder, particleEngine.textureManager);
					began = true;
				}
				float g = ((ParticleAddon) particle).asyncParticles$isTicked() ? f : f + 1f;
				try {
					particle.render(bufferBuilder, camera, g);
				} catch (Throwable t) {
					throw constructCrashReport(particle, particleRenderType, t);
				}
			}
			if (began) {
				particleRenderType.end(tesselator);
			}
		}
		if (poseStack2 != null) {
			poseStack2.popPose();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.depthMask(true);
			RenderSystem.disableBlend();
			lightTexture.turnOffLightLayer();
		}
	}

	public static ReportedException constructCrashReport(Particle particle, ParticleRenderType particleRenderType, Throwable t) {
		ReportedException re = Utils.getReportedException(t);
		if (re != null) {
			return re;
		}
		CrashReport crashReport = CrashReport.forThrowable(t, "Rendering Particle");
		CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being rendered");
		crashReportCategory.setDetail("Particle", particle::toString);
		crashReportCategory.setDetail("Particle Type", particleRenderType::toString);
		return new ReportedException(crashReport);
	}

	public static boolean isMixedParticleRenderingSetting() {
		return mixedParticleRenderingSetting;
	}

	private static ParticleRenderingSettings getRenderingSettings() {
		return Iris.getPipelineManager().getPipeline()
			.map(WorldRenderingPipeline::getParticleRenderingSettings)
			.orElse(ParticleRenderingSettings.MIXED);
	}

	/* BufferBuilder */

	private static final Map<ParticleRenderType, Pair<VertexFormat.Mode, VertexFormat>> FORMATS = new IdentityHashMap<>();
	private static final Pair<VertexFormat.Mode, VertexFormat> EMPTY_FORMAT = Pair.of(null, null);

	public static BufferBuilder beginBufferBuilder(ParticleRenderType particleRenderType, TextureManager textureManager) {
		Pair<VertexFormat.Mode, VertexFormat> pair = FORMATS.computeIfAbsent(particleRenderType, k -> computeVertexFormatPair(k, textureManager));
		if (pair == EMPTY_FORMAT) {
			return FakeBufferBuilder.INSTANCE;
		}
		BufferBuilder builder = BUFFER_BUILDERS.computeIfAbsent(particleRenderType,
			k -> new BufferBuilder(RenderType.TRANSIENT_BUFFER_SIZE)); // minimal size
		if (builder.building()) {
			return builder;
		}
		builder.begin(pair.first(), pair.second());
		return builder;
	}

	private static @NotNull Pair<VertexFormat.Mode, VertexFormat> computeVertexFormatPair(ParticleRenderType k, TextureManager textureManager) {
		// we try and store the vertex format/mode to avoid call begin() twice per frame...
		TryAndStoreFakeBufferBuilder fakeBufferBuilder = new TryAndStoreFakeBufferBuilder();
		// compatibility shit...
		k.begin(fakeBufferBuilder, textureManager);
		Exception exception = null;
		try {
			k.end(FakeTesselator.INSTANCE); // we must call end since some mod may reset something in this method
		} catch (Exception e) {
			// pray it doesn't throw...
			// this most breaks only one frame, so we can ignore it for now...
			exception = e;
			LOGGER.warn("Exception try&store-ing vertex format/mode for particle render type: {}", k, e);
		}
		VertexFormat.Mode mode = fakeBufferBuilder.getVertexFormatMode();
		VertexFormat format = fakeBufferBuilder.getFormat();
		if (mode != null && format != null) {
			return Pair.of(mode, format);
		}
		if (exception != null) {
			// this should never happen...
			// custom particles should not throw any exception in end()...
			throw Utils.toThrowDirectly(exception);
		}
		return EMPTY_FORMAT;
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
		Set<Particle> particles = SYNC_PARTICLES.computeIfAbsent(particleRenderType,
			k -> Collections.newSetFromMap(new IdentityHashMap<>()));
		synchronized (particles) {
			particles.add(particle);
		}
	}

	public static Set<Particle> getSync(ParticleRenderType particleRenderType) {
		Set<Particle> set = SYNC_PARTICLES.get(particleRenderType);
		return set == null ? Collections.emptySet() : set;
	}

	private static void clearSync() {
		SYNC_PARTICLES.clear();
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
				buffer capacity: %s,
				render order: %s,
				sync particle count: %d,
				sync particle types: %s,
				sync particle render types: %s,
				iris particle state: %s"""
				.formatted(asyncTasksSize,
					BUFFER_BUILDERS.entrySet()
						.stream()
						.collect(Collectors.toMap(
							Map.Entry::getKey,
							e -> e.getValue() instanceof FakeBufferBuilder ? 0 : e.getValue().buffer.capacity())),
					ModListHelper.IS_FORGE
						? Minecraft.getInstance().particleEngine.particles.keySet()
						: ParticleEngine.RENDER_ORDER,
					SYNC_PARTICLES.values().stream().mapToInt(Set::size).sum(),
					SYNC_PARTICLE_TYPES.stream().map(Class::getName).toList(),
					FORMATS.entrySet().stream()
						.filter(e -> e.getValue() == EMPTY_FORMAT)
						.map(Map.Entry::getKey).toList(),
					ModListHelper.IRIS_LIKE_LOADED && IrisApi.getInstance().isShaderPackInUse()
						? getRenderingSettings().name() : "disabled"));
			debugConsumer = null;
		}
	}

	/* Destroy */

	public static void destroy() {
		if (asyncTask != null) {
			// 应该不会到这里
			asyncTask.join();
//			asyncTask = null;
		}
		FORMATS.clear();
		clearSync();
	}
}

package fun.qu_an.minecraft.asyncparticles.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import dev.architectury.injectables.annotations.ExpectPlatform;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import fun.qu_an.minecraft.asyncparticles.client.util.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.irisshaders.iris.Iris;
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
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
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
			addSyncByClassName("org.ladysnake.effective.particle.SplashParticle");
		}
		if (ModListHelper.FORGE_EFFECTIVE_LOADED) {
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
		// TODO: configure this set
	}

	private static void addSyncByClassName(String className) {
		try {
			SYNC_PARTICLE_TYPES.add((Class<? extends Particle>) Class.forName(className));
		} catch (Exception e) {
			LOGGER.warn("", e);
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
		tryDebug();
		clearSync();
		resetBTesselators();
		captureParticleRenderingSetting();
		profiler.push("render_async");
		ParticleEngine particleEngine = mc.particleEngine;
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
			BindingTesselator bTesselator = getBTesselator(particleRenderType, textureManager);
			if (bTesselator.shouldSync) {
				continue;
			}
			asyncTasks.add(CompletableFuture.runAsync(
					() -> renderParticles(f, camera, queue, particleRenderType, bTesselator.begin()),
					EXECUTOR)
				.exceptionally(AsyncRenderer::renderAsyncExceptionally));
		}
		int size = asyncTasksSize = asyncTasks.size();
		asyncTask = CompletableFuture.allOf(asyncTasks.toArray(new CompletableFuture[size]));
		profiler.pop();
	}

	private static void renderParticles(float f,
										Camera camera,
										Queue<Particle> particles,
										ParticleRenderType particleRenderType,
										BufferBuilder bufferBuilder) {
		Frustum frustum = AsyncRenderer.frustum;
		float f2 = f + 1;
		for (Particle particle : particles) {
			if (!particle.isAlive()) {
				continue;
			}
			float f3 = ((ParticleAddon) particle).asyncparticles$isTicked() ? f : f2;
			if (SimplePropertiesConfig.isCullParticles() &&
				!frustum.isVisible(((ParticleAddon) particle).getRenderBoundingBox(f3))) {
				continue;
			}
			if (((ParticleAddon) particle).asyncparticles$isRenderSync()) {
				recordSync(particleRenderType, particle);
				continue;
			}
			try {
				particle.render(bufferBuilder, camera, f3);
			} catch (Throwable t) {
				boolean tolerable = AsyncTicker.isTolerable(t);
				if (tolerable && !EXCEPTION_TRACKER.addException(particle.getClass(), t)) {
					continue;
				}
				((ParticleAddon) particle).asyncparticles$setRenderSync();
				if (!shouldSync(particle.getClass())) {
					if (!tolerable) {
						LOGGER.warn("Exception while rendering particle {}, marking as sync", particle, t);
					} else {
						LOGGER.warn("Exception {} thrown while rendering particle {} exceeds the threshold, please contact the author: {}",
							t.getClass().getSimpleName(),
							particle,
							AsyncparticlesClient.ISSUE_URL);
						LOGGER.warn("", t);
					}
					markAsSync(particle.getClass());
				}
				recordSync(particleRenderType, particle);
			}
		}
	}

	private static Void renderAsyncExceptionally(Throwable e) {
		LOGGER.error("Error rendering particle", e);
		Minecraft mc1 = Minecraft.getInstance();
		if (mc1.level != null && mc1.player != null) {
			throw ExceptionUtil.toThrowDirectly(e);
		}
		return null;
	}

	@ExpectPlatform
	public static void irisOpaque(float f, Camera camera, LightTexture lightTexture, Predicate<ParticleRenderType> predicate) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static void irisTranslucent(float f, Camera camera, LightTexture lightTexture, Predicate<ParticleRenderType> predicate) {
		throw new AssertionError();
	}

	// TODO: 是否需要在transparencyChain.process(partialTick)前调用？
	public static void join(float f, Camera camera, LightTexture lightTexture) {
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
		waitForAsyncTasks();
		profiler.pop();

		ParticleEngine particleEngine = mc.particleEngine;
		if (ModListHelper.FABRIC_IRIS_LOADED) {
			((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.EVERYTHING);
		}
		particleEngine.render(lightTexture, camera, f);

		if (levelRenderer.transparencyChain != null) {
			RenderStateShard.PARTICLES_TARGET.clearRenderState();
		}
	}

	public static void waitForAsyncTasks() {
		if (asyncTask != null) {
			asyncTask.join();
			asyncTask = null;
		}
	}

	public static ReportedException constructCrashReport(Particle particle, ParticleRenderType particleRenderType, Throwable t) {
		AsyncTicker.debugLater(LOGGER::info);
		AsyncTicker.tryDebug();
		debugLater(LOGGER::info);
		tryDebug();
		CrashReport crashReport = CrashReport.forThrowable(t, "Rendering Particle");
		CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being rendered");
		crashReportCategory.setDetail("Particle", particle::toString);
		crashReportCategory.setDetail("Particle Type", particleRenderType::toString);
		return new ReportedException(crashReport);
	}

	private static void captureParticleRenderingSetting() {
		if (ModListHelper.IRIS_LIKE_LOADED) {
			mixedParticleRenderingSetting = Iris.isPackInUseQuick() &&
											getParticleRenderingSettings0() == ParticleRenderingSettings.MIXED;
		}
	}

	public static boolean isMixedParticleRenderingSetting() {
		return mixedParticleRenderingSetting;
	}

	private static ParticleRenderingSettings getParticleRenderingSettings0() {
		if (!Iris.isPackInUseQuick()) {
			return ParticleRenderingSettings.UNSET;
		}
		return Iris.getPipelineManager().getPipeline()
			.map(WorldRenderingPipeline::getParticleRenderingSettings)
			.orElse(ParticleRenderingSettings.MIXED);
	}

	/* BufferBuilder */

	private static final Map<ParticleRenderType, BindingTesselator> BTESSELATORS = new ConcurrentHashMap<>();

	private static void resetBTesselators() {
		BTESSELATORS.values().forEach(BindingTesselator::clear);
	}

	private static void closeBTesselators() {
		for (Iterator<BindingTesselator> iterator = BTESSELATORS.values().iterator(); iterator.hasNext(); ) {
			iterator.next().close();
			iterator.remove();
		}
	}

	public static BindingTesselator getBTesselator(ParticleRenderType particleRenderType, TextureManager textureManager) {
		return BTESSELATORS.computeIfAbsent(particleRenderType, k -> computeBTesselator(k, textureManager));
	}

	private static @NotNull BindingTesselator computeBTesselator(ParticleRenderType particleRenderType, TextureManager textureManager) {
//		if (particleRenderType == ParticleRenderType.CUSTOM) { // special case
//			return BindingTesselator.EMPTY;
//		}

		FakeBeginTesselator fakeBeginTesselator = FakeBeginTesselator.newFakeBeginTesselator();

		// FIXME: This will mess up the title screen render state
		//  because in 1.21.1 the ParticleRenderType#end method is removed
		BufferBuilder builder = particleRenderType.begin(fakeBeginTesselator, textureManager);

		RenderSystem.disableBlend();
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		RenderSystem.enableCull();
		RenderSystem.defaultBlendFunc();

		if (builder == null) {
			return BindingTesselator.EMPTY;
		}

		VertexFormat.Mode mode = fakeBeginTesselator.getMode();
		VertexFormat format = fakeBeginTesselator.getFormat();
		if (mode == null || format == null) {
			return BindingTesselator.EMPTY;
		}

		return new BindingTesselator(256, mode, format, particleRenderType == ParticleRenderType.CUSTOM); // minimal size
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

	@SuppressWarnings("ConstantValue")
	static void tryDebug() {
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
					BTESSELATORS.entrySet()
						.stream()
						.filter(e -> !e.getValue().shouldSync)
						.collect(Collectors.toMap(
							Map.Entry::getKey,
							e -> e.getValue().buffer.capacity)),
					ModListHelper.IS_FORGE
						? Minecraft.getInstance().particleEngine.particles.keySet()
						: ParticleEngine.RENDER_ORDER,
					SYNC_PARTICLES.values().stream().mapToInt(Set::size).sum(),
					SYNC_PARTICLE_TYPES.stream().map(Class::getName).toList(),
					BTESSELATORS.entrySet().stream()
						.filter(e -> e.getValue().shouldSync)
						.map(Map.Entry::getKey).toList(),
					ModListHelper.IRIS_LIKE_LOADED && Iris.isPackInUseQuick()
						? getParticleRenderingSettings0().name() : "disabled"));
			debugConsumer = null;
		}
	}

	/* Destroy */

	public static void reset() {
		waitForAsyncTasks();
		closeBTesselators();
		clearSync();
	}
}

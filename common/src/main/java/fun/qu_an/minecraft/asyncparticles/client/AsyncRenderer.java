package fun.qu_an.minecraft.asyncparticles.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import dev.architectury.injectables.annotations.ExpectPlatform;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.iris.IrisCompat;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.ParticleCullingMode;
import fun.qu_an.minecraft.asyncparticles.client.util.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
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
import java.util.stream.Collectors;

import static fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode.*;

// TODO: 整理这一坨
@Environment(EnvType.CLIENT)
public class AsyncRenderer {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Set<Class<? extends Particle>> SYNC_PARTICLE_TYPES = Collections.newSetFromMap(new IdentityHashMap<>());
	public static boolean renderAsync = false;
	private static boolean particlePhase = false;

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

	public static final ForkJoinPool EXECUTOR;
	public static final String THREAD_PREFIX = "AsyncParticleRenderer";

	static {
		AtomicInteger workerCount = new AtomicInteger(1);
		int clamp = Mth.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, 6);
		EXECUTOR = new ForkJoinPool(clamp, (forkJoinPool) -> {
			ForkJoinWorkerThread forkJoinWorkerThread = new AsyncRendererThread(forkJoinPool);
			forkJoinWorkerThread.setName(THREAD_PREFIX + "-" + workerCount.getAndIncrement());
			forkJoinWorkerThread.setDaemon(true);
			return forkJoinWorkerThread;
		}, Util::onThreadException, true);
	}

	public static Frustum frustum;
	private static Consumer<String> debugConsumer;
	private static CompletableFuture<Void> asyncTask;
	private static int asyncTasksSize;
	private static final ExceptionTracker<Class<? extends Particle>> EXCEPTION_TRACKER = new ExceptionTracker<>(
		() -> 5000,
		ConfigHelper::getRenderFailurePerSecondThreshold
	);

	/* Renderer */

	public static void start(float f, Camera camera, int irm) {
		tryDebug();
		if (InternalRenderingMode.isSync(irm)) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		ProfilerFiller profiler = mc.getProfiler();
		profiler.popPush("particles");
		clearSync();
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
			asyncTasks.add(CompletableFuture.runAsync(() -> renderParticles(f, camera, queue, particleRenderType, bTesselator.begin()),
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
		ParticleCullingMode particleCullingMode = ConfigHelper.getParticleCullingMode();
		float f2 = f + 1f;
		for (Particle particle : particles) {
			if (!particle.isAlive()) {
				continue;
			}
			float f3;
			ParticleAddon particleAddon = (ParticleAddon) particle;
			switch (particleCullingMode) {
				case AABB -> {
					f3 = particleAddon.asyncparticles$isTicked() ? f : f2;
					if (particleAddon.asyncparticles$shouldCull() &&
						!FrustumUtil.isVisible(frustum, particleAddon.getRenderBoundingBox(f3))) {
						continue;
					}
				}
				case SPHERE -> {
					if (particleAddon.asyncparticles$shouldCull() && !FrustumUtil.isVisible(frustum, particle)) {
						continue;
					}
					f3 = particleAddon.asyncparticles$isTicked() ? f : f2;
				}
				case ASYNC_AABB, ASYNC_SPHERE -> {
					if (particleAddon.asyncparticles$shouldCull() &&
						!particleAddon.asyncparticles$isVisibleOnScreen()) {
						continue;
					}
					f3 = particleAddon.asyncparticles$isTicked() ? f : f2;
				}
				default -> f3 = particleAddon.asyncparticles$isTicked() ? f : f2;
			}
			if (particleAddon.asyncparticles$isRenderSync()) {
				recordSync(particleRenderType, particle);
				continue;
			}
			try {
				particle.render(bufferBuilder, camera, f3);
			} catch (Throwable t) {
				onRenderingParticleException(particleRenderType, particle, t);
			}
		}
	}

	private static void onRenderingParticleException(ParticleRenderType particleRenderType, Particle particle, Throwable t) {
		boolean tolerable = AsyncTicker.isTolerable(t);
		Class<? extends Particle> particleClass = ((ParticleAddon) particle).asyncparticles$getRealClass();
		if (tolerable && !EXCEPTION_TRACKER.addException(particleClass, t)) {
			return;
		}
		((ParticleAddon) particle).asyncparticles$setRenderSync();
		if (!shouldSync(particleClass)) {
			if (!tolerable) {
				LOGGER.warn("Exception while rendering particle {}, marking as sync", particle, t);
			} else {
				LOGGER.warn("Exception {} thrown while rendering particle {} exceeds the threshold, please contact the author: {}",
					t.getClass().getSimpleName(),
					particle,
					AsyncParticlesClient.ISSUE_URL,
					t);
			}
			markAsSync(particleClass);
		}
		recordSync(particleRenderType, particle);
	}

	private static Void renderAsyncExceptionally(Throwable e) {
		LOGGER.error("Error rendering particle", e);
		Minecraft mc1 = Minecraft.getInstance();
		if (mc1.level != null && mc1.player != null) {
			throw ExceptionUtil.toThrowDirectly(e);
		}
		return null;
	}

	public static void endAll(float f, Camera camera, LightTexture lightTexture, boolean isAsync) {
//		if (!SimplePropertiesConfig.isRenderAsync()) { // Tested outside.
//			return;
//		}
//		if (isMixedParticleRendering()) { // Tested outside.
//			return;
//		}
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("particles");

		LevelRenderer levelRenderer = mc.levelRenderer;
		if (levelRenderer.transparencyChain != null) {
			RenderTarget particlesTarget = levelRenderer.getParticlesTarget();
			particlesTarget.clear(Minecraft.ON_OSX);
			particlesTarget.copyDepthFrom(mc.getMainRenderTarget());
			RenderStateShard.PARTICLES_TARGET.setupRenderState();
		}

		ParticleEngine particleEngine = mc.particleEngine;

		if (ModListHelper.FABRIC_IRIS_LOADED) {
			((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.EVERYTHING);
		}
		renderAsync = isAsync;
		particlePhase = true;
		particleEngine.render(lightTexture, camera, f);
		renderAsync = false;
		particlePhase = false;

		if (levelRenderer.transparencyChain != null) {
			RenderStateShard.PARTICLES_TARGET.clearRenderState();
		}
	}

	@ExpectPlatform
	public static void endOpaque(LightTexture lightTexture, Camera camera, float f, boolean isAsync) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static void endTranslucent(LightTexture lightTexture, Camera camera, float f, boolean isAsync) {
		throw new AssertionError();
	}

	public static boolean isRenderAsync() {
		return renderAsync;
	}

	public static boolean isParticlePhase() {
		return particlePhase;
	}

	public static void waitForAsyncTasks() {
		if (asyncTask != null) {
			asyncTask.join();
			asyncTask = null;
		}
	}

	public static void tryWaitingForAsyncTasks() {
//		if (ConfigHelper.isRenderAsync() && stage != Stage.RENDERABLE) {
//			throw new IllegalStateException("Can only wait for async tasks around translucent");
//		}
		waitForAsyncTasks();
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

		TryAndStoreFakeBufferBuilder fakeBufferBuilder = new TryAndStoreFakeBufferBuilder();

		BufferBuilder builder = particleRenderType.begin(fakeBufferBuilder, textureManager);

		RenderSystem.disableBlend();
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		RenderSystem.enableCull();
		RenderSystem.defaultBlendFunc();

		if (builder == null) {
			return BindingTesselator.EMPTY;
		}

		VertexFormat.Mode mode = fakeBufferBuilder.getMode();
		VertexFormat format = fakeBufferBuilder.getFormat();
		if (mode == null || format == null) {
			return BindingTesselator.EMPTY;
		}

		return new BindingTesselator(256, mode, format, particleRenderType == ParticleRenderType.CUSTOM); // minimal size
	}

	/* Sync Rendering */

	private static final Map<ParticleRenderType, Set<Particle>> SYNC_PARTICLES = Collections.synchronizedMap(new IdentityHashMap<>());

	public static void markAsSync(Class<? extends Particle> aClass) {
		synchronized (SYNC_PARTICLE_TYPES) {
			SYNC_PARTICLE_TYPES.add(aClass);
		}
	}

	public static boolean shouldSync(Class<?> aClass) {
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
				particle mode: %s,
				iris particle mode: %s"""
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
					switch (InternalRenderingMode.getMode()) {
						case SYNC -> "SYNC";
						case DELAYED_ASYNC -> "DELAYED_ASYNC";
						case BEFORE_SYNC -> "BEFORE_SYNC";
						case COMPATIBILITY_ASYNC -> "COMPATIBILITY_ASYNC";
						case MIXED_SYNC -> "MIXED_SYNC";
						case BEFORE_ASYNC -> "BEFORE_ASYNC";
						case MIXED_ASYNC -> "MIXED_ASYNC";
						default -> "UNKNOWN";
					},
					ModListHelper.IRIS_LIKE_LOADED ? IrisCompat.getParticleRenderingSettings().name() : "DISABLED"));
			debugConsumer = null;
		}
	}

	/* Destroy */

	public static void reset() {
		renderAsync = false;
		particlePhase = false;
		waitForAsyncTasks();
		closeBTesselators();
		clearSync();
	}

	public static class AsyncRendererThread extends AsyncParticleWorkerThread {
		public AsyncRendererThread(ForkJoinPool forkJoinPool) {
			super(forkJoinPool);
		}

		protected void onTermination(Throwable throwable) {
			if (throwable != null) {
				LOGGER.warn("{} died", this.getName(), throwable);
			} else {
				LOGGER.debug("{} shutdown", this.getName());
			}

			super.onTermination(throwable);
		}
	}
}

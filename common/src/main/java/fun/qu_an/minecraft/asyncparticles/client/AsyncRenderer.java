package fun.qu_an.minecraft.asyncparticles.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
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
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
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

import static fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode.*;

// TODO: Organize this shit
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

	@NotNull
	public static Frustum frustum = new Frustum(new Matrix4f(), new Matrix4f());
	private static Consumer<String> debugConsumer;
	private static CompletableFuture<Void> particlsTask;
	private static boolean mixedParticleRenderingSetting = false;
	private static int asyncTasksSize;
	public static boolean particlePhase = false;
	private static final ExceptionTracker<Class<? extends Particle>> EXCEPTION_TRACKER = new ExceptionTracker<>(
		() -> 5000,
		ConfigHelper::getRenderFailurePerSecondThreshold
	);

	/* Renderer */

	public static void begin(float partialTick, Camera camera, int irm) {
		tryDebug();
		switch (irm) {
			case MIXED_SYNC -> {
				mixedParticleRenderingSetting = true;
				return;
			}
			case SYNC, BEFORE_SYNC -> {
				mixedParticleRenderingSetting = false;
				return;
			}
			case MIXED_ASYNC -> mixedParticleRenderingSetting = true;
			default -> mixedParticleRenderingSetting = false;
		}
		Minecraft mc = Minecraft.getInstance();
		ProfilerFiller profiler = Profiler.get();
		profiler.push("begin_particles");
		clearSync();
		ParticleEngine particleEngine = mc.particleEngine;
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
			BindingTesselator bTesselator = getBTesselator(particleRenderType);
			if (bTesselator == BindingTesselator.EMPTY) {
				continue;
			}
			asyncTasks.add(CompletableFuture.runAsync(() -> renderParticles(partialTick, camera, queue, particleRenderType, bTesselator.begin()),
					EXECUTOR)
				.exceptionally(AsyncRenderer::renderAsyncExceptionally));
		}
		int size = asyncTasksSize = asyncTasks.size();
		particlsTask = CompletableFuture.allOf(asyncTasks.toArray(new CompletableFuture[size]));
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
					AsyncParticlesClient.ISSUE_URL_STR,
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

	// Fabric
	public static void endAll(Camera camera, float f, Collection<ParticleRenderType> renderOrder) {
		tryWaitingForParticleTask();
		float f2 = f + 1f;
		for (ParticleRenderType particleRenderType : renderOrder) {
			BindingTesselator tesselator = BTESSELATORS.get(particleRenderType);
			if (tesselator == null || tesselator == BindingTesselator.EMPTY) {
				continue;
			}
			BufferBuilder builder;
			Set<Particle> sync = getSync(particleRenderType);
			if (sync.isEmpty()) {
				builder = tesselator.getBuilder();
				if (builder == null ||
					!builder.building) {
					continue;
				}
			} else {
				builder = tesselator.begin();
				for (Particle particle : sync) {
					if (!particle.isAlive()) {
						continue;
					}
					// All culling have been processed async
					float f3 = ((ParticleAddon) particle).asyncparticles$isTicked() ? f : f2;
					try {
						particle.render(builder, camera, f3);
					} catch (Throwable t) {
						throw AsyncRenderer.constructCrashReport(particle, particleRenderType, t);
					}
				}
			}
			MeshData meshData = builder.build();
			if (meshData == null) {
				continue;
			}
			RenderType renderType = particleRenderType.renderType();
//			assert renderType != null;
			if (renderType.sortOnUpload()) {
				meshData.sortQuads(tesselator.buffer, RenderSystem.getProjectionType().vertexSorting());
			}
			renderType.draw(meshData);
		}
	}

	// Forge
	public static void endAll(Camera camera, float f, Collection<ParticleRenderType> renderOrder, Predicate<ParticleRenderType> renderTypePredicate) {
		tryWaitingForParticleTask();
		float f2 = f + 1f;
		for (ParticleRenderType particleRenderType : renderOrder) {
			if (particleRenderType.renderType() == null) {
				continue;
			}
			if (!renderTypePredicate.test(particleRenderType)) {
				continue;
			}
			BindingTesselator tesselator = BTESSELATORS.get(particleRenderType);
			if (tesselator == null || tesselator == BindingTesselator.EMPTY) {
				continue;
			}
			BufferBuilder builder;
			Set<Particle> sync = getSync(particleRenderType);
			if (sync.isEmpty()) {
				builder = tesselator.getBuilder();
				if (builder == null ||
					!builder.building) {
					continue;
				}
			} else {
				builder = tesselator.begin();
				for (Particle particle : sync) {
					if (!particle.isAlive()) {
						continue;
					}
					// All culling have been processed async
					float f3 = ((ParticleAddon) particle).asyncparticles$isTicked() ? f : f2;
					try {
						particle.render(builder, camera, f3);
					} catch (Throwable t) {
						throw AsyncRenderer.constructCrashReport(particle, particleRenderType, t);
					}
				}
			}
			MeshData meshData = builder.build();
			if (meshData == null) {
				continue;
			}
			RenderType renderType = particleRenderType.renderType();
//			assert renderType != null;
			if (renderType.sortOnUpload()) {
				meshData.sortQuads(tesselator.buffer, RenderSystem.getProjectionType().vertexSorting());
			}
			renderType.draw(meshData);
		}
	}

	public static boolean isParticlePhase() {
		return particlePhase;
	}

	private static void waitForParticleTask() {
		if (particlsTask != null) {
			particlsTask.join();
			particlsTask = null;
		}
	}

	private static void tryWaitingForParticleTask() {
//		if (ConfigHelper.isRenderAsync() && stage != Stage.RENDERABLE) {
//			throw new IllegalStateException("Can only wait for async tasks around translucent");
//		}
		waitForParticleTask();
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

	public static boolean isMixedParticleRendering() {
		return mixedParticleRenderingSetting;
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

	public static BindingTesselator getBTesselator(ParticleRenderType particleRenderType) {
		return BTESSELATORS.computeIfAbsent(particleRenderType, AsyncRenderer::computeBTesselator);
	}

	private static @NotNull BindingTesselator computeBTesselator(ParticleRenderType particleRenderType) {
		RenderType renderType = particleRenderType.renderType();
		if (renderType == null) { // special case
			return BindingTesselator.EMPTY;
		}
		return new BindingTesselator(256, renderType.mode(), renderType.format()); // minimal size
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
				iris particle mode: %s,
				weather column array size(rain|snow): %d|%d"""
				.formatted(asyncTasksSize,
					BTESSELATORS.entrySet()
						.stream()
						.filter(e -> e.getValue() != BindingTesselator.EMPTY)
						.collect(Collectors.toMap(
							entry -> entry.getKey().name(),
							e -> e.getValue().buffer.capacity)),
					(ModListHelper.IS_FORGE
						? Minecraft.getInstance().particleEngine.particles.keySet()
						: ParticleEngine.RENDER_ORDER)
						.stream().map(ParticleRenderType::name).toList(),
					SYNC_PARTICLES.values().stream().mapToInt(Set::size).sum(),
					SYNC_PARTICLE_TYPES.stream().map(Class::getName).toList(),
					BTESSELATORS.entrySet().stream()
						.filter(e -> e.getValue() == BindingTesselator.EMPTY)
						.map(p -> p.getKey().name())
						.toList(),
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
					ModListHelper.IRIS_LIKE_LOADED ? IrisCompat.getParticleRenderingSettings().name() : "DISABLED",
					WeatherRenderer.rainColumns.length,
					WeatherRenderer.snowColumns.length
				));
			debugConsumer = null;
		}
	}

	/* Destroy */

	public static void reset() {
		particlePhase = false;
		try {
			waitForParticleTask();
		} catch (Exception e) {
			LOGGER.error("Error waiting for particle task while resetting async renderer", e);
		}
		try {
			WeatherRenderer.waitForWeatherTask();
		} catch (Exception e) {
			LOGGER.error("Error waiting for weather task while resetting async renderer", e);
		}
		closeBTesselators();
		clearSync();
		WeatherRenderer.reset();
	}

	public static boolean isTranslucentPhase(Enum<?> phase) {
		return phase == ParticleRenderingPhase.TRANSLUCENT;
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

package fun.qu_an.minecraft.asyncparticles.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.logging.LogUtils;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import fun.qu_an.minecraft.asyncparticles.client.util.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
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
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
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
	public static CompletableFuture<Void> asyncTask;
	private static ParticleRenderingSettings particleRenderingSettings;
	private static int asyncTasksSize;
	private static final ExceptionTracker<Class<? extends Particle>> EXCEPTION_TRACKER = new ExceptionTracker<>(
		() -> 5000,
		() -> SimplePropertiesConfig.renderFailurePerSecondThreshold
	);

	/* Renderer */

	public static void start(float f, Camera camera, Matrix4f frustumMatrix, Matrix4f projectionMatrix) {
		Minecraft mc = Minecraft.getInstance();
		LevelRenderer levelRenderer = mc.levelRenderer;
		ProfilerFiller profiler = Profiler.get();
		profiler.popPush("culling");
		boolean flag = levelRenderer.capturedFrustum != null;
		frustum = flag ? levelRenderer.capturedFrustum : levelRenderer.cullingFrustum;
		profiler.popPush("captureFrustum");
		if (levelRenderer.captureFrustum) {
			Vec3 vec3 = camera.getPosition();
			levelRenderer.capturedFrustum = flag ? new Frustum(frustumMatrix, projectionMatrix) : levelRenderer.cullingFrustum;
			levelRenderer.capturedFrustum.prepare(vec3.x, vec3.y, vec3.z);
			levelRenderer.captureFrustum = false;
		}
		if (!SimplePropertiesConfig.isRenderAsync()) {
			AsyncRenderer.captureParticleRenderingSetting();
			AsyncRenderer.tryDebug();
			return;
		}
		profiler.popPush("async_particles");
		resetBTesselators();
		ParticleEngine particleEngine = mc.particleEngine;
		captureParticleRenderingSetting();
		profiler.push("render_async");
		TextureManager textureManager = particleEngine.textureManager;
		Collection<ParticleRenderType> renderOrder = ModListHelper.IS_FORGE
			? particleEngine.particles.keySet()
			: ParticleEngine.RENDER_ORDER;
		ObjectArrayList<CompletableFuture<Void>> asyncTasks = new ObjectArrayList<>(renderOrder.size());
		for (ParticleRenderType particleRenderType : renderOrder) {
			if (particleRenderType.renderType() == null) {
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
			CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
					renderParticles(f, camera, queue, particleRenderType, bufferBuilder), EXECUTOR)
				.exceptionally(AsyncRenderer::renderAsyncExceptionally);
			asyncTasks.add(future);
		}
		int size = asyncTasksSize = asyncTasks.size();
		asyncTask = CompletableFuture.allOf(asyncTasks.toArray(new CompletableFuture[size]));
		tryDebug();
		clearSync();
		profiler.pop();
	}

	public static void captureParticleRenderingSetting() {
		if (ModListHelper.IRIS_LIKE_LOADED && Iris.isPackInUseQuick()) {
			particleRenderingSettings = getParticleRenderingSettings0();
		}
	}

	private static void renderParticles(float f, Camera camera, Queue<Particle> particles, ParticleRenderType particleRenderType, BufferBuilder bufferBuilder) {
		float f2 = f + 1f;
		Frustum frustum = AsyncRenderer.frustum;
		for (Particle particle : particles) {
			if (!particle.isAlive()) {
				continue;
			}
			float f3 = ((ParticleAddon) particle).asyncParticles$isTicked() ? f : f2;
			if (SimplePropertiesConfig.isCullParticles() && !FrustumUtil.isVisible(frustum, ((ParticleAddon) particle).getRenderBoundingBox(f3))) {
				continue;
			}
			if (((ParticleAddon) particle).asyncedParticles$isRenderSync()) {
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
	public static void endAll(Collection<ParticleRenderType> renderOrder) {
		waitForAsyncTasks();
		for (ParticleRenderType particleRenderType : renderOrder) {
			BindingTesselator tesselator = BTESSELATORS.get(particleRenderType);
			if (tesselator == null || tesselator == BindingTesselator.EMPTY) {
				continue;
			}
			BufferBuilder builder = tesselator.getBuilder();
			if (builder == null ||
				!builder.building) {
				continue;
			}
			MeshData meshData = builder.build();
			if (meshData == null) {
				continue;
			}
			RenderType renderType = particleRenderType.renderType();
			assert renderType != null;
			if (renderType.sortOnUpload()) {
				meshData.sortQuads(tesselator.buffer, RenderSystem.getProjectionType().vertexSorting());
			}
			renderType.draw(meshData);
		}
	}

	// Forge
	public static void endAll(Predicate<ParticleRenderType> renderTypePredicate, Collection<ParticleRenderType> renderOrder) {
		waitForAsyncTasks();
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
			BufferBuilder builder = tesselator.getBuilder();
			if (builder == null ||
				!builder.building) {
				continue;
			}
			MeshData meshData = builder.build();
			if (meshData == null) {
				continue;
			}
			RenderType renderType = particleRenderType.renderType();
			assert renderType != null;
			if (renderType.sortOnUpload()) {
				meshData.sortQuads(tesselator.buffer, RenderSystem.getProjectionType().vertexSorting());
			}
			renderType.draw(meshData);
		}
	}

	public static void waitForAsyncTasks() {
		if (asyncTask != null) {
			asyncTask.join();
			asyncTask = null;
		}
	}

	public static ReportedException constructCrashReport(Particle particle, ParticleRenderType particleRenderType, Throwable t) {
		if (t instanceof ReportedException re) {
			return re;
		}
		CrashReport crashReport = CrashReport.forThrowable(t, "Rendering Particle");
		CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being rendered");
		crashReportCategory.setDetail("Particle", particle::toString);
		crashReportCategory.setDetail("Particle Type", particleRenderType::toString);
		if (particleRenderType.renderType() != null) {
			crashReportCategory.setDetail("Render Type", particleRenderType.renderType()::toString);
		} else {
			crashReportCategory.setDetail("Render Type", "Custom");
		}
		return new ReportedException(crashReport);
	}

	public static boolean isMixedParticleRenderingSetting() {
		return particleRenderingSettings == ParticleRenderingSettings.MIXED;
	}

	public static ParticleRenderingSettings getParticleRenderingSettings() {
		return particleRenderingSettings;
	}

	private static ParticleRenderingSettings getParticleRenderingSettings0() {
		return Iris.getPipelineManager().getPipeline()
			.map(WorldRenderingPipeline::getParticleRenderingSettings)
			.orElse(ParticleRenderingSettings.MIXED);
	}

	/* BufferBuilder */

	private static final Map<ParticleRenderType, BindingTesselator> BTESSELATORS = new IdentityHashMap<>();

	private static void resetBTesselators() {
		BTESSELATORS.values().forEach(BindingTesselator::clear);
	}

	private static void closeBTesselators() {
		for (Iterator<BindingTesselator> iterator = BTESSELATORS.values().iterator(); iterator.hasNext(); ) {
			iterator.next().close();
			iterator.remove();
		}
	}

	public static @NotNull BufferBuilder beginBufferBuilder(ParticleRenderType particleRenderType, TextureManager textureManager) {
		// assert main thread
		return BTESSELATORS.computeIfAbsent(particleRenderType,
			k -> computeBTesselator(k, textureManager)).begin();
	}

	private static @NotNull BindingTesselator computeBTesselator(ParticleRenderType particleRenderType, TextureManager textureManager) {
		RenderType renderType = particleRenderType.renderType();
		if (renderType == null) { // special case
			return BindingTesselator.EMPTY;
		}
		return new BindingTesselator(256, renderType.mode(), renderType.format()); // minimal size
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
	public static void tryDebug() {
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
						.filter(e -> e.getValue().buffer != null)
						.collect(Collectors.toMap(
							e -> e.getKey().name(),
							e -> e.getValue().buffer.capacity)),
					ModListHelper.IS_FORGE
						? Minecraft.getInstance().particleEngine.particles.keySet()
						.stream()
						.map(ParticleRenderType::name)
						.toList()
						: ParticleEngine.RENDER_ORDER
						.stream()
						.map(ParticleRenderType::name)
						.toList(),
					SYNC_PARTICLES.values().stream().mapToInt(Set::size).sum(),
					SYNC_PARTICLE_TYPES.stream().map(Class::getName).toList(),
					BTESSELATORS.entrySet().stream()
						.filter(e -> e.getValue() == BindingTesselator.EMPTY)
						.map(p -> p.getKey().name())
						.toList(),
					ModListHelper.IRIS_LIKE_LOADED && IrisApi.getInstance().isShaderPackInUse()
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

	public static boolean isTranslucentPhase(Object phase) {
		return phase == ParticleRenderingPhase.TRANSLUCENT;
	}

	public static boolean isOpaquePhase(Object phase) {
		return phase == ParticleRenderingPhase.OPAQUE;
	}
}

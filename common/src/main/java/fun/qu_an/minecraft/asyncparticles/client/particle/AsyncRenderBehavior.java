package fun.qu_an.minecraft.asyncparticles.client.particle;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.GLCaps;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.iris.IrisCompat;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.ParticleCullingMode;
import fun.qu_an.minecraft.asyncparticles.client.util.*;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
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
import org.joml.Matrix4f;
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

// TODO: Organize this shit
@Environment(EnvType.CLIENT)
public class AsyncRenderBehavior {
	public static final AsyncRenderBehavior INSTANCE = new AsyncRenderBehavior();
	private static final Logger LOGGER = LogUtils.getLogger();
	private final Set<Class<? extends Particle>> syncParticleTypes = Collections.newSetFromMap(new IdentityHashMap<>());
	private boolean renderAsync = false;
	private boolean particlePhase = false;

	{
		syncParticleTypes.add(ItemPickupParticle.class);
		syncParticleTypes.add(MobAppearanceParticle.class);
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
		// TODO: configure this set
	}

	private void addSyncByClassName(String className) {
		try {
			syncParticleTypes.add((Class<? extends Particle>) Class.forName(className));
		} catch (Exception e) {
			LOGGER.error("", e);
		}
	}

	public final ForkJoinPool EXECUTOR;
	public final String THREAD_PREFIX = "AsyncParticleRenderer";
	public final int THREADS = Mth.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, 6);

	{
		AtomicInteger workerCount = new AtomicInteger(1);
		EXECUTOR = new ForkJoinPool(THREADS, (forkJoinPool) -> {
			ForkJoinWorkerThread forkJoinWorkerThread = new AsyncRendererThread(forkJoinPool);
			forkJoinWorkerThread.setName(THREAD_PREFIX + "-" + workerCount.getAndIncrement());
			forkJoinWorkerThread.setDaemon(true);
			return forkJoinWorkerThread;
		}, Util::onThreadException, true);
	}

	private Frustum frustum = new Frustum(new Matrix4f(), new Matrix4f());
	private Consumer<String> debugConsumer;
	private CompletableFuture<Void> asyncTask;
	private boolean irisEarlyOpaquePhase = false;
	private int asyncTasksSize;
	private final ExceptionTracker<Class<? extends Particle>> EXCEPTION_TRACKER = new ExceptionTracker<>(
		() -> 5000,
		ConfigHelper::getRenderFailurePerSecondThreshold
	);

	/* Renderer */

	public void start(float f, Camera camera, int irm) {
		tryDebug();
		switch (irm) {
			case MIXED_SYNC, BEFORE_SYNC -> {
				irisEarlyOpaquePhase = true;
				return;
			}
			case SYNC -> {
				irisEarlyOpaquePhase = ModListHelper.IRIS_LIKE_LOADED;
				return;
			}
			case MIXED_ASYNC, BEFORE_ASYNC -> irisEarlyOpaquePhase = true;
			case COMPATIBILITY_ASYNC -> irisEarlyOpaquePhase = ModListHelper.IRIS_LIKE_LOADED;
			default -> irisEarlyOpaquePhase = false;
		}
		Minecraft mc = Minecraft.getInstance();
		ProfilerFiller profiler = mc.getProfiler();
		profiler.popPush("particles");
		clearSync();
		profiler.push("render_async");
		ParticleEngine particleEngine = mc.particleEngine;
		TextureManager textureManager = particleEngine.textureManager;
		ObjectArrayList<CompletableFuture<Void>> asyncTasks = new ObjectArrayList<>(asyncTasksSize);
		for (ParticleRenderType particleRenderType :
			ModListHelper.IS_FORGE ? particleEngine.particles.keySet() : ParticleEngine.RENDER_ORDER) {
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
			asyncTasks.add(CompletableFuture.runAsync(() -> renderParticles(f, camera, queue, particleRenderType, bufferBuilder), EXECUTOR)
				.exceptionally(this::renderAsyncExceptionally));
		}
		int size = asyncTasksSize = asyncTasks.size();
		asyncTask = CompletableFuture.allOf(asyncTasks.toArray(new CompletableFuture[size]));
		profiler.pop();
	}

	private void renderParticles(float f,
										Camera camera,
										Queue<Particle> particles,
										ParticleRenderType particleRenderType,
										BufferBuilder bufferBuilder) {
		Frustum frustum = this.getFrustum();
		ParticleCullingMode particleCullingMode = ConfigHelper.getParticleCullingMode();
		float f2 = f + 1f;
		for (Particle particle : particles) {
			if (!particle.isAlive()) {
				continue;
			}
			ParticleAddon particleAddon = (ParticleAddon) particle;
			switch (particleCullingMode) {
				case AABB -> {
					if (particleAddon.shouldCull() &&
						!FrustumUtil.isVisible(frustum, particle.getBoundingBox())) {
						continue;
					}
				}
				case SPHERE -> {
					if (particleAddon.shouldCull() && !FrustumUtil.isVisible(frustum, particle)) {
						continue;
					}
				}
				case ASYNC_AABB, ASYNC_SPHERE -> {
					if (particleAddon.shouldCull() &&
						!particleAddon.asyncparticles$isVisibleOnScreen()) {
						continue;
					}
				}
			}
			if (particleAddon.asyncparticles$isRenderSync()) {
				recordSync(particleRenderType, particle);
				continue;
			}
			float f3 = particleAddon.asyncparticles$isTicked() ? f : f2;
			try {
				particle.render(bufferBuilder, camera, f3);
			} catch (Throwable t) {
				onRenderingParticleException(particleRenderType, particle, t);
			}
		}
	}

	private void onRenderingParticleException(ParticleRenderType particleRenderType, Particle particle, Throwable t) {
		boolean tolerable = AsyncTickBehavior.INSTANCE.isTolerable(t);
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
					t.getClass().getName(),
					particle,
					AsyncParticlesClient.ISSUE_URL,
					t);
			}
			markAsSync(particleClass);
		}
		recordSync(particleRenderType, particle);
	}

	private Void renderAsyncExceptionally(Throwable e) {
		LOGGER.error("Error rendering particle", e);
		Minecraft mc1 = Minecraft.getInstance();
		if (mc1.level != null && mc1.player != null) {
			throw ExceptionUtil.toThrowDirectly(e);
		}
		return null;
	}

	public void endAll(PoseStack poseStack, float f, Camera camera, LightTexture lightTexture, boolean isAsync) {
//		if (!SimplePropertiesConfig.isRenderAsync()) { // Tested outside.
//			return;
//		}
//		if (isMixedParticleRendering()) { // Tested outside.
//			return;
//		}
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("particles");

		onTranslucent(mc);

		MultiBufferSource.BufferSource bufferSource = mc.levelRenderer.renderBuffers.bufferSource();
		ParticleEngine particleEngine = mc.particleEngine;
		if (ModListHelper.IRIS_LIKE_LOADED) {
			((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.EVERYTHING);
		}
		renderAsync = isAsync;
		particlePhase = true;
		particleEngine.render(poseStack, bufferSource, lightTexture, camera, f);
		renderAsync = false;
		particlePhase = false;

		postTranslucent(mc);
	}

	public boolean isIrisEarlyOpaquePhase() {
		return irisEarlyOpaquePhase;
	}

	public void irisOpaque(PoseStack poseStack, float f, Camera camera, LightTexture lightTexture, boolean isAsync) {
//		if (!SimplePropertiesConfig.isRenderAsync()) { // Tested outside.
//			return;
//		}
//		if (!isMixedParticleRendering()) { // Tested outside.
//			return;
//		}
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("particles");

		LevelRenderer levelRenderer = mc.levelRenderer;
		MultiBufferSource.BufferSource bufferSource = levelRenderer.renderBuffers.bufferSource();

		ParticleEngine particleEngine = mc.particleEngine;
		((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.OPAQUE);
		renderAsync = isAsync;
		particlePhase = true;
		particleEngine.render(poseStack, bufferSource, lightTexture, camera, f);
		renderAsync = false;
		particlePhase = false;
	}

	public void irisTranslucent(PoseStack poseStack, float f, Camera camera, LightTexture lightTexture, boolean isAsync) {
//		if (!SimplePropertiesConfig.isRenderAsync()) { // Tested outside.
//			return;
//		}
//		if (!isMixedParticleRendering()) { // Tested outside.
//			return;
//		}
		Minecraft mc = Minecraft.getInstance();
		mc.getProfiler().popPush("particles");
		LevelRenderer levelRenderer = mc.levelRenderer;
		MultiBufferSource.BufferSource bufferSource = levelRenderer.renderBuffers.bufferSource();

		onTranslucent(mc);

		ParticleEngine particleEngine = mc.particleEngine;
		((PhasedParticleEngine) particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.TRANSLUCENT);
		renderAsync = isAsync;
		particlePhase = true;
		particleEngine.render(poseStack, bufferSource, lightTexture, camera, f);
		renderAsync = false;
		particlePhase = false;

		postTranslucent(mc);
	}

	public void irisCustom(PoseStack poseStack, float f, Camera camera, LightTexture lightTexture) {
//		if (!isMixedParticleRendering()) { // Tested outside.
//			return;
//		}
		PoseStack poseStack2 = null;
		Minecraft mc = Minecraft.getInstance();
		ParticleEngine particleEngine = mc.particleEngine;
		Frustum frustum = this.getFrustum();
		ParticleCullingMode particleCullingMode = ConfigHelper.getParticleCullingMode();
		float f2 = f + 1f;
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
				particlePhase = true;
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
				ParticleAddon particleAddon = (ParticleAddon) particle;
				switch (particleCullingMode) {
					case AABB -> {
						if (particleAddon.shouldCull() &&
							!FrustumUtil.isVisible(frustum, particle.getBoundingBox())) {
							continue;
						}
					}
					case SPHERE -> {
						if (particleAddon.shouldCull() && !FrustumUtil.isVisible(frustum, particle)) {
							continue;
						}
					}
					case ASYNC_AABB, ASYNC_SPHERE -> {
						if (particleAddon.shouldCull() &&
							!particleAddon.asyncparticles$isVisibleOnScreen()) {
							continue;
						}
					}
				}
				if (!began) {
					particleRenderType.begin(bufferBuilder, particleEngine.textureManager);
					began = true;
				}
				float f3 = particleAddon.asyncparticles$isTicked() ? f : f2;
				try {
					particle.render(bufferBuilder, camera, f3);
				} catch (Throwable t) {
					throw constructCrashReport(particle, particleRenderType, t);
				}
			}
			if (began) {
				particleRenderType.end(tesselator);
			}
		}
		if (poseStack2 != null) {
			particlePhase = false;
			poseStack2.popPose();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.depthMask(true);
			RenderSystem.disableBlend();
			lightTexture.turnOffLightLayer();
		}
	}

	public boolean isRenderAsync() {
		return renderAsync;
	}

	public boolean isParticlePhase() {
		return particlePhase;
	}

	private void waitForAsyncTasks() {
		if (asyncTask != null) {
			asyncTask.join();
			asyncTask = null;
		}
	}

	public void tryWaitingForAsyncTasks() {
//		if (ConfigHelper.isRenderAsync() && stage != Stage.RENDERABLE) {
//			throw new IllegalStateException("Can only wait for async tasks around translucent");
//		}
		waitForAsyncTasks();
	}

	public ReportedException constructCrashReport(Particle particle, ParticleRenderType particleRenderType, Throwable t) {
		AsyncTickBehavior.INSTANCE.debugLater(LOGGER::info);
		AsyncTickBehavior.INSTANCE.tryDebug();
		debugLater(LOGGER::info);
		tryDebug();
		CrashReport crashReport = CrashReport.forThrowable(t, "Rendering Particle");
		CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being rendered");
		crashReportCategory.setDetail("Particle", particle::toString);
		crashReportCategory.setDetail("Particle Type", particleRenderType::toString);
		return new ReportedException(crashReport);
	}

	/* BufferBuilder */

	private final Map<ParticleRenderType, Pair<VertexFormat.Mode, VertexFormat>> FORMATS = new ConcurrentHashMap<>();
	public final Pair<VertexFormat.Mode, VertexFormat> EMPTY_FORMAT = Pair.of(null, null);
	private final Map<ParticleRenderType, BufferBuilder> BUFFER_BUILDERS = new IdentityHashMap<>();

	public BufferBuilder beginBufferBuilder(ParticleRenderType particleRenderType, TextureManager textureManager) {
		Pair<VertexFormat.Mode, VertexFormat> pair = getVertexFormatPair(particleRenderType, textureManager);
		if (pair == EMPTY_FORMAT) {
			return FakeBufferBuilder.INSTANCE;
		}
		BufferBuilder builder = BUFFER_BUILDERS.computeIfAbsent(particleRenderType,
			k -> new BufferBuilder(256)); // minimal size
		if (builder.building()) {
			return builder;
		}
		builder.begin(pair.first(), pair.second());
		return builder;
	}

	public @NotNull Pair<VertexFormat.Mode, VertexFormat> getVertexFormatPair(ParticleRenderType particleRenderType, TextureManager textureManager) {
		return FORMATS.computeIfAbsent(particleRenderType, k -> computeVertexFormatPair(k, textureManager));
	}

	@SuppressWarnings("ConstantValue")
	private @NotNull Pair<VertexFormat.Mode, VertexFormat> computeVertexFormatPair(ParticleRenderType k, TextureManager textureManager) {
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
			LOGGER.error("Exception try&store-ing vertex format/mode for particle render type: {}", k, e);
			RenderSystem.disableBlend();
			RenderSystem.depthMask(true);
			RenderSystem.enableDepthTest();
			RenderSystem.enableCull();
			RenderSystem.defaultBlendFunc();
		}
		VertexFormat.Mode mode = fakeBufferBuilder.getMode();
		VertexFormat format = fakeBufferBuilder.getFormat();
		if (mode != null && format != null) {
			return Pair.of(mode, format);
		}
		return EMPTY_FORMAT;
	}

	/* Sync Rendering */

	private final Map<ParticleRenderType, Set<Particle>> SYNC_PARTICLES = Collections.synchronizedMap(new IdentityHashMap<>());

	public void markAsSync(Class<? extends Particle> aClass) {
		synchronized (syncParticleTypes) {
			syncParticleTypes.add(aClass);
		}
	}

	public boolean shouldSync(Class<?> aClass) {
		return syncParticleTypes.contains(aClass);
	}

	public void recordSync(ParticleRenderType particleRenderType, Particle particle) {
		Set<Particle> particles = SYNC_PARTICLES.computeIfAbsent(particleRenderType,
			k -> Collections.newSetFromMap(new IdentityHashMap<>()));
		synchronized (particles) {
			particles.add(particle);
		}
	}

	public Set<Particle> getSync(ParticleRenderType particleRenderType) {
		Set<Particle> set = SYNC_PARTICLES.get(particleRenderType);
		return set == null ? Collections.emptySet() : set;
	}

	private void clearSync() {
		if (!SYNC_PARTICLES.isEmpty()) {
			SYNC_PARTICLES.clear();
		}
	}

	/* Debug */

	public void debugLater(Consumer<String> consumer) {
		debugConsumer = consumer;
	}

	public void tryDebug() {
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
				iris particle mode: %s
				glCapabilities: TransformFeedback: %s,
				                ExplicitAttribLocation: %s,
				                ComputeShader: %s""" // TODO
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
					syncParticleTypes.stream().map(Class::getName).toList(),
					FORMATS.entrySet().stream()
						.filter(e -> e.getValue() == EMPTY_FORMAT)
						.map(Map.Entry::getKey).toList(),
					switch (getMode()) {
						case SYNC -> "SYNC";
						case DELAYED_ASYNC -> "DELAYED_ASYNC";
						case BEFORE_SYNC -> "BEFORE_SYNC";
						case COMPATIBILITY_ASYNC -> "COMPATIBILITY_ASYNC";
						case MIXED_SYNC -> "MIXED_SYNC";
						case BEFORE_ASYNC -> "BEFORE_ASYNC";
						case MIXED_ASYNC -> "MIXED_ASYNC";
						default -> "UNKNOWN";
					},
					ModListHelper.IRIS_LIKE_LOADED && IrisApi.getInstance().isShaderPackInUse()
						? Optional.ofNullable(IrisCompat.getParticleRenderingSettings())
						.map(ParticleRenderingSettings::name)
						.orElse("disabled") : "disabled",
					GLCaps.tfSupport.getClass().getSimpleName(),
					GLCaps.supportsExplicitAttribLocation,
					GLCaps.csSupport.getClass().getSimpleName()));
			debugConsumer = null;
		}
	}

	/* Destroy */

	public void reset() {
		irisEarlyOpaquePhase = false;
		renderAsync = false;
		particlePhase = false;
		waitForAsyncTasks();
		FORMATS.clear();
		clearSync();
	}

	public void onTranslucent(Minecraft mc) {
		if (mc.levelRenderer.transparencyChain != null) {
			RenderTarget particlesTarget = mc.levelRenderer.getParticlesTarget();
			particlesTarget.clear(Minecraft.ON_OSX);
			particlesTarget.copyDepthFrom(mc.getMainRenderTarget());
			RenderStateShard.PARTICLES_TARGET.setupRenderState();
		}

	}

	public void postTranslucent(Minecraft mc) {
		if (mc.levelRenderer.transparencyChain != null) {
			RenderStateShard.PARTICLES_TARGET.clearRenderState();
		}
	}

	public @NotNull Frustum getFrustum() {
		return frustum;
	}

	public void setFrustum(@NotNull Frustum frustum) {
		this.frustum = frustum;
	}

	public class AsyncRendererThread extends AsyncParticleWorkerThread {
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

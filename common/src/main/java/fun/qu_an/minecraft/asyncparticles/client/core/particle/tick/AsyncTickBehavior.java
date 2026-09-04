package fun.qu_an.minecraft.asyncparticles.client.core.particle.tick;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.DevRuntimeDebug;
import fun.qu_an.minecraft.asyncparticles.client.config.ParticleCleanupStrategy;
import fun.qu_an.minecraft.asyncparticles.client.core.Phase;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.Backends;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.ParticleHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.TaskHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeEvictingQueue;
import fun.qu_an.minecraft.asyncparticles.client.util.ParticleThreadLocal;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AsyncTickBehavior {
	static final Logger LOGGER = LogManager.getLogger();
	public static final ParticleThreadLocal<Phase> PHASE = ParticleThreadLocal.withInitial(ThreadUtil::isOnMainThread, () -> Phase.NONE);
	public static final int THREADS;

	static {
		CentralProcessor cpu = new SystemInfo().getHardware().getProcessor();
		int logical = Math.min(Runtime.getRuntime().availableProcessors(), cpu.getLogicalProcessorCount());
		int physical = Math.min(cpu.getPhysicalProcessorCount(), logical);
		if (physical <= 2) {
			THREADS = 1;
		} else if (logical > physical) {
			THREADS = Mth.clamp(logical - Math.max(logical / physical, 2), 2, 6);
		} else {
			THREADS = Mth.clamp(logical - 1, 2, 6);
		}
	}

	public static final String THREAD_PREFIX = "AsyncParticleTickWorker";
	private static final AsyncTickBehavior INSTANCE = new AsyncTickBehavior();

	private final ForkJoinPool EXECUTOR;
	private Consumer<String> debugConsumer;
	private boolean particlePhase;
	private final AtomicLong timeUsageNano = new AtomicLong(0L);
	private volatile LevelBundle levelBundle;

	{
		AtomicInteger workerCount = new AtomicInteger(1);
		EXECUTOR = new ForkJoinPool(THREADS, (forkJoinPool) -> {
			ForkJoinWorkerThread forkJoinWorkerThread = new AsyncTickerThread(forkJoinPool);
			forkJoinWorkerThread.setName(THREAD_PREFIX + "-" + workerCount.getAndIncrement());
			forkJoinWorkerThread.setDaemon(true);
			return forkJoinWorkerThread;
		}, Util::onThreadException, true);
	}

	private final TaskHelper tickTaskHelper = new TaskHelper(EXECUTOR);
	private final TaskHelper cleanupTaskHelper = new TaskHelper(EXECUTOR);
	private final TickExceptionHandler exceptionHandler = new TickExceptionHandler(this);
	private boolean reloadLater;
	private boolean isTailTick;

	private final Set<Class<?>> syncAnimationTypes = new ReferenceOpenHashSet<>();
	private final Set<Class<?>> syncParticleTypes = new ReferenceOpenHashSet<>();
	private final Map<ParticleRenderType, Set<Particle>> syncParticles = new Reference2ReferenceOpenHashMap<>();
	private final Map<ParticleRenderType, Set<TextureSheetParticle>> syncGpuParticles = new Reference2ReferenceOpenHashMap<>();

	public static AsyncTickBehavior getInstance() {
		return INSTANCE;
	}

	public void ensureLevelRunning(Runnable r, Consumer<Exception> exceptionHandler) {
		LevelBundle levelBundle = getLevelBundle();
		if (levelBundle == null || levelBundle.isLevelReset()) {
			return;
		}
		try {
			r.run();
		} catch (Exception e) {
			if (!levelBundle.isLevelReset()) {
				exceptionHandler.accept(e);
			}
		}
	}

	public void ensureLevelRunning(Runnable r, Consumer<Exception> exceptionHandler, Phase phase) {
		LevelBundle levelBundle = getLevelBundle();
		if (levelBundle == null || levelBundle.isLevelReset()) {
			return;
		}
		PHASE.set(phase);
		try {
			r.run();
		} catch (Exception e) {
			if (!levelBundle.isLevelReset()) {
				exceptionHandler.accept(e);
			}
		} finally {
			PHASE.set(Phase.NONE);
		}
	}

	public void addTaskEnsureLevelRunning(Runnable r, Consumer<Exception> exceptionHandler) {
		getTickTaskManager().addTask(() -> ensureLevelRunning(r, exceptionHandler));
	}

	public void addTaskEnsureLevelRunning(Runnable r, Consumer<Exception> exceptionHandler, @NotNull Phase phase) {
		getTickTaskManager().addTask(() -> ensureLevelRunning(r, exceptionHandler, phase));
	}

	public boolean shouldRemove(Particle particle) {
		if (!particle.isAlive()) {
			return true;
		}
		ParticleAddon particleAddon = (ParticleAddon) particle;
		if (ConfigHelper.isAsyncParticleTick() && particleAddon.asyncparticles$isTicked()) {
			particleAddon.asyncparticles$resetTicked();
		}
		return false;
	}

	public void preTick(boolean isTailTick) {
		tickTaskHelper.waitForCompletion(exceptionHandler::tickExceptionally);
		this.isTailTick = isTailTick;
		if (ConfigHelper.isAsyncParticleTick()) {
			if (!LevelBundle.isLevelRunning()) {
				return;
			}
			if (ConfigHelper.getParticleCleanupStrategy() == ParticleCleanupStrategy.PARALLEL_WITH_TICK) {
				if (cleanupTaskHelper.isRunning()) {
					cleanupTaskHelper.waitForCompletion(ExceptionUtil::toThrowDirectly);
				}
				prepareCleanupTasks(cleanupTaskHelper);
				cleanupTaskHelper.submitAll();
			}
		}
	}

	public void prepareCleanupTasks(TaskHelper taskHelper) {
		Minecraft mc = Minecraft.getInstance();
		if (!ConfigHelper.isGpuOnlyAsyncParticleTick()) {
			Collection<Queue<Particle>> queues = mc.particleEngine.particles.values();
			for (Queue<Particle> queue : queues) {
				if (!queues.isEmpty()) {
					cleanupTaskHelper.addTask(() -> doParticlesRemoveIf(queue));
				}
			}
		}
		if (ConfigHelper.isGpuParticles()) {
			Collection<Queue<TextureSheetParticle>> gpuQueues = GpuParticleBehavior.getInstance().gpuParticles.values();
			for (Queue<TextureSheetParticle> queue : gpuQueues) {
				if (!gpuQueues.isEmpty()) {
					cleanupTaskHelper.addTask(() -> doParticlesRemoveIf(queue));
				}
			}
		}
		Queue<TrackingEmitter> trackingEmitters = mc.particleEngine.trackingEmitters;
		if (!trackingEmitters.isEmpty()) {
			taskHelper.addTask(() -> doEmittersRemoveIf(trackingEmitters));
		}
		taskHelper.groupTasks(true);
	}

	public void doEmittersRemoveIf(Queue<? extends TrackingEmitter> queue) {
		if (queue.isEmpty()) {
			return;
		}
		doRemoveIf(queue, p -> !p.isAlive());
	}

	public void doParticlesRemoveIf(Collection<? extends Particle> particles) {
		if (particles.isEmpty()) {
			return;
		}
		doRemoveIf(particles, this::shouldRemove);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public void doRemoveIf(Collection<? extends Particle> particles, Predicate<? super Particle> shouldRemove) {
		if (ConfigHelper.isParallelQueueRemoval() && particles instanceof IterationSafeEvictingQueue<?> iseq) {
			iseq.parallelRemoveIf((Predicate) shouldRemove,
				ConfigHelper.isParallelQueueEviction(),
				AsyncTickBehavior.THREADS,
				EXECUTOR);
		} else {
			particles.removeIf(shouldRemove);
		}
	}

	public void postTick() {
		cleanupTaskHelper.waitForCompletion(ExceptionUtil::toThrowDirectly);
		tryReload();
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		LocalPlayer player = mc.player;
		Entity cameraEntity = mc.getCameraEntity();
		boolean levelAvailable = level != null && player != null && cameraEntity != null;
		boolean levelRunning = levelAvailable && !mc.isPaused();
		if (!levelRunning || !isTailTick()) {
			Queue<Particle> particlesToAdd = mc.particleEngine.particlesToAdd;
			if (!particlesToAdd.isEmpty()) {
				particlesToAdd.forEach(ParticleHelper::onEvictIgnoreExceptions);
				particlesToAdd.clear();
			}
			if (!levelAvailable) {
				tickTaskHelper.disposeTasks();
				return;
			}
		} else {
			tryDebug();
			tickTaskHelper.groupTasks(false);
			if (ConfigHelper.isAsyncParticleTick()) {
				ParticleCleanupStrategy cleanupStrategy = ConfigHelper.getParticleCleanupStrategy();
				if (cleanupStrategy == ParticleCleanupStrategy.BLOCK_MAIN_THREAD) {
					prepareCleanupTasks(cleanupTaskHelper);
					cleanupTaskHelper.submitAll();
					cleanupTaskHelper.waitForCompletion(ExceptionUtil::toThrowDirectly);
				} else if (cleanupStrategy == ParticleCleanupStrategy.MAIN_THREAD) {
					if (!ConfigHelper.isGpuOnlyAsyncParticleTick()) {
						Collection<Queue<Particle>> queues = mc.particleEngine.particles.values();
						for (Queue<Particle> queue : queues) {
							if (!queues.isEmpty()) {
								doParticlesRemoveIf(queue);
							}
						}
					}
					if (ConfigHelper.isGpuParticles()) {
						Collection<Queue<TextureSheetParticle>> gpuQueues = GpuParticleBehavior.getInstance().gpuParticles.values();
						for (Queue<TextureSheetParticle> queue : gpuQueues) {
							if (!gpuQueues.isEmpty()) {
								doParticlesRemoveIf(queue);
							}
						}
					}
					Queue<TrackingEmitter> trackingEmitters = mc.particleEngine.trackingEmitters;
					if (!trackingEmitters.isEmpty()) {
						doEmittersRemoveIf(trackingEmitters);
					}
				}
				particlePhase = true;
				mc.particleEngine.tick();
				particlePhase = false;
			}
		}
		tickTaskHelper.submitAll(() -> {
			timeUsageNano.setRelease(System.nanoTime());
			levelBundle = new LevelBundle(level, player, cameraEntity);
		}, () -> {
			levelBundle = null;
			timeUsageNano.setRelease(System.nanoTime() - timeUsageNano.getAcquire());
		}, e -> {
			LevelBundle levelBundle = getLevelBundle();
			if (levelBundle != null && !levelBundle.isLevelReset()) {
				throw ExceptionUtil.toThrowDirectly(e);
			}
			// else level reset
		});
	}

	public void reloadLater() {
		reloadLater = true;
	}

	private void tryReload() {
		if (reloadLater) {
			reload();
			reloadLater = false;
		}
	}

	public void reload() { // Redirect to clearParticles
		Minecraft.getInstance().particleEngine.clearParticles();
	}

	public void reset() {
		if (tickTaskHelper.isRunning()) {
			tickTaskHelper.waitForCompletion(exceptionHandler::tickExceptionally);
		}
		tickTaskHelper.disposeTasks();
		if (cleanupTaskHelper.isRunning()) {
			cleanupTaskHelper.waitForCompletion(ExceptionUtil::toThrowDirectly);
		}
		cleanupTaskHelper.disposeTasks();
		syncAnimationTypes.clear();
		syncAnimationTypes.addAll(ConfigHelper.getSyncAnimationClassesTick());
		syncParticleTypes.clear();
		syncParticleTypes.addAll(ConfigHelper.getSyncParticleClassesTick());
	}

	public void debugLater(Consumer<String> consumer) {
		debugConsumer = consumer;
	}

	public void tryDebug() {
		if (debugConsumer == null) {
			return;
		}
		debugConsumer.accept(String.format("""
			[AsyncParticles Debug]
			last tick duration: %.3f ms,
			particle task count: %d,
			particle limit: %d,
			particles queues (render order, size): %s,
			GPU particles queues (render order, size): %s,
			particles to add size: %d
			sync particle types: %s,
			Backend: %s"""
			.formatted(
				timeUsageNano.getAcquire() / 1000000d,
				tickTaskHelper.taskCount(),
				ConfigHelper.getParticleLimit(),
				Minecraft.getInstance().particleEngine.particles.entrySet()
					.stream().map(e -> {
						Queue<Particle> queue = e.getValue();
						return e.getKey() + ": " + queue.size() + "/" + ((IterationSafeEvictingQueue<Particle>) queue).arraySize();
					}).toList(),
				GpuParticleBehavior.getInstance().gpuParticles.entrySet()
					.stream().map(e -> {
						Queue<TextureSheetParticle> queue = e.getValue();
						return e.getKey() + ": " + queue.size() + "/" + ((IterationSafeEvictingQueue<TextureSheetParticle>) queue).arraySize();
					}).toList(),
				Minecraft.getInstance().particleEngine.particlesToAdd.size(),
				syncParticleTypes.stream().map(Class::getName).toList(),
				Backends.debugInfo()
			)));

		debugConsumer = null;
	}

	public boolean shouldSync(Class<? extends Particle> aClass) {
		return syncParticleTypes.contains(aClass) || DevRuntimeDebug.isSyncAllParticles();
	}

	public boolean shouldSyncAnimateTick(Block block) {
		return syncAnimationTypes.contains(block.getClass());
	}

	public boolean shouldSyncRenderType(Class<? extends ParticleRenderType> aClass) {
		return syncParticleTypes.contains(aClass);
	}

	public boolean isTailTick() {
		return isTailTick;
	}

	public ExecutorService getExecutor() {
		return tickTaskHelper.executor();
	}

	public TaskHelper getTickTaskManager() {
		return tickTaskHelper;
	}

	public TaskHelper getCleanupTaskManager() {
		return cleanupTaskHelper;
	}

	public boolean isParticlePhase() {
		return particlePhase;
	}

	public void dumpParticles() {
		ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
		LOGGER.info((ModListHelper.IS_FORGE ? particleEngine.particles.keySet() : ParticleEngine.RENDER_ORDER)
			.stream()
			.collect(Collectors.toMap(ParticleRenderType::getClass, Object::toString)));
		LOGGER.info(particleEngine.particles);
		LOGGER.info(GpuParticleBehavior.getInstance().gpuParticles);
	}

	public LevelBundle getLevelBundle() {
		return levelBundle;
	}

	public TickExceptionHandler getExceptionHandler() {
		return exceptionHandler;
	}

	public Set<Particle> getSyncParticles(ParticleRenderType renderType) {
		return syncParticles.computeIfAbsent(renderType, k -> new ReferenceOpenHashSet<>());
	}

	public boolean isSyncParticle(Particle particle) {
		Set<Particle> particles;
		Set<TextureSheetParticle> gpuParticles;
		ParticleRenderType renderType = particle.getRenderType();
		return ((particles = syncParticles.get(renderType)) != null && particles.contains(particle))
			|| (particle instanceof TextureSheetParticle tsp && ((gpuParticles = syncGpuParticles.get(renderType)) != null && gpuParticles.contains(tsp)));
	}

	@SuppressWarnings("unchecked")
	public <T extends Particle> Set<T> getSyncGpuParticles(ParticleRenderType renderType) {
		return (Set<T>) syncGpuParticles.computeIfAbsent(renderType, k -> new ReferenceOpenHashSet<>());
	}
}

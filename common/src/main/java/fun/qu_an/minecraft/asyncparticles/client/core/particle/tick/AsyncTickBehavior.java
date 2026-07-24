package fun.qu_an.minecraft.asyncparticles.client.core.particle.tick;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleGroupAddition;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.Backends;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.TaskHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeEvictingQueue;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	public static final int THREADS = Mth.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, 6);
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

	private final Set<Class<?>> syncParticleTypes = new ReferenceOpenHashSet<>();

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

	public void addTaskEnsureLevelRunning(Runnable r, Consumer<Exception> exceptionHandler) {
		getTickTaskManager().addTask(() -> ensureLevelRunning(r, exceptionHandler));
	}

	public boolean shouldRemove(Particle particle) {
		if (!particle.isAlive()) {
			return true;
		}
		ParticleAddon particleAddon = (ParticleAddon) particle;
		if (ConfigHelper.isAsyncTickParticle() && particleAddon.asyncparticles$isTicked()) {
			particleAddon.asyncparticles$resetTicked();
			return false;
		}
		return ConfigHelper.isRemoveIfMissedTick();
	}

	public boolean isCancelled() {
		return false;
	}

	public void preTick(boolean isHeadTick, boolean isTailTick) {
		if (isHeadTick) {
			tickTaskHelper.waitForCompletion(exceptionHandler::tickExceptionally);
		}
		this.isTailTick = isTailTick;
		if (!ConfigHelper.isAsyncTickParticle()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		boolean levelRunning = mc.level != null && mc.player != null && !mc.isPaused();
		if (!levelRunning) {
			return;
		}
		if (cleanupTaskHelper.isRunning()) {
			throw new IllegalStateException("cleanupFuture is not null!");
		}
		Collection<ParticleGroup<?>> groups = mc.particleEngine.particles.values();
		for (ParticleGroup<?> group : groups) {
			if (!groups.isEmpty()) {
				cleanupTaskHelper.addTask(((ParticleGroupAddition) group)::asyncparticles$removeDeadParticles);
			}
		}
		cleanupTaskHelper.addTask(() -> {
			Queue<TrackingEmitter> trackingEmitters = Minecraft.getInstance().particleEngine.trackingEmitters;
			doEmittersRemoveIf(trackingEmitters);
		});
		cleanupTaskHelper.groupTasks(true);
		cleanupTaskHelper.submitAll();
	}

	public void doEmittersRemoveIf(Queue<? extends TrackingEmitter> queue) {
		if (queue.isEmpty()) {
			return;
		}
		doRemoveIf(queue, p -> !p.isAlive());
	}

	public void doParticlesRemoveIf(Queue<? extends Particle> particles) {
		if (particles.isEmpty()) {
			return;
		}
		ParticleEngine engine = Minecraft.getInstance().particleEngine;
		doRemoveIf(particles, p -> {
			boolean b = shouldRemove(p);
			if (b) {
				p.getParticleLimit().ifPresent(options -> engine.updateCount(options, -1));
			}
			return b;
		});
	}

	public void doRemoveIf(Queue<? extends Particle> particles, Predicate<? super Particle> shouldRemove) {
		if (ConfigHelper.isParallelQueueRemoval()) {
			((IterationSafeEvictingQueue<? extends Particle>) particles)
				.parallelRemoveIf(shouldRemove,
					ConfigHelper.isParallelQueueEviction(),
					AsyncTickBehavior.THREADS,
					tickTaskHelper.executor());
		} else {
			particles.removeIf(shouldRemove);
		}
	}

	public void postTick() {
		cleanupTaskHelper.waitForCompletion(ExceptionUtil::toThrowDirectly);
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		LocalPlayer player = mc.player;
		Entity cameraEntity = mc.getCameraEntity();
		boolean levelRunning = level != null && player != null && cameraEntity != null && !mc.isPaused();
		if (!ConfigHelper.isAsyncTickParticle()) {
			tryReload();
			tryDebug();
			if (levelRunning) {
				tickTaskHelper.runAllTasks();
			} else {
				tickTaskHelper.disposeTasks();
				mc.particleEngine.particlesToAdd.clear();
			}
			return;
		}
		if (!levelRunning || !isTailTick) {
			tickTaskHelper.disposeTasks();
			mc.particleEngine.particlesToAdd.clear();
			return;
		}
		tickTaskHelper.groupTasks(false);
		particlePhase = true;
		mc.particleEngine.tick();
		particlePhase = false;
		tryReload();
		tryDebug();
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

	private void reload() { // Redirect to clearParticles
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
		syncParticleTypes.clear();
		syncParticleTypes.addAll(ConfigHelper.getSyncParticleClassesTick());
	}

	public void debugLater(Consumer<String> consumer) {
		debugConsumer = consumer;
	}

	void tryDebug() {
		if (debugConsumer == null) {
			return;
		}
		debugConsumer.accept(String.format("""
			[AsyncParticles Debug]
			last tick duration: %.3f ms,
			particle task count: %d,
			particle limit: %d,
			particles groups (render order, size):
			%s,
			particles to add size: %d
			sync particle types: %s,
			Backend: %s"""
			.formatted(
				ConfigHelper.isAsyncTickParticle() ? timeUsageNano.getAcquire() / 1000000d : Double.NaN,
				tickTaskHelper.taskCount(),
				ConfigHelper.getParticleLimit(),
				Minecraft.getInstance().particleEngine.particles.entrySet()
					.stream()
					.collect(Collectors.toMap(Map.Entry::getKey, e -> {
						ParticleGroup<?> group = e.getValue();
						String s = "{total: " + group.size();
						if (group instanceof GpuParticleGroup) {
							s += ", GPU: " + ((GpuParticleGroup) group).asyncparticles$getGpuParticles().size();
						}
						s += "}";
						return s;
					})),
				Minecraft.getInstance().particleEngine.particlesToAdd.size(),
				syncParticleTypes.stream().map(Class::getName).toList(),
				Backends.debugInfo()
			)));

		debugConsumer = null;
	}

	public boolean shouldSync(Class<?> aClass) {
		if (ModListHelper.DEV_ENV && ConfigHelper.isSyncAllParticles()) {
			return true;
		}
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

	private boolean isParticlePhase() {
		return particlePhase;
	}

	public boolean shouldTickParticleEngine() {
		if (isParticlePhase() || !ConfigHelper.isAsyncTickParticle()) {
			return true;
		}
		throw new IllegalStateException("ParticleEngine.tick() called outside the particle phase unexpectedly.");
	}

	public void dumpParticles() {
		ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
		for (Map.Entry<ParticleRenderType, ParticleGroup<?>> entry : particleEngine.particles.entrySet()) {
			ParticleRenderType particleRenderType = entry.getKey();
			ParticleGroup<?> value = entry.getValue();
			LOGGER.info("Particle group: {}, size: {}, particles: ", value, value.size());
			for (Particle particle : value.particles) {
				LOGGER.info("{{}, {}}", particle, particle.getClass());
			}
			if (value instanceof GpuParticleGroup gpuGroup) {
				LOGGER.info("GPU particles: ");
				for (SingleQuadParticle particle : gpuGroup.asyncparticles$getGpuParticles()) {
					LOGGER.info("{{}, {}}", particle, particle.getClass());
				}
			}
		}
	}

	public LevelBundle getLevelBundle() {
		return levelBundle;
	}

	public TickExceptionHandler getExceptionHandler() {
		return exceptionHandler;
	}
}

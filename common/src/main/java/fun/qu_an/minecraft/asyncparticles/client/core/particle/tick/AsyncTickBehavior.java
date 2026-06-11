package fun.qu_an.minecraft.asyncparticles.client.core.particle.tick;

import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleGroupAddition;
import fun.qu_an.minecraft.asyncparticles.client.compat.GLCaps;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.TaskManager;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionTracker;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeEvictingQueue;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AsyncTickBehavior {
	static final Logger LOGGER = LogManager.getLogger();
	public static final int THREADS = Mth.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, 6);
	public static final String THREAD_PREFIX = "AsyncParticleTickWorker";
	private static final AsyncTickBehavior INSTANCE = new AsyncTickBehavior();

	private final ForkJoinPool EXECUTOR;
	private Consumer<String> debugConsumer;

	{
		AtomicInteger workerCount = new AtomicInteger(1);
		EXECUTOR = new ForkJoinPool(THREADS, (forkJoinPool) -> {
			ForkJoinWorkerThread forkJoinWorkerThread = new AsyncTickerThread(forkJoinPool);
			forkJoinWorkerThread.setName(THREAD_PREFIX + "-" + workerCount.getAndIncrement());
			forkJoinWorkerThread.setDaemon(true);
			return forkJoinWorkerThread;
		}, Util::onThreadException, true);
	}

	private final TaskManager tickTaskManager = new TaskManager(EXECUTOR, e -> {
		throw new RuntimeException(e);
	});
	private final TaskManager cleanupTaskManager = new TaskManager(EXECUTOR, e -> {
		throw new RuntimeException(e);
	});
	private boolean reloadLater;
	private boolean isTailTick;

	private final ExceptionTracker<Object> EXCEPTION_TRACKER = new ExceptionTracker<>(
		() -> 5000,
		ConfigHelper::getTickFailurePerSecondThreshold
	);
	private final Set<Class<?>> syncParticleTypes = new ReferenceOpenHashSet<>();

	public static AsyncTickBehavior getInstance() {
		return INSTANCE;
	}

	public <T extends Particle> void onEvict(T particle) {
		particle.getParticleLimit().ifPresent(limit -> Minecraft.getInstance().particleEngine.updateCount(limit, -1));
		if (particle.isAlive()) {
			particle.remove();
		}
	}

	public void doEmittersRemoveIf(Queue<? extends TrackingEmitter> queue) {
		if (ConfigHelper.isParallelQueueRemoval()) {
			((IterationSafeEvictingQueue<? extends TrackingEmitter>) queue)
				.parallelRemoveIf(particle -> !particle.isAlive(),
					ConfigHelper.isParallelQueueEviction(),
					AsyncTickBehavior.THREADS,
					tickTaskManager.executor());
		} else {
			queue.removeIf(this::shouldRemove);
		}
	}

	public boolean shouldRemove(Particle particle) {
		if (!particle.isAlive()) {
			return true;
		}
		ParticleAddon particleAddon = (ParticleAddon) particle;
		if (particleAddon.asyncparticles$isTickSync()) {
			return false;
		}
		if (particleAddon.asyncparticles$isTicked()) {
			particleAddon.asyncparticles$resetTicked();
			return false;
		}
		return ConfigHelper.isRemoveIfMissedTick();
	}

	public boolean isCancelled() {
		return false;
	}

	public boolean isTolerable(@NotNull Throwable e) {
		if (!(e instanceof Exception)) {
			return false;
		}
		Throwable rootCause = ExceptionUtil.getRootCause(e);
		return rootCause instanceof MissingPaletteEntryException
			|| rootCause instanceof NullPointerException
			|| rootCause instanceof IndexOutOfBoundsException
			|| rootCause instanceof ArrayIndexOutOfBoundsException
			|| (rootCause instanceof ConcurrentModificationException && ConfigHelper.suppressCME());
	}

	public ReportedException onTickParticleException(Particle particle, Throwable t) {
		if (ThreadUtil.isOnMainThread()) {
			return constructCrashReport(particle, t);
		}
		boolean tolerable = isTolerable(t);
		Class<? extends Particle> particleClass = ((ParticleAddon) particle).asyncparticles$getRealClass();
		if (tolerable && !EXCEPTION_TRACKER.addException(particleClass, t)) {
			return null;
		}
//		if (ConfigHelper.markSyncIfTickFailed()) {
//			((ParticleAddon) particle).asyncparticles$setTickSync();
//			if (!shouldSync(particleClass)) {
//				if (!tolerable) {
//					LOGGER.warn("Exception while ticking particle {}, marking as sync", particle, t);
//				} else {
//					LOGGER.warn("Exception {} thrown while ticking particle {} exceeds the threshold, please contact the author: {}",
//						t.getClass().getName(),
//						particle,
//						AsyncParticlesClient.ISSUE_URI,
//						t);
//				}
//				markAsSync(particleClass);
//			}
//			recordSync(particle);
//		} else
		if (tolerable) {
			return constructCrashReport(particle, new RuntimeException(
				"Exception %s thrown while ticking particle %s, exceeds the threshold, please contact the author: %s"
					.formatted(
						t.getClass().getName(),
						particle,
						AsyncParticlesClient.ISSUE_URI),
				t));
		} else {
			return constructCrashReport(particle, t);
		}
	}

	public ReportedException constructCrashReport(Particle particle, Throwable t) {
		while (t instanceof CompletionException || t instanceof ExecutionException) {
			t = t.getCause();
		}
		if (t instanceof ReportedException re) {
			return re;
		}
//		debugLater(LOGGER::info);
//		tryDebug();
//		AsyncRenderBehavior.getInstance().debugLater(LOGGER::info);
//		AsyncRenderBehavior.getInstance().tryDebug();
		CrashReport crashReport = CrashReport.forThrowable(t, "Ticking Particle");
		CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being ticked");
		crashReportCategory.setDetail("Particle", particle::toString);
		crashReportCategory.setDetail("Particle Type", particle.getGroup()::toString);
		return new ReportedException(crashReport);
	}

	public void preTick(boolean isHeadTick) {
		if (isHeadTick) {
			tickTaskManager.waitForCompletion();
		}
		if (!ConfigHelper.isTickAsync()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		boolean levelRunning = mc.level != null && mc.player != null && !mc.isPaused();
		if (levelRunning) {
			if (cleanupTaskManager.isRunning()) {
				throw new IllegalStateException("cleanupFuture is not null!");
			}
			Collection<ParticleGroup<?>> groups = mc.particleEngine.particles.values();
			for (ParticleGroup<?> group : groups) {
				if (!groups.isEmpty()) {
					cleanupTaskManager.submitImmediately(((ParticleGroupAddition) group)::asyncparticles$removeDeadParticles);
				}
			}
		}
	}

	public void postTick(boolean isTailTick) {
		cleanupTaskManager.waitForCompletion();
		Minecraft mc = Minecraft.getInstance();
		boolean levelRunning = mc.level != null && mc.player != null && !mc.isPaused();
		if (!ConfigHelper.isTickAsync()) {
			tryReload();
			if (levelRunning) {
				tickTaskManager.runAllTasks();
			}
			return;
		}
		if (!levelRunning) {
			return;
		}
		mc.particleEngine.tick();
		if (!isTailTick) {
			this.isTailTick = false;
			tickTaskManager.runAllTasks();
		} else {
			this.isTailTick = true;
			tryReload();
			tryDebug();
			tickTaskManager.submitAllSequentially();
		}
	}

	public void dispatch(Runnable tickParticles) {
		tickTaskManager.addTask(tickParticles);
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
		tickTaskManager.waitForCompletion();
		tickTaskManager.disposeTasks();
		cleanupTaskManager.waitForCompletion();
		cleanupTaskManager.disposeTasks();
		syncParticleTypes.clear();
		syncParticleTypes.addAll(ConfigHelper.getSyncParticleClassesTick());
	}

	public void debugLater(Consumer<String> consumer) {
		debugConsumer = consumer;
	}

	private void tryDebug() {
		if (debugConsumer == null) {
			return;
		}
		debugConsumer.accept(String.format("""
			[AsyncParticles Debug]
			particle task count: %d,
			particle limit: %d,
			particles groups (render order, size/allocated):
			%s,
			particles to add size: %d
			sync particle types: %s,
			glCapabilities: TransformFeedback: %s,
			                ExplicitAttribLocation: %s"""
			.formatted(
				tickTaskManager.taskCount(),
				ConfigHelper.getParticleLimit(),
				Minecraft.getInstance().particleEngine.particles.entrySet()
					.stream().filter(entry -> entry.getValue() instanceof QuadParticleGroup)
					.collect(Collectors.toMap(Map.Entry::getKey, e -> {
						Queue<?> queue = e.getValue().getAll();
						if (queue instanceof IterationSafeEvictingQueue<?> q1) {
							return queue.size() + "/" + q1.arraySize();
						}
						return "Unsupported Queue";
					})),
				Minecraft.getInstance().particleEngine.particlesToAdd.size(),
				syncParticleTypes.stream().map(Class::getName).toList(),
				GLCaps.tfSupport.getClass().getSimpleName(),
				GLCaps.supportsExplicitAttribLocation
			)));

		debugConsumer = null;
	}

	public boolean shouldSync(Class<?> aClass) {
		return syncParticleTypes.contains(aClass);
	}

	public boolean isTailTick() {
		return isTailTick;
	}

	public ExecutorService getExecutor() {
		return tickTaskManager.executor();
	}

	public TaskManager getTickTaskManager() {
		return tickTaskManager;
	}

	public TaskManager getCleanupTaskManager() {
		return cleanupTaskManager;
	}
}

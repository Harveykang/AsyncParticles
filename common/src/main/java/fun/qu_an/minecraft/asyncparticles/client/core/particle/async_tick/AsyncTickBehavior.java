package fun.qu_an.minecraft.asyncparticles.client.core.particle.async_tick;

import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleGroupAddition;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.TaskManager;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_render.AsyncRenderBehavior;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionTracker;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeEvictingQueue;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSets;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncTickBehavior {
	static final Logger LOGGER = LogManager.getLogger();
	public static final int THREADS = Mth.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, 6);
	public static final String THREAD_PREFIX = "AsyncParticleTickWorker";
	private static final AsyncTickBehavior INSTANCE = new AsyncTickBehavior();

	private final ForkJoinPool EXECUTOR;

	{
		AtomicInteger workerCount = new AtomicInteger(1);
		EXECUTOR = new ForkJoinPool(THREADS, (forkJoinPool) -> {
			ForkJoinWorkerThread forkJoinWorkerThread = new AsyncTickerThread(forkJoinPool);
			forkJoinWorkerThread.setName(THREAD_PREFIX + "-" + workerCount.getAndIncrement());
			forkJoinWorkerThread.setDaemon(true);
			return forkJoinWorkerThread;
		}, Util::onThreadException, true);
	}

	private final TaskManager tickManager = new TaskManager(EXECUTOR, e -> {
		throw new RuntimeException(e);
	});
	private final TaskManager cleanupManager = new TaskManager(EXECUTOR, e -> {
		throw new RuntimeException(e);
	});
	private boolean reloadLater;
	private boolean isTailTick;

	private final ExceptionTracker<Object> EXCEPTION_TRACKER = new ExceptionTracker<>(
		() -> 5000,
		ConfigHelper::getTickFailurePerSecondThreshold
	);
	private final Set<Particle> syncParticles = ReferenceSets.synchronize(new ReferenceOpenHashSet<>());

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
					tickManager.executor());
		} else {
			queue.removeIf(particle -> shouldRemove(particle, ConfigHelper.isRemoveIfMissedTick()));
		}
	}

	public boolean shouldRemove(Particle particle, boolean removeIfMissedTick) {
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
		return removeIfMissedTick;
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
//		AsyncRenderBehavior.debugLater(LOGGER::info);
//		AsyncRenderBehavior.tryDebug();
		CrashReport crashReport = CrashReport.forThrowable(t, "Ticking Particle");
		CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being ticked");
		crashReportCategory.setDetail("Particle", particle::toString);
		crashReportCategory.setDetail("Particle Type", particle.getGroup()::toString);
		return new ReportedException(crashReport);
	}

	public void preTick(boolean isHeadTick) {
		if (isHeadTick) {
			tickManager.waitForCompletion();
		}
		if (!ConfigHelper.isTickAsync()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		boolean levelRunning = mc.level != null && mc.player != null && !mc.isPaused();
		if (levelRunning) {
			if (cleanupManager.isRunning()) {
				throw new IllegalStateException("cleanupFuture is not null!");
			}
			Collection<ParticleGroup<?>> groups = mc.particleEngine.particles.values();
			for (ParticleGroup<?> group : groups) {
				if (!groups.isEmpty()) {
					cleanupManager.submitImmediately(((ParticleGroupAddition) group)::asyncparticles$removeDeadParticles);
				}
			}
		}
	}

	public void postTick(boolean isTailTick) {
		cleanupManager.waitForCompletion();
		Minecraft mc = Minecraft.getInstance();
		boolean levelRunning = mc.level != null && mc.player != null && !mc.isPaused();
		if (!ConfigHelper.isTickAsync()) {
			tryReload();
			if (levelRunning) {
				tickManager.runAllTasksDirectly();
			}
			return;
		}
		if (!levelRunning) {
			return;
		}
		mc.particleEngine.tick();
		if (!isTailTick) {
			this.isTailTick = false;
			tickManager.runAllTasksDirectly();
		} else {
			this.isTailTick = true;
			tryReload();
			tickManager.submitAll();
		}
	}

	public void dispatch(Runnable tickParticles) {
		tickManager.addTask(tickParticles);
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

	public void reload() {
		AsyncRenderBehavior.reset();
		reset();
		Minecraft.getInstance().particleEngine.clearParticles();
	}

	public void reset() {
		tickManager.waitForCompletion();
		tickManager.disposeTasks();
		cleanupManager.waitForCompletion();
		cleanupManager.disposeTasks();
	}

	public boolean shouldSync(Class<?> aClass) {
		return false; // TODO
	}

	public void recordSync(Particle particle) {
		synchronized (syncParticles) {
			syncParticles.add(particle);
		}
	}

	public boolean isTailTick() {
		return isTailTick;
	}

	public ExecutorService getExecutor() {
		return tickManager.executor();
	}

	public TaskManager getTickManager() {
		return tickManager;
	}

	public TaskManager getCleanupManager() {
		return cleanupManager;
	}

	public void tickSyncParticles() {
		// TODO
	}
}

package fun.qu_an.minecraft.asyncparticles.client.core.particle.tick;

import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleGroupAddition;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.BackendCaps;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.TaskHelper;
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

	{
		AtomicInteger workerCount = new AtomicInteger(1);
		EXECUTOR = new ForkJoinPool(THREADS, (forkJoinPool) -> {
			ForkJoinWorkerThread forkJoinWorkerThread = new AsyncTickerThread(forkJoinPool);
			forkJoinWorkerThread.setName(THREAD_PREFIX + "-" + workerCount.getAndIncrement());
			forkJoinWorkerThread.setDaemon(true);
			return forkJoinWorkerThread;
		}, Util::onThreadException, true);
	}

	private final TaskHelper tickTaskHelper = new TaskHelper(EXECUTOR, e -> {
		throw new RuntimeException(e);
	});
	private final TaskHelper cleanupTaskHelper = new TaskHelper(EXECUTOR, e -> {
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

	public boolean shouldRemove(Particle particle) {
		if (!particle.isAlive()) {
			return true;
		}
		ParticleAddon particleAddon = (ParticleAddon) particle;
		if (particleAddon.asyncparticles$isTickSync()) {
			return false;
		}
		if (ConfigHelper.isAsyncTickParticle() && particleAddon.asyncparticles$isTicked()) {
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

	public void preTick(boolean isHeadTick, boolean isTailTick) {
		if (isHeadTick) {
			tickTaskHelper.waitForCompletion();
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
				cleanupTaskHelper.submitImmediately(((ParticleGroupAddition) group)::asyncparticles$removeDeadParticles);
			}
		}
		cleanupTaskHelper.submitImmediately(() -> {
			Queue<TrackingEmitter> trackingEmitters = Minecraft.getInstance().particleEngine.trackingEmitters;
			doEmittersRemoveIf(trackingEmitters);
		});
	}

	public void doEmittersRemoveIf(Queue<? extends TrackingEmitter> queue) {
		doRemoveIf(queue, p -> !p.isAlive());
	}

	public void doParticlesRemoveIf(Queue<? extends Particle> particles) {
		doRemoveIf(particles, this::shouldRemove);
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
		cleanupTaskHelper.waitForCompletion();
		Minecraft mc = Minecraft.getInstance();
		boolean levelRunning = mc.level != null && mc.player != null && !mc.isPaused();
		if (!ConfigHelper.isAsyncTickParticle()) {
			tryReload();
			tryDebug();
			if (levelRunning) {
				tickTaskHelper.runAllTasks();
			}
			return;
		}
		if (!levelRunning) {
			return;
		}
		if (!isTailTick) {
			tickTaskHelper.disposeTasks();
			mc.particleEngine.particlesToAdd.forEach(this::onEvict);
			mc.particleEngine.particlesToAdd.clear();
		} else {
			tickTaskHelper.groupTasks(false);
			particlePhase = true;
			mc.particleEngine.tick();
			particlePhase = false;
			tryReload();
			tryDebug();
			tickTaskHelper.submitAll();
		}
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
		tickTaskHelper.waitForCompletion();
		tickTaskHelper.disposeTasks();
		cleanupTaskHelper.waitForCompletion();
		cleanupTaskHelper.disposeTasks();
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
			particles groups (render order, size):
			%s,
			particles to add size: %d
			sync particle types: %s,
			Backend: %s"""
			.formatted(
				tickTaskHelper.taskCount(),
				ConfigHelper.getParticleLimit(),
				Minecraft.getInstance().particleEngine.particles.entrySet()
					.stream()
					.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size())),
				Minecraft.getInstance().particleEngine.particlesToAdd.size(),
				syncParticleTypes.stream().map(Class::getName).toList(),
				BackendCaps.debugInfo()
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
}

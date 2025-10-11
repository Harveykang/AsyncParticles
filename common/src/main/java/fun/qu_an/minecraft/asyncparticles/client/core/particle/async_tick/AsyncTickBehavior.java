package fun.qu_an.minecraft.asyncparticles.client.core.particle.async_tick;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleGroupAddition;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_render.AsyncRendererThread;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionTracker;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeEvictingQueue;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncTickBehavior {
	public static final int THREADS = Mth.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, 6);
	public static final ForkJoinPool EXECUTOR;
	public static final String THREAD_PREFIX = "AsyncParticleTicker";
	private static Runnable tickFuture;
	private static Runnable cleanupFuture;
	private static final List<Runnable> tickTasks = new ArrayList<>();

	static {
		AtomicInteger workerCount = new AtomicInteger(1);
		EXECUTOR = new ForkJoinPool(THREADS, (forkJoinPool) -> {
			ForkJoinWorkerThread forkJoinWorkerThread = new AsyncRendererThread(forkJoinPool);
			forkJoinWorkerThread.setName(THREAD_PREFIX + "-" + workerCount.getAndIncrement());
			forkJoinWorkerThread.setDaemon(true);
			return forkJoinWorkerThread;
		}, Util::onThreadException, true);
	}

	private static final ExceptionTracker<Object> EXCEPTION_TRACKER = new ExceptionTracker<>(
		() -> 5000,
		ConfigHelper::getTickFailurePerSecondThreshold
	);

	public static <T extends Particle> void onEvict(T particle) {
		particle.getParticleLimit().ifPresent(limit -> Minecraft.getInstance().particleEngine.updateCount(limit, -1));
		if (particle.isAlive()) {
			particle.remove();
		}
	}

	public static void doEmittersRemoveIf(Queue<? extends TrackingEmitter> queue) {
		if (ConfigHelper.isParallelQueueRemoval()) {
			((IterationSafeEvictingQueue<? extends TrackingEmitter>) queue)
				.parallelRemoveIf(particle -> !particle.isAlive(),
					ConfigHelper.isParallelQueueEviction(),
					THREADS,
					EXECUTOR);
		} else {
			queue.removeIf(particle -> shouldRemove(particle, ConfigHelper.isRemoveIfMissedTick()));
		}
	}

	public static boolean shouldRemove(Particle particle, boolean removeIfMissedTick) {
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

	public static boolean isCancelled() {
		return false;
	}

	public static boolean isTolerable(@NotNull Throwable e) {
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

	public static ReportedException onTickParticleException(Particle particle, Throwable t) {
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
//						AsyncParticlesClient.ISSUE_URL,
//						t);
//				}
//				markAsSync(particleClass);
//			}
//			recordSync(particle);
//		} else if (tolerable) {
//			throw constructCrashReport(particle, new RuntimeException(
//				"Exception %s thrown while ticking particle %s, exceeds the threshold, please contact the author: %s"
//					.formatted(
//						t.getClass().getName(),
//						particle,
//						AsyncParticlesClient.ISSUE_URI),
//				t));
//		} else {
		throw constructCrashReport(particle, t);
//		}
	}

	public static ReportedException constructCrashReport(Particle particle, Throwable t) {
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

	public static void waitTickFuture() {
		if (tickFuture != null) {
			tickFuture.run();
			tickFuture = null;
		}
	}

	public static void waitCleanupFuture() {
		if (cleanupFuture != null) {
			cleanupFuture.run();
			cleanupFuture = null;
		}
	}

	public static void preTick(boolean isTheFirstTick) {
		if (isTheFirstTick) {
			waitTickFuture();
		}
		Minecraft mc = Minecraft.getInstance();
		boolean levelRunning = mc.level != null && mc.player != null && !mc.isPaused();
		if (levelRunning) {
			if (cleanupFuture != null) {
				throw new IllegalStateException("cleanupFuture is not null!");
			}
			Collection<ParticleGroup<?>> groups = mc.particleEngine.particles.values();
			ForkJoinTask<?>[] tasks = new ForkJoinTask[groups.size()];
			int idx = 0;
			for (ParticleGroup<?> group : groups) {
				if (!groups.isEmpty()) {
					tasks[idx++] = EXECUTOR.submit(((ParticleGroupAddition) group)::asyncparticles$cleanUp);
				}
			}
			cleanupFuture = () -> {
				for (ForkJoinTask<?> task : tasks) {
					if (task == null) {
						break;
					}
					try {
						task.get();
					} catch (InterruptedException | ExecutionException e) {
						throw new RuntimeException(e);
					}
				}
			};
		}
	}

	public static void postTick(boolean isTheLastTick) {
		waitCleanupFuture();
		Minecraft mc = Minecraft.getInstance();
		boolean levelRunning = mc.level != null && mc.player != null && !mc.isPaused();
		if (levelRunning) {
			mc.particleEngine.tick();
			if (!isTheLastTick) {
				tickTasks.forEach(Runnable::run);
			} else {
				ForkJoinTask<?>[] tasks = tickTasks.stream()
					.map(EXECUTOR::submit)
					.toArray(ForkJoinTask[]::new);
				tickFuture = () -> {
					for (ForkJoinTask<?> task : tasks) {
						if (task == null) {
							break;
						}
						try {
							task.get();
						} catch (InterruptedException | ExecutionException e) {
							throw new RuntimeException(e);
						}
					}
				};
			}
			tickTasks.clear();
		}
	}

	public static void dispatch(Runnable tickParticles) {
		tickTasks.add(tickParticles);
	}

}

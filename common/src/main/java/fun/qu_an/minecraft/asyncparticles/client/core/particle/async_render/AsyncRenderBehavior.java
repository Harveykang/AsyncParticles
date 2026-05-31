package fun.qu_an.minecraft.asyncparticles.client.core.particle.async_render;

import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionTracker;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncRenderBehavior {
	public static final int THREADS = Mth.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, 6);
	public static final ForkJoinPool EXECUTOR;
	public static final String THREAD_PREFIX = "AsyncParticleRenderWorker";
	private static boolean particlePhase;
	private static final List<ForkJoinTask<?>> futures = new ArrayList<>();
	private static Frustum frustum;

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

	public static boolean isParticlePhase() {
		return particlePhase;
	}

	public static void waitRenderingFuture() {
		CrashReport crashReport = null;
		CrashReportCategory category = null;
		for (ForkJoinTask<?> task : futures) {
			try {
				task.get();
			} catch (Exception e) {
				if (category == null) {
					crashReport = CrashReport.forThrowable(new RuntimeException(), "Exception during rendering");
					category = crashReport.addCategory("Supressed exceptions");
				}

				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				category.setDetail("Supressed exception", sw.toString());
			}
		}
		futures.clear();
		if (category != null) {
			throw new ReportedException(crashReport);
		}
	}

	public static void addRenderingFuture(ForkJoinTask<?> future) {
		futures.add(future);
	}

	public static void setFrustum(Frustum frustum) {
		AsyncRenderBehavior.frustum = frustum;
	}

	public static Frustum getFrustum() {
		return frustum;
	}

	public static boolean shouldSync(Class<?> aClass) {
		return false;
	}

	public static void submit(Runnable task) {
		futures.add(EXECUTOR.submit(task));
	}
}

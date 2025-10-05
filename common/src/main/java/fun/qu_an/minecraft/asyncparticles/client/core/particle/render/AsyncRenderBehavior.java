package fun.qu_an.minecraft.asyncparticles.client.core.particle.render;

import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionTracker;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncRenderBehavior {
	public static final int THREADS = Mth.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, 6);
	public static final ForkJoinPool EXECUTOR;
	public static final String THREAD_PREFIX = "AsyncParticleRenderer";
	private static boolean particlePhase;
	private static final List<ForkJoinTask<?>> futures = new ArrayList<>();

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
		Exception exception = null;
		for (ForkJoinTask<?> task : futures) {
			try {
				task.get();
			} catch (Exception e) {
				if (exception == null){
					exception = new RuntimeException("Rendering failed");
				} else {
					exception.addSuppressed(e);
				}
			}
		}
		futures.clear();
		if (exception!= null) {
			throw ExceptionUtil.toThrowDirectly(exception);
		}
	}

	public static void addRenderingFuture(ForkJoinTask<?> future) {
		futures.add(future);
	}
}

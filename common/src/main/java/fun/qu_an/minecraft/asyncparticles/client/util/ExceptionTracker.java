package fun.qu_an.minecraft.asyncparticles.client.util;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongPriorityQueue;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;

public class ExceptionTracker<T> {
	private static final Logger LOGGER = LogUtils.getLogger();
	private final Map<T, Map<Class<? extends Throwable>, ExceptionQueue>> exceptions = new ConcurrentHashMap<>();
	private final IntSupplier duration;
	private final IntSupplier failurePerSecThreshold;

	/**
	 * @param duration in milliseconds
	 * @param failurePerSecThreshold in failures per second
	 */
	public ExceptionTracker(IntSupplier duration, IntSupplier failurePerSecThreshold) {
		this.failurePerSecThreshold = failurePerSecThreshold;
		this.duration = duration;
	}

	/**
	 * @return true if the exception rate exceeds the threshold for the given object and type of exception
	 * @apiNote MUST pay attention to memory leak, cause the obj will be KEPT IN MEMORY
	 */
	public boolean addException(T obj, Throwable t) {
		Throwable rootCause = ExceptionUtil.getRootCause(t);
		return exceptions
			.computeIfAbsent(obj, k -> new ConcurrentHashMap<>())
			.computeIfAbsent(rootCause.getClass(), k -> {
				LOGGER.warn("Captured exception: (Note: If you encountered a crash, this exception is likely not the cause, please check the crash report for details)", t); // Print the exception once.
				return new ExceptionQueue();
			})
			.push();
	}

	public String toString() {
		return "ExceptionTracker{" +
			   "exceptions=" + exceptions +
			   ", duration=" + duration.getAsInt() +
			   ", failurePerSecThreshold=" + failurePerSecThreshold.getAsInt() +
			   '}';
	}

	private class ExceptionQueue {
		private final LongPriorityQueue queue = new LongArrayFIFOQueue();

		public synchronized boolean push() {
			long time = System.currentTimeMillis();
			LongPriorityQueue queue = this.queue;
			int size = queue.size();
			while (size-- > 0 && time - queue.firstLong() > duration.getAsInt()) {
				queue.dequeueLong();
			}
			queue.enqueue(time);
			return queue.size() / (duration.getAsInt() * 0.001) >= failurePerSecThreshold.getAsInt();
		}

		public String toString() {
			return "Exception queue size: " + queue.size();
		}
	}
}

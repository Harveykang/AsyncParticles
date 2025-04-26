package fun.qu_an.minecraft.asyncparticles.client.util;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongPriorityQueue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;

public class ExceptionTracker<T> {
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
		return exceptions
			.computeIfAbsent(obj, k -> new ConcurrentHashMap<>())
			.computeIfAbsent(ExceptionUtil.getRootCause(t).getClass(), k -> new ExceptionQueue())
			.push();
	}

	private class ExceptionQueue {
		private final LongPriorityQueue queue = new LongArrayFIFOQueue();

		public boolean push() {
			long time = System.currentTimeMillis();
			LongPriorityQueue queue = this.queue;
			queue.enqueue(time);
			int size = queue.size();
			while (--size >= 0 && time - queue.firstLong() > duration.getAsInt()) {
				queue.dequeueLong();
			}
			return queue.size() / (duration.getAsInt() * 0.001) >= failurePerSecThreshold.getAsInt();
		}
	}
}

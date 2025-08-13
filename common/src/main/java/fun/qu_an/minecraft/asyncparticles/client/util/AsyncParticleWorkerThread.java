package fun.qu_an.minecraft.asyncparticles.client.util;

import it.unimi.dsi.fastutil.HashCommon;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncParticleWorkerThread extends ForkJoinWorkerThread {
	private static final AtomicInteger indexGenerator = new AtomicInteger(0);
	private Object[] data = new Object[Math.max(HashCommon.nextPowerOfTwo(indexGenerator.get()), 4)];

	protected AsyncParticleWorkerThread(ForkJoinPool pool) {
		super(pool);
	}

	public static int nextThreadLocalIndex() {
		return indexGenerator.getAndIncrement();
	}

	public Object getThreadLocalValue(int index) {
		ensureIndex(index);
		return data[index];
	}

	private void ensureIndex(int index) {
		int length = data.length;
		if (index >= length) {
			Object[] newArray = new Object[HashCommon.nextPowerOfTwo(index + 1)];
			System.arraycopy(data, 0, newArray, 0, length);
			data = newArray;
		}
	}

	public void setThreadLocalValue(int index, Object value) {
		ensureIndex(index);
		data[index] = value;
	}
}

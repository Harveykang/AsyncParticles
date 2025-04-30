package fun.qu_an.minecraft.asyncparticles.client.util;

import java.util.function.Supplier;

public interface SpinLock {
	void lock();

	void unlock();

	default AutoCloseable sugar() {
		lock();
		return this::unlock;
	}

	default void wrap(Runnable runnable) {
		lock();
		try {
			runnable.run();
		} finally {
			unlock();
		}
	}

	default <T> T wrap(Supplier<T> supplier) {
		lock();
		try {
			return supplier.get();
		} finally {
			unlock();
		}
	}
}

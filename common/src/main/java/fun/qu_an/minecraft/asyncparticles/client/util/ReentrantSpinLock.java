package fun.qu_an.minecraft.asyncparticles.client.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Supplier;

public class ReentrantSpinLock implements AutoCloseable {
	private static final VarHandle OWNER;

	static {
		try {
			OWNER = MethodHandles.lookup()
				.findVarHandle(ReentrantSpinLock.class, "owner", Thread.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private volatile Thread owner;
	private int holdCount;

	public void lock() {
		Thread currentThread = Thread.currentThread();
		if (!OWNER.compareAndSet(this, null, currentThread)) {
			if (currentThread == owner) {
				holdCount++;
				return;
			}
			while (!OWNER.compareAndSet(this, null, currentThread)) {
				Thread.onSpinWait();
			}
		}
		holdCount = 1;
	}

	public void unlock() {
		Thread currentThread = Thread.currentThread();
		if (currentThread != owner) {
			throw new IllegalMonitorStateException("Attempt to unlock a lock held by another thread!");
		}
		if (--holdCount == 0) {
			owner = null;
		}
	}

	@Override
	public void close() {
		unlock();
	}

	public ReentrantSpinLock sugar() {
		lock();
		return this;
	}

	public void wrap(Runnable runnable) {
		lock();
		try {
			runnable.run();
		} finally {
			unlock();
		}
	}

	public <T> T wrap(Supplier<T> supplier) {
		lock();
		try {
			return supplier.get();
		} finally {
			unlock();
		}
	}
}

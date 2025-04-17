package fun.qu_an.minecraft.asyncparticles.client.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Supplier;

public class SpinLock {
	private static final VarHandle OWNER;

	static {
		try {
			OWNER = MethodHandles.lookup()
				.findVarHandle(SpinLock.class, "owner", Thread.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@SuppressWarnings("unused")
	private volatile Thread owner;

	public void lock() {
		Thread thread = Thread.currentThread();
		if (OWNER.compareAndSet(this, null, thread)) {
			return;
		}
		if (thread == this.owner) {
			throw new IllegalMonitorStateException("Attempt to lock an already locked lock!");
		}
		while (!OWNER.compareAndSet(this, null, thread)) {
			Thread.onSpinWait();
		}
	}

	public void unlock() {
		Thread thread = Thread.currentThread();
		if (!OWNER.compareAndSet(this, thread, null)) {
			throw new IllegalMonitorStateException("Attempt to unlock an non-locked lock!");
		}
	}

	public AutoCloseable sugar() {
		lock();
		return this::unlock;
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

package fun.qu_an.minecraft.asyncparticles.client.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class SpinLock {
	private static final VarHandle VALUE;

	static {
		try {
			VALUE = MethodHandles.lookup()
				.findVarHandle(SpinLock.class, "thread", Thread.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@SuppressWarnings("unused")
	private volatile Thread thread;

	public void lock() {
		Thread thread = Thread.currentThread();
		while (!VALUE.compareAndSet(this, null, thread)) {
			if (thread == this.thread) {
				throw new IllegalMonitorStateException("Attempt to lock an already locked lock!");
			}
			Thread.onSpinWait();
		}
	}

	public void unlock() {
		Thread thread = Thread.currentThread();
		if (!VALUE.compareAndSet(this, thread, null)) {
			throw new IllegalMonitorStateException("Attempt to unlock an non-locked lock!");
		}
	}
}

package fun.qu_an.minecraft.asyncparticles.client.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class SingleSpinLock implements SpinLock {
	private static final VarHandle OWNER;

	static {
		try {
			OWNER = MethodHandles.lookup()
				.findVarHandle(SingleSpinLock.class, "owner", Thread.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@SuppressWarnings("unused")
	private volatile Thread owner;

	@Override
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

	@Override
	public void unlock() {
		Thread thread = Thread.currentThread();
		if (!OWNER.compareAndSet(this, thread, null)) {
			throw new IllegalMonitorStateException("Attempt to unlock an non-locked lock!");
		}
	}
}

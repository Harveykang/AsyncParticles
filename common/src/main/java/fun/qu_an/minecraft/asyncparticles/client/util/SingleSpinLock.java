package fun.qu_an.minecraft.asyncparticles.client.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Supplier;

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
		Thread currentThread = Thread.currentThread();
		if (OWNER.compareAndSet(this, null, currentThread)) {
			return;
		}
		if (currentThread == this.owner) {
			throw new IllegalMonitorStateException("Attempt to lock an already locked lock!");
		}
		int i = 0;
		while (!OWNER.compareAndSet(this, null, currentThread)) {
			if (i < 50){
				Thread.onSpinWait();
				++i;
			} else {
				Thread.yield();
			}
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

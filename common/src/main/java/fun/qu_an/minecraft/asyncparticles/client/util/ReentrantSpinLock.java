package fun.qu_an.minecraft.asyncparticles.client.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class ReentrantSpinLock implements SpinLock {
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

	@Override
	public void lock() {
		Thread currentThread = Thread.currentThread();
		if (!OWNER.compareAndSet(this, null, currentThread)) {
			if (currentThread == owner) {
				holdCount++;
				return;
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
		holdCount = 1;
	}

	@Override
	public void unlock() {
		Thread currentThread = Thread.currentThread();
		if (currentThread != owner) {
			throw new IllegalMonitorStateException("Attempt to unlock a lock held by another thread!");
		}
		if (--holdCount == 0) {
			owner = null;
		}
	}
}

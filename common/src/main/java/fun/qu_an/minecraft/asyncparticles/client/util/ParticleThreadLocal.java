package fun.qu_an.minecraft.asyncparticles.client.util;

import java.util.function.Supplier;

public sealed class ParticleThreadLocal<T> permits ParticleThreadLocal.SuppliedParticleThreadLocal {
	public final int index;
	protected ThreadLocal<T> fallback;

	public static <S> ParticleThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
		return new SuppliedParticleThreadLocal<>(supplier);
	}

	public ParticleThreadLocal() {
		this.index = AsyncParticleWorkerThread.nextThreadLocalIndex();
	}

	public void setUnsafe(T value) {
		AsyncParticleWorkerThread wt = (AsyncParticleWorkerThread) Thread.currentThread();
		wt.setThreadLocalValue(index, value);
	}

	@SuppressWarnings("unchecked")
	public T getUnsafe() {
		AsyncParticleWorkerThread wt = (AsyncParticleWorkerThread) Thread.currentThread();
		return (T) wt.getThreadLocalValue(index);
	}

	public T getSafe(T orElse) {
		Thread thread = Thread.currentThread();
		if (!(thread instanceof AsyncParticleWorkerThread)) {
			return orElse;
		}
		return getUnsafe();
	}

	public T getSafe(Supplier<T> orElse) {
		Thread thread = Thread.currentThread();
		if (!(thread instanceof AsyncParticleWorkerThread)) {
			return orElse.get();
		}
		return getUnsafe();
	}

	public T get() {
		Thread thread = Thread.currentThread();
		if (thread instanceof AsyncParticleWorkerThread) {
			return getUnsafe();
		} else {
			return getFallback().get();
		}
	}

	private ThreadLocal<T> getFallback() {
		ThreadLocal<T> fallback = this.fallback;
		if (fallback == null) {
			synchronized (this) {
				fallback = this.fallback;
				if (fallback == null) {
					fallback = this.fallback = newFallbackThreadLocal();
				}
			}
		}
		return fallback;
	}

	protected ThreadLocal<T> newFallbackThreadLocal() {
		return new ThreadLocal<>();
	}

	public void set(T value) {
		Thread thread = Thread.currentThread();
		if (thread instanceof AsyncParticleWorkerThread wt) {
			setUnsafe(value);
		} else {
			getFallback().set(value);
		}
	}

	public void remove() {
		Thread thread = Thread.currentThread();
		if (thread instanceof AsyncParticleWorkerThread wt) {
			setUnsafe(null);
		} else {
			getFallback().remove();
		}
	}

	protected static final class SuppliedParticleThreadLocal<S> extends ParticleThreadLocal<S> {
		private static final Object NULL_VALUE = new Object();
		private final Supplier<? extends S> supplier;

		private SuppliedParticleThreadLocal(Supplier<? extends S> supplier) {
			this.supplier = supplier;
		}

		public void setUnsafe(S value) {
			AsyncParticleWorkerThread wt = (AsyncParticleWorkerThread) Thread.currentThread();
			wt.setThreadLocalValue(index, value == null ? NULL_VALUE : value);
		}

		@SuppressWarnings("unchecked")
		public S getUnsafe() {
			AsyncParticleWorkerThread wt = (AsyncParticleWorkerThread) Thread.currentThread();
			@SuppressWarnings("unchecked")
			S value = (S) wt.getThreadLocalValue(index);
			if (value == null) {
				S value1 = supplier.get();
				setUnsafe(value1);
				return value1;
			}
			return value == NULL_VALUE ? null : value;
		}

		@Override
		protected ThreadLocal<S> newFallbackThreadLocal() {
			return ThreadLocal.withInitial(supplier);
		}
	}
}

package fun.qu_an.minecraft.asyncparticles.client.util;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public sealed class ParticleThreadLocal<T> permits ParticleThreadLocal.SuppliedParticleThreadLocal {
	public final int index;
	protected ThreadLocal<T> fallback;
	protected BooleanSupplier isMainThread;
	protected T mainValue;

	public static <S> ParticleThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
		return new SuppliedParticleThreadLocal<>(supplier);
	}

	public static <S> ParticleThreadLocal<S> withInitial(BooleanSupplier isMainThread, Supplier<? extends S> supplier) {
		return new SuppliedParticleThreadLocal<>(isMainThread, supplier);
	}

	public ParticleThreadLocal() {
		Thread initThread = Thread.currentThread();
		this(() -> initThread == Thread.currentThread());
	}

	public ParticleThreadLocal(BooleanSupplier isMainThread) {
		this.isMainThread = isMainThread;
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

	protected T getMain() {
		return mainValue;
	}

	protected void setMain(T value) {
		mainValue = value;
	}

	public T getSafe(T orElse) {
		Thread thread = Thread.currentThread();
		if (isMainThread.getAsBoolean()) {
			return getMain();
		} else if (thread instanceof AsyncParticleWorkerThread) {
			return getUnsafe();
		} else {
			return orElse;
		}
	}

	public T getSafe(Supplier<T> orElse) {
		Thread thread = Thread.currentThread();
		if (isMainThread.getAsBoolean()) {
			return getMain();
		} else if (thread instanceof AsyncParticleWorkerThread) {
			return getUnsafe();
		} else {
			return orElse.get();
		}
	}

	public T get() {
		Thread thread = Thread.currentThread();
		if (isMainThread.getAsBoolean()) {
			return getMain();
		} else if (thread instanceof AsyncParticleWorkerThread) {
			return getUnsafe();
		} else {
			return getFallback().get();
		}
	}

	protected ThreadLocal<T> getFallback() {
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
		if (isMainThread.getAsBoolean()) {
			setMain(value);
		} else if (thread instanceof AsyncParticleWorkerThread wt) {
			setUnsafe(value);
		} else {
			getFallback().set(value);
		}
	}

	public void remove() {
		Thread thread = Thread.currentThread();
		if (isMainThread.getAsBoolean()) {
			setMain(null);
		} else if (thread instanceof AsyncParticleWorkerThread wt) {
			setUnsafe(null);
		} else {
			getFallback().remove();
		}
	}

	protected static final class SuppliedParticleThreadLocal<S> extends ParticleThreadLocal<S> {
		private static final Object NULL_VALUE = new Object();
		private final Supplier<? extends S> supplier;

		private SuppliedParticleThreadLocal(Supplier<? extends S> supplier) {
			super();
			this.supplier = supplier;
		}

		private SuppliedParticleThreadLocal(BooleanSupplier isMainThread, Supplier<? extends S> supplier) {
			super(isMainThread);
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

		protected S getMain() {
			if (mainValue == null) {
				return mainValue = supplier.get();
			} else if (mainValue == NULL_VALUE) {
				return null;
			} else {
				return mainValue;
			}
		}

		@SuppressWarnings("unchecked")
		protected void setMain(S value) {
			if (value == null) {
				mainValue = (S) NULL_VALUE;
			} else {
				mainValue = value;
			}
		}

		@Override
		protected ThreadLocal<S> newFallbackThreadLocal() {
			return ThreadLocal.withInitial(supplier);
		}
	}
}

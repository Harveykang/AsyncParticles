package fun.qu_an.minecraft.asyncparticles.client.util;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BusyWaitEvictingQueue<E> extends IterationSafeEvictingQueue<E> {
	private final ReentrantSpinLock lock = new ReentrantSpinLock();

	public BusyWaitEvictingQueue(int initialCapacity, int maxCapacity) {
		super(initialCapacity, maxCapacity);
	}

	public BusyWaitEvictingQueue(int initialCapacity, int maxCapacity, Consumer<E> onEvict) {
		super(initialCapacity, maxCapacity, onEvict);
	}

	public static <E> BusyWaitEvictingQueue<E> newInstance(int initialCapacity, int maxCapacity) {
		return new BusyWaitEvictingQueue<>(Math.min(initialCapacity, maxCapacity), maxCapacity);
	}

	public static <E> BusyWaitEvictingQueue<E> newInstance(int initialCapacity, int maxCapacity, Consumer<E> onEvict) {
		return new BusyWaitEvictingQueue<>(Math.min(initialCapacity, maxCapacity), maxCapacity, onEvict);
	}

	@Override
	public boolean add(E e) {
		return lock.wrap(() -> super.add(e));
	}

	@Override
	public boolean remove(Object o) {
		return lock.wrap(() -> super.remove(o));
	}

	@Override
	public boolean addAll(@NotNull Collection<? extends E> c) {
		return lock.wrap(() -> super.addAll(c));
	}

	@Override
	public boolean removeAll(@NotNull Collection<?> c) {
		return lock.wrap(() -> super.removeAll(c));
	}

	public void parallelRemoveIf(@NotNull Predicate<? super E> filter, boolean parallelEvicting, int threads, ExecutorService executor) {
		lock.wrap(() -> super.parallelRemoveIf(filter, parallelEvicting, threads, executor));
	}

	@Override
	public boolean removeIf(@NotNull Predicate<? super E> filter) {
		return lock.wrap(() -> super.removeIf(filter));
	}

	@Override
	public boolean retainAll(@NotNull Collection<?> c) {
		return lock.wrap(() -> super.retainAll(c));
	}

	@Override
	public void clear() {
		lock.wrap(super::clear);
	}

	@Override
	public boolean offer(E e) {
		return lock.wrap(() -> super.offer(e));
	}

	@Override
	public E remove() {
		return lock.wrap(() -> super.remove());
	}

	@Override
	public E poll() {
		return lock.wrap(super::poll);
	}
}

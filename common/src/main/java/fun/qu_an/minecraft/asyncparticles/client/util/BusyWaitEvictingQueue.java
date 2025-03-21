package fun.qu_an.minecraft.asyncparticles.client.util;

import com.google.common.collect.Queues;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.core.impl.shadow.C;

import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BusyWaitEvictingQueue<E> extends IterationSafeEvictingQueue<E> {
	private final ReentrantSpinLock lock = new ReentrantSpinLock();

	public BusyWaitEvictingQueue(int initialCapacity, int maxCapacity, Consumer<E> onEvict) {
		super(initialCapacity, maxCapacity, onEvict);
	}

	@Override
	public boolean offer(E e) {
		return lock.wrap(() -> super.offer(e));
	}

	@Override
	public E poll() {
		return lock.wrap(super::poll);
	}

	@Override
	public E peek() {
		return lock.wrap(super::peek);
	}

	@Override
	public int size() {
		return lock.wrap(super::size);
	}

	@Override
	public boolean isEmpty() {
		return lock.wrap(super::isEmpty);
	}

	@Override
	public void clear() {
		lock.wrap(super::clear);
	}

	@Override
	public boolean remove(Object o) {
		return lock.wrap(() -> super.remove(o));
	}

	@Override
	public boolean contains(Object o) {
		return lock.wrap(() -> super.contains(o));
	}

	@Override
	public Object @NotNull [] toArray() {
		return lock.wrap(() -> super.toArray());
	}

	@Override
	public <T> T @NotNull [] toArray(T[] a) {
		return lock.wrap(() -> super.toArray(a));
	}

	@Override
	public String toString() {
		return lock.wrap(super::toString);
	}

	@Override
	public int hashCode() {
		return lock.wrap(super::hashCode);
	}

	@Override
	public boolean equals(Object obj) {
		return lock.wrap(() -> super.equals(obj));
	}

	@Override
	public void forEach(@NotNull Consumer<? super E> action) {
		lock.wrap(() -> super.forEach(action));
	}

	@Override
	public boolean removeIf(@NotNull Predicate<? super E> filter) {
		return lock.wrap(() -> super.removeIf(filter));
	}
}

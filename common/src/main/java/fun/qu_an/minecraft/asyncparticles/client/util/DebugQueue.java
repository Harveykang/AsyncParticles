package fun.qu_an.minecraft.asyncparticles.client.util;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class DebugQueue<E> implements Queue<E> {
	private final Queue<E> delegate;
	private final Debugger debugger;

	public DebugQueue(Queue<E> delegate, Debugger debugger) {
		this.delegate = delegate;
		this.debugger = debugger;
	}

	@Override
	public int size() {
		debugger.debugHead();
		try {
			return delegate.size();
		} finally {
			debugger.debugFinally();
		}
	}

	@Override
	public boolean isEmpty() {
		debugger.debugHead();
		try {
			return delegate.isEmpty();
		} finally {
			debugger.debugFinally();
		}
	}

	@Override
	public boolean contains(Object o) {
		debugger.debugHead();
		try {
			return delegate.contains(o);
		} finally {
			debugger.debugFinally();
		}
	}

	@Override
	public @NotNull Iterator<E> iterator() {
		debugger.debugHead();
		try {
			return delegate.iterator();
		} finally {
			debugger.debugFinally();
		}
	}

	@Override
	public void forEach(Consumer<? super E> action) {
		debugger.debugHead();
		try {
			delegate.forEach(action);
		} finally {
			debugger.debugFinally();
		}
	}

	@Override
	public @NotNull Object[] toArray() {
		debugger.debugHead();
		try {
			return delegate.toArray();
		} finally {
			debugger.debugFinally();
		}
	}

	@Override
	public @NotNull <T> T[] toArray(@NotNull T[] a) {
		debugger.debugHead();
		try {
			return delegate.toArray(a);
		} finally {
			debugger.debugFinally();
		}
	}

	@Override
	public <T> T[] toArray(@NotNull IntFunction<T[]> generator) {
		debugger.debugHead();
		try {
			return delegate.toArray(generator);
		} finally {
			debugger.debugFinally();
		}
	}

	@Override
	public boolean add(E e) {
		debugger.debugHead();
		try {
			return delegate.add(e);
		} finally {
			debugger.debugFinally();
		}
	}

	@Override
	public boolean remove(Object o) {
		debugger.debugHead();
		try {
			return delegate.remove(o);
		} finally {
			debugger.debugFinally();
		}
	}

	@Override
	public boolean containsAll(@NotNull Collection<?> c) {
		debugger.debugHead();
		try {
			return delegate.containsAll(c);
		} finally {
			debugger.debugFinally();
		}
	}

	@Override
	public boolean addAll(@NotNull Collection<? extends E> c) {
		debugger.debugHead();
		try {
			return delegate.addAll(c);
		} finally {
			debugger.debugFinally();
		}
	}

	@Override
	public boolean removeAll(@NotNull Collection<?> c) {
		debugger.debugHead();
		try {
			return delegate.removeAll(c);
		} finally {
			debugger.debugFinally();
		}
	}

	@Override
	public boolean removeIf(@NotNull Predicate<? super E> filter) {
		debugger.debugHead();
		try {
			return delegate.removeIf(filter);
		} finally {
			debugger.debugFinally();
		}
	}

	@Override
	public boolean retainAll(@NotNull Collection<?> c) {
		debugger.debugHead();
		try {
			return delegate.retainAll(c);
		} finally {
			debugger.debugFinally();
		}
	}

	@Override
	public void clear() {
		debugger.debugHead();
		try {
			delegate.clear();
		} finally {
			debugger.debugFinally();
		}
	}

	@Override
	public @NotNull Spliterator<E> spliterator() {
		debugger.debugHead();
		try {
			return delegate.spliterator();
		} finally {
			debugger.debugFinally();
		}
	}

	@Override
	public @NotNull Stream<E> stream() {
		debugger.debugHead();
		try {
			return delegate.stream();
		} finally {
			debugger.debugFinally();
		}
	}

	@Override
	public @NotNull Stream<E> parallelStream() {
		debugger.debugHead();
		try {
			return delegate.parallelStream();
		} finally {
			debugger.debugFinally();
		}
	}

	@Override
	public boolean offer(E e) {
		debugger.debugHead();
		try {
			return delegate.offer(e);
		} finally {
			debugger.debugFinally();
		}
	}

	@Override
	public E remove() {
		debugger.debugHead();
		try {
			return delegate.remove();
		} finally {
			debugger.debugFinally();
		}
	}

	@Override
	public E poll() {
		debugger.debugHead();
		try {
			return delegate.poll();
		} finally {
			debugger.debugFinally();
		}
	}

	@Override
	public E element() {
		debugger.debugHead();
		try {
			return delegate.element();
		} finally {
			debugger.debugFinally();
		}
	}

	@Override
	public E peek() {
		debugger.debugHead();
		try {
			return delegate.peek();
		} finally {
			debugger.debugFinally();
		}
	}

	public interface Debugger {
		default void debugHead() {
		}

		default void debugFinally() {
		}
	}
}

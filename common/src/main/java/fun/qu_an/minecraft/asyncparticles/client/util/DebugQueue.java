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
	private final CollectionDebugger debugger;

	public DebugQueue(Queue<E> delegate, CollectionDebugger debugger) {
		this.delegate = delegate;
		this.debugger = debugger;
	}

	@Override
	public int size() {
		debugger.debugHead("size");
		try {
			return delegate.size();
		} finally {
			debugger.debugFinally("size");
		}
	}

	@Override
	public boolean isEmpty() {
		debugger.debugHead("isEmpty");
		try {
			return delegate.isEmpty();
		} finally {
			debugger.debugFinally("isEmpty");
		}
	}

	@Override
	public boolean contains(Object o) {
		debugger.debugHead("contains(Object o)");
		try {
			return delegate.contains(o);
		} finally {
			debugger.debugFinally("contains(Object o)");
		}
	}

	@Override
	public @NotNull Iterator<E> iterator() {
		debugger.debugHead("iterator");
		try {
			return delegate.iterator();
		} finally {
			debugger.debugFinally("iterator");
		}
	}

	@Override
	public void forEach(Consumer<? super E> action) {
		debugger.debugHead("forEach(Consumer<? super E> action)");
		try {
			delegate.forEach(action);
		} finally {
			debugger.debugFinally("forEach(Consumer<? super E> action)");
		}
	}

	@Override
	public @NotNull Object @NotNull [] toArray() {
		debugger.debugHead("toArray");
		try {
			return delegate.toArray();
		} finally {
			debugger.debugFinally("toArray");
		}
	}

	@Override
	public @NotNull <T> T @NotNull [] toArray(@NotNull T[] a) {
		debugger.debugHead("toArray(T[] a)");
		try {
			return delegate.toArray(a);
		} finally {
			debugger.debugFinally("toArray(T[] a)");
		}
	}

	@Override
	public <T> T[] toArray(@NotNull IntFunction<T[]> generator) {
		debugger.debugHead("toArray(IntFunction<T[]> generator)");
		try {
			return delegate.toArray(generator);
		} finally {
			debugger.debugFinally("toArray(IntFunction<T[]> generator)");
		}
	}

	@Override
	public boolean add(E e) {
		debugger.debugHead("add");
		try {
			return delegate.add(e);
		} finally {
			debugger.debugFinally("add");
		}
	}

	@Override
	public boolean remove(Object o) {
		debugger.debugHead("remove(Object o)");
		try {
			return delegate.remove(o);
		} finally {
			debugger.debugFinally("remove(Object o)");
		}
	}

	@Override
	public boolean containsAll(@NotNull Collection<?> c) {
		debugger.debugHead("containsAll(Collection<?> c)");
		try {
			return delegate.containsAll(c);
		} finally {
			debugger.debugFinally("containsAll(Collection<?> c)");
		}
	}

	@Override
	public boolean addAll(@NotNull Collection<? extends E> c) {
		debugger.debugHead("addAll(Collection<? extends E> c)");
		try {
			return delegate.addAll(c);
		} finally {
			debugger.debugFinally("addAll(Collection<? extends E> c)");
		}
	}

	@Override
	public boolean removeAll(@NotNull Collection<?> c) {
		debugger.debugHead("removeAll(Collection<?> c)");
		try {
			return delegate.removeAll(c);
		} finally {
			debugger.debugFinally("removeAll(Collection<?> c)");
		}
	}

	@Override
	public boolean removeIf(@NotNull Predicate<? super E> filter) {
		debugger.debugHead("removeIf(Predicate<? super E> filter)");
		try {
			return delegate.removeIf(filter);
		} finally {
			debugger.debugFinally("removeIf(Predicate<? super E> filter)");
		}
	}

	@Override
	public boolean retainAll(@NotNull Collection<?> c) {
		debugger.debugHead("retainAll(Collection<?> c)");
		try {
			return delegate.retainAll(c);
		} finally {
			debugger.debugFinally("retainAll(Collection<?> c)");
		}
	}

	@Override
	public void clear() {
		debugger.debugHead("clear");
		try {
			delegate.clear();
		} finally {
			debugger.debugFinally("clear");
		}
	}

	@Override
	public @NotNull Spliterator<E> spliterator() {
		debugger.debugHead("spliterator");
		try {
			return delegate.spliterator();
		} finally {
			debugger.debugFinally("spliterator");
		}
	}

	@Override
	public @NotNull Stream<E> stream() {
		debugger.debugHead("stream");
		try {
			return delegate.stream();
		} finally {
			debugger.debugFinally("stream");
		}
	}

	@Override
	public @NotNull Stream<E> parallelStream() {
		debugger.debugHead("parallelStream");
		try {
			return delegate.parallelStream();
		} finally {
			debugger.debugFinally("parallelStream");
		}
	}

	@Override
	public boolean offer(E e) {
		debugger.debugHead("offer(E e)");
		try {
			return delegate.offer(e);
		} finally {
			debugger.debugFinally("offer(E e)");
		}
	}

	@Override
	public E remove() {
		debugger.debugHead("remove");
		try {
			return delegate.remove();
		} finally {
			debugger.debugFinally("remove");
		}
	}

	@Override
	public E poll() {
		debugger.debugHead("poll");
		try {
			return delegate.poll();
		} finally {
			debugger.debugFinally("poll");
		}
	}

	@Override
	public E element() {
		debugger.debugHead("element");
		try {
			return delegate.element();
		} finally {
			debugger.debugFinally("element");
		}
	}

	@Override
	public E peek() {
		debugger.debugHead("peek");
		try {
			return delegate.peek();
		} finally {
			debugger.debugFinally("peek");
		}
	}

}

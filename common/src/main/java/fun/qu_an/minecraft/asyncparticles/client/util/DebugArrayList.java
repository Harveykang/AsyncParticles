package fun.qu_an.minecraft.asyncparticles.client.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class DebugArrayList<E> extends ArrayList<E> {
	private final CollectionDebugger debugger;

	public DebugArrayList(CollectionDebugger debugger) {
		super();
		this.debugger = debugger;
	}

	public DebugArrayList(CollectionDebugger debugger, int initialCapacity) {
		super(initialCapacity);
		this.debugger = debugger;
	}

	public DebugArrayList(CollectionDebugger debugger, Collection<? extends E> c) {
		super(c);
		this.debugger = debugger;
	}

	@Override
	public int size() {
		debugger.debugHead("size");
		try {
			return super.size();
		} finally {
			debugger.debugFinally("size");
		}
	}

	@Override
	public boolean isEmpty() {
		debugger.debugHead("isEmpty");
		try {
			return super.isEmpty();
		} finally {
			debugger.debugFinally("isEmpty");
		}
	}

	@Override
	public boolean contains(Object o) {
		debugger.debugHead("contains(Object o)");
		try {
			return super.contains(o);
		} finally {
			debugger.debugFinally("contains");
		}
	}

	@Override
	public @NotNull Iterator<E> iterator() {
		debugger.debugHead("iterator");
		try {
			return super.iterator();
		} finally {
			debugger.debugFinally("iterator");
		}
	}

	@Override
	public void forEach(Consumer<? super E> action) {
		debugger.debugHead("forEach(Consumer<? super E> action)");
		try {
			super.forEach(action);
		} finally {
			debugger.debugFinally("forEach(Consumer<? super E> action)");
		}
	}

	@Override
	public @NotNull Object @NotNull [] toArray() {
		debugger.debugHead("toArray");
		try {
			return super.toArray();
		} finally {
			debugger.debugFinally("toArray");
		}
	}

	@Override
	public <T> T[] toArray(@NotNull IntFunction<T[]> generator) {
		debugger.debugHead("toArray(IntFunction generator)");
		try {
			return super.toArray(generator);
		} finally {
			debugger.debugFinally("toArray(IntFunction generator)");
		}
	}

	@Override
	public boolean add(E o) {
		debugger.debugHead("add(Object o)");
		try {
			return super.add(o);
		} finally {
			debugger.debugFinally("add(Object o)");
		}
	}

	@Override
	public boolean remove(Object o) {
		debugger.debugHead("remove(Object o)");
		try {
			return super.remove(o);
		} finally {
			debugger.debugFinally("remove(Object o)");
		}
	}

	@Override
	public boolean addAll(@NotNull Collection<? extends E> c) {
		debugger.debugHead("addAll(Collection c)");
		try {
			return super.addAll(c);
		} finally {
			debugger.debugFinally("addAll(Collection c)");
		}
	}

	@Override
	public boolean removeIf(@NotNull Predicate<? super E> filter) {
		debugger.debugHead("removeIf(Predicate filter)");
		try {
			return super.removeIf(filter);
		} finally {
			debugger.debugFinally("removeIf(Predicate filter)");
		}
	}

	@Override
	public boolean addAll(int index, @NotNull Collection<? extends E> c) {
		debugger.debugHead("addAll(int index, Collection<? extends E> c)");
		try {
			return super.addAll(index, c);
		} finally {
			debugger.debugFinally("addAll(int index, Collection<? extends E> c)");
		}
	}

	@Override
	public void replaceAll(@NotNull UnaryOperator<E> operator) {
		debugger.debugHead("replaceAll(UnaryOperator operator)");
		try {
			super.replaceAll(operator);
		} finally {
			debugger.debugFinally("replaceAll(UnaryOperator operator)");
		}
	}

	@Override
	public void sort(@Nullable Comparator<? super E> c) {
		debugger.debugHead("sort(Comparator c)");
		try {
			super.sort(c);
		} finally {
			debugger.debugFinally("sort(Comparator c)");
		}
	}

	@Override
	public void clear() {
		debugger.debugHead("clear");
		try {
			super.clear();
		} finally {
			debugger.debugFinally("clear");
		}
	}

	@Override
	public E get(int index) {
		debugger.debugHead("get(int index)");
		try {
			return super.get(index);
		} finally {
			debugger.debugFinally("get(int index)");
		}
	}

	@Override
	public E set(int index, E element) {
		debugger.debugHead("set(int index, Object element)");
		try {
			return super.set(index, element);
		} finally {
			debugger.debugFinally("set(int index, Object element)");
		}
	}

	@Override
	public void add(int index, E element) {
		debugger.debugHead("add(int index, Object element)");
		try {
			super.add(index, element);
		} finally {
			debugger.debugFinally("add(int index, Object element)");
		}
	}

	@Override
	public E remove(int index) {
		debugger.debugHead("remove(int index)");
		try {
			return super.remove(index);
		} finally {
			debugger.debugFinally("remove(int index)");
		}
	}

	@Override
	public int indexOf(Object o) {
		debugger.debugHead("indexOf(Object o)");
		try {
			return super.indexOf(o);
		} finally {
			debugger.debugFinally("indexOf(Object o)");
		}
	}

	@Override
	public int lastIndexOf(Object o) {
		debugger.debugHead("lastIndexOf(Object o)");
		try {
			return super.lastIndexOf(o);
		} finally {
			debugger.debugFinally("lastIndexOf(Object o)");
		}
	}

	@Override
	public @NotNull ListIterator<E> listIterator() {
		debugger.debugHead("listIterator");
		try {
			return super.listIterator();
		} finally {
			debugger.debugFinally("listIterator");
		}
	}

	@Override
	public @NotNull ListIterator<E> listIterator(int index) {
		debugger.debugHead("listIterator(int index)");
		try {
			return super.listIterator(index);
		} finally {
			debugger.debugFinally("listIterator(int index)");
		}
	}

	@Override
	public @NotNull List<E> subList(int fromIndex, int toIndex) {
		debugger.debugHead("subList(int fromIndex, int toIndex)");
		try {
			return super.subList(fromIndex, toIndex);
		} finally {
			debugger.debugFinally("subList(int fromIndex, int toIndex)");
		}
	}

	@Override
	public @NotNull Spliterator<E> spliterator() {
		debugger.debugHead("spliterator");
		try {
			return super.spliterator();
		} finally {
			debugger.debugFinally("spliterator");
		}
	}

	@Override
	public @NotNull Stream<E> stream() {
		debugger.debugHead("stream");
		try {
			return super.stream();
		} finally {
			debugger.debugFinally("stream");
		}
	}

	@Override
	public @NotNull Stream<E> parallelStream() {
		debugger.debugHead("parallelStream");
		try {
			return super.parallelStream();
		} finally {
			debugger.debugFinally("parallelStream");
		}
	}

	@Override
	public boolean retainAll(@NotNull Collection<?> c) {
		debugger.debugHead("retainAll(Collection c)");
		try {
			return super.retainAll(c);
		} finally {
			debugger.debugFinally("retainAll(Collection c)");
		}
	}

	@Override
	public boolean removeAll(@NotNull Collection<?> c) {
		debugger.debugHead("removeAll(Collection c)");
		try {
			return super.removeAll(c);
		} finally {
			debugger.debugFinally("removeAll(Collection c)");
		}
	}

	@Override
	public boolean containsAll(@NotNull Collection<?> c) {
		debugger.debugHead("containsAll(Collection c)");
		try {
			return super.containsAll(c);
		} finally {
			debugger.debugFinally("containsAll(Collection c)");
		}
	}

	@Override
	public @NotNull <T> T @NotNull [] toArray(@NotNull T[] a) {
		debugger.debugHead("toArray(T[] a)");
		try {
			return super.toArray(a);
		} finally {
			debugger.debugFinally("toArray(T[] a)");
		}
	}
}

package fun.qu_an.minecraft.asyncparticles.client.util;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class ThreadedCustomEvictingDeque<T> implements Deque<T> {
	private final ArrayDeque<T> delegate;
	private final Thread initThread;
	private final Consumer<Runnable> executor;
	private final Consumer<T> evictedCallback;
	private final int maxCapacity;

	public ThreadedCustomEvictingDeque(int maxCapacity, Consumer<Runnable> executor, Consumer<T> evictedCallback) {
		this.maxCapacity = maxCapacity;
		this.delegate = new ArrayDeque<>(maxCapacity);
		this.initThread = Thread.currentThread();
		this.executor = executor;
		this.evictedCallback = evictedCallback;
	}

	public ThreadedCustomEvictingDeque(int maxCapacity, Collection<? extends T> c, Consumer<Runnable> executor, Consumer<T> evictedCallback) {
		this.maxCapacity = maxCapacity;
		this.delegate = new ArrayDeque<>(maxCapacity);
		this.initThread = Thread.currentThread();
		this.delegate.addAll(c);
		this.executor = executor;
		this.evictedCallback = evictedCallback;
	}

	public ArrayDeque<T> delegate() {
		return delegate;
	}

	@Override
	public void addFirst(T t) {
		if (Thread.currentThread() == initThread) {
			while (delegate.size() >= maxCapacity) {
				evictedCallback.accept(delegate.removeLast());
			}
			delegate.addFirst(t);
		}
		executor.accept(() -> {
			while (delegate.size() >= maxCapacity) {
				evictedCallback.accept(delegate.removeLast());
			}
			delegate.addFirst(t);
		});
	}

	@Override
	public void addLast(T t) {
		if (Thread.currentThread() == initThread) {
			while (delegate.size() >= maxCapacity) {
				evictedCallback.accept(delegate.removeFirst());
			}
			delegate.addLast(t);
		}
		executor.accept(() -> {
			while (delegate.size() >= maxCapacity) {
				evictedCallback.accept(delegate.removeFirst());
			}
			delegate.addLast(t);
		});
	}

	@Override
	public boolean offerFirst(T t) {
		addFirst(t);
		return true;
	}

	@Override
	public boolean offerLast(T t) {
		addLast(t);
		return true;
	}

	@Override
	public T removeFirst() {
		if (Thread.currentThread() == initThread) {
			return delegate.removeFirst();
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public T removeLast() {
		if (Thread.currentThread() == initThread) {
			return delegate.removeLast();
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public T pollFirst() {
		if (Thread.currentThread() == initThread) {
			return delegate.pollFirst();
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public T pollLast() {
		if (Thread.currentThread() == initThread) {
			return delegate.pollLast();
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public T getFirst() {
		return delegate.getFirst();
	}

	@Override
	public T getLast() {
		return delegate.getLast();
	}

	@Override
	public T peekFirst() {
		return delegate.peekFirst();
	}

	@Override
	public T peekLast() {
		return delegate.peekLast();
	}

	@Override
	public boolean removeFirstOccurrence(Object o) {
		if (Thread.currentThread() == initThread) {
			return delegate.removeFirstOccurrence(o);
		}
		executor.accept(() -> delegate.removeFirstOccurrence(o));
		return false;
	}

	@Override
	public boolean removeLastOccurrence(Object o) {
		if (Thread.currentThread() == initThread) {
			return delegate.removeLastOccurrence(o);
		}
		executor.accept(() -> delegate.removeLastOccurrence(o));
		return false;
	}

	@Override
	public boolean add(T t) {
		addLast(t);
		return true;
	}

	@Override
	public boolean offer(T t) {
		addLast(t);
		return true;
	}

	@Override
	public T remove() {
		return removeFirst();
	}

	@Override
	public T poll() {
		return isEmpty() ? null : removeFirst();
	}

	@Override
	public T element() {
		return delegate.element();
	}

	@Override
	public T peek() {
		return delegate.peek();
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		if (Thread.currentThread() == initThread) {
			return delegate.addAll(c);
		}
		executor.accept(() -> delegate.addAll(c));
		return true;
	}

	@Override
	public boolean removeAll(@NotNull Collection<?> c) {
		if (Thread.currentThread() == initThread) {
			return delegate.removeAll(c);
		}
		executor.accept(() -> delegate.removeAll(c));
		return true;
	}

	@Override
	public boolean retainAll(@NotNull Collection<?> c) {
		if (Thread.currentThread() == initThread) {
			return delegate.retainAll(c);
		}
		executor.accept(() -> delegate.retainAll(c));
		return true;
	}

	@Override
	public void clear() {
		if (Thread.currentThread() == initThread) {
			delegate.clear();
		}
		executor.accept(delegate::clear);
	}

	@Override
	public void push(T t) {
		addFirst(t);
	}

	@Override
	public T pop() {
		return removeFirst();
	}

	@Override
	public boolean remove(Object o) {
		if (Thread.currentThread() == initThread) {
			return delegate.remove(o);
		}
		executor.accept(() -> delegate.remove(o));
		return true;
	}

	@Override
	public boolean containsAll(@NotNull Collection<?> c) {
		return delegate.containsAll(c);
	}

	@Override
	public boolean contains(Object o) {
		return delegate.contains(o);
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public @NotNull Iterator<T> iterator() {
		if (Thread.currentThread() == initThread) {
			return delegate.iterator();
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public Object @NotNull [] toArray() {
		return delegate.toArray();
	}

	@Override
	public @NotNull <T1> T1 @NotNull [] toArray(@NotNull T1 @NotNull [] a) {
		return delegate.toArray(a);
	}

	@Override
	public @NotNull Iterator<T> descendingIterator() {
		if (Thread.currentThread() == initThread) {
			return delegate.descendingIterator();
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public void forEach(Consumer<? super T> action) {
		if (Thread.currentThread() == initThread) {
			delegate.forEach(action);
		}
		executor.accept(() -> delegate.forEach(action));
	}

	public Consumer<Runnable> getExecutor() {
		return executor;
	}
}

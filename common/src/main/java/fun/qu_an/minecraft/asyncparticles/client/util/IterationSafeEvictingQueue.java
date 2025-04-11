package fun.qu_an.minecraft.asyncparticles.client.util;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class IterationSafeEvictingQueue<E> implements Queue<E> {
	protected Object[] queue;
	protected final int maxCapacity;
	protected final int maxCapacityPowerOfTwo;
	protected final Consumer<E> onEvict;
	protected int head;
	protected int size;

	public IterationSafeEvictingQueue(int initialCapacity, int maxCapacity) {
		this(initialCapacity, maxCapacity, e -> {
		});
	}

	public IterationSafeEvictingQueue(int initialCapacity, int maxCapacity, Consumer<E> onEvict) {
		if (initialCapacity <= 0 || maxCapacity <= 0 || initialCapacity > maxCapacity) {
			throw new IllegalArgumentException("Invalid capacities");
		}
		this.queue = new Object[roundUpToPowerOfTwo(initialCapacity)];
		this.maxCapacity = maxCapacity;
		this.maxCapacityPowerOfTwo = roundUpToPowerOfTwo(maxCapacity);
		this.onEvict = onEvict;
		this.head = 0;
		this.size = 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean offer(E item) {
		if (item == null) {
			throw new NullPointerException("Item cannot be null");
		}
		Object[] q = queue;
		int capacity = q.length;
		int size = this.size;
		if (size >= maxCapacity) {
			// Remove the oldest element
			int head = this.head;
			this.head = (head + 1) & (capacity - 1);
			E evicted = (E) q[head];
			if (evicted != null) {
				onEvict.accept(evicted);
			}
//			q[head] = item; // head is now tail
			q[head] = null;
			q[head + size & (capacity - 1)] = item;
		} else {
			if (capacity == size) {
				q = resize(capacity = capacity << 1); // update capacity and q
			}
			q[(head + size) & (capacity - 1)] = item;
			this.size++;
		}
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public E poll() {
		if (size == 0) {
			return null;
		}
		Object[] q = queue;
		int head = this.head;
		E item = (E) q[head];
		this.head = (head + 1) & (q.length - 1);
		q[head] = null;
		size--;
		return item;
	}

	@Override
	@SuppressWarnings("unchecked")
	public E peek() {
		if (size == 0) {
			return null;
		}
		Object o;
		// thread safety
		while ((o = queue[head]) == null) {
			if (size == 0) {
				return null;
			}
		}
		return (E) o;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

//	@Override
//	public @NotNull Spliterator<E> spliterator() {
//		// FIXME: implement a Spliterator
//		throw new UnsupportedOperationException();
//	}

	private Object[] resize(int newCapacity) {
		if (newCapacity > this.maxCapacityPowerOfTwo) {
			throw new IllegalStateException("Cannot increase capacity beyond max capacity");
		}
		Object[] q = this.queue;
		int head = this.head;
		int tail = head + this.size;
		int capacity = q.length;
		Object[] a = new Object[newCapacity];
		if (tail <= capacity) {
			System.arraycopy(q, head, a, head, this.size);
		} else {
			int l = capacity - head;
			System.arraycopy(q, head, a, 0, l);
			System.arraycopy(q, 0, a, l, tail - capacity);
		}
		this.queue = a;
		this.head = 0;
		return a;
	}

	public int arraySize() {
		return queue.length;
	}

	@Override
	public @NotNull Iterator<E> iterator() {
		return new QueueIterator();
	}

	private class QueueIterator implements Iterator<E> {
		private final Object[] a = queue;
		private final int mask = a.length - 1;
		private final int head = IterationSafeEvictingQueue.this.head;
		private int tail = IterationSafeEvictingQueue.this.size + head;
		private int cursor = head;
		private Object curr;
		private Object next;

		@Override
		public boolean hasNext() {
			if (next != null) {
				return true;
			}
			final Object e = curr;
			while (cursor < tail) {
				next = a[cursor++ & mask];
				if (next != null && next != e) {
					return true;
				}
			}
			return false;
		}

		@Override
		@SuppressWarnings("unchecked")
		public E next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			Object next = this.next;
			this.next = null;
			return (E) (curr = next);
		}

		/**
		 * NOTE: This method is not thread-safe and should not be used concurrently.
		 */
		@Override
		public void remove() {
			if (curr == null) {
				throw new IllegalStateException();
			}
			int i = cursor - 1;
			while (i >= head && a[i & mask] != curr) {
				--i;
			}
			if (i < 0) {
				throw new IllegalStateException();
			}
			IterationSafeEvictingQueue.this.removeIndex(a, i, tail);
			tail--;
			curr = null;
		}
	}

	@Override
	public boolean add(E e) {
		return offer(e);
	}

	@Override
	public E remove() {
		E item = poll();
		if (item == null) {
			throw new NoSuchElementException("Queue is empty");
		}
		return item;
	}

	@Override
	public E element() {
		E item = peek();
		if (item == null) {
			throw new NoSuchElementException("Queue is empty");
		}
		return item;
	}

	@Override
	public boolean containsAll(@NotNull Collection<?> c) {
		for (Object e : c) {
			if (!contains(e)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean addAll(@NotNull Collection<? extends E> c) {
		// TODO: optimize this method
		if (c.isEmpty()) {
			return false;
		}
		for (E e : c) {
			add(e);
		}
		return true;
	}

	@Override
	public boolean removeAll(@NotNull Collection<?> c) {
		return removeIf(c::contains);
	}

	@Override
	public boolean retainAll(@NotNull Collection<?> c) {
		return removeIf(e -> !c.contains(e));
	}

	@SuppressWarnings("unchecked")
	public boolean removeIf(@NotNull Predicate<? super E> filter) {
		final Object[] a = this.queue;
		final int mask = a.length - 1;
		int i = head;
		int to = size + i;
		for (; i < to; i++) {
			if (filter.test((E) a[i & mask])) {
				break;
			}
		}
		if (i == to) {
			return false;
		}
		final Object[] b = new Object[a.length];
		if (i > a.length) { // Copy the elements before the first removed one.
			System.arraycopy(a, head, b, head, a.length - head);
			System.arraycopy(a, 0, b, 0, i - a.length);
		} else {
			System.arraycopy(a, head, b, head, i - head);
		}
		int j = i++; // Index of the next element to copy.
		for (; i < to; i++) {
			E e = (E) a[i & mask];
			if (!filter.test(e)) {
				b[j++ & mask] = e;
			}
		}
		this.queue = b;
		this.size = j - head;
		return true;
	}

	private void removeIndex(Object[] q, int toRemove, int to) {
		int l = to - toRemove;
		if (l > 0) {
			if (to <= q.length) {
				System.arraycopy(q, toRemove + 1, q, toRemove, l);
			} else {
				System.arraycopy(q, toRemove, q, toRemove + 1, q.length - toRemove);
				q[q.length - 1] = q[0];
				System.arraycopy(q, 1, q, 0, head + size - q.length);
			}
		}
		q[to - 1 & q.length - 1] = null;
		size--;
	}

	@Override
	public void clear() {
		Object[] q = queue;
		int head = this.head;
		int tail = head + this.size;
		int capacity = q.length;
		if (tail <= capacity) {
			Arrays.fill(q, head, tail, null);
		} else {
			Arrays.fill(q, head, capacity, null);
			Arrays.fill(q, 0, tail - capacity, null);
		}
		this.head = 0;
		this.size = 0;
	}

	@Override
	public boolean contains(Object o) {
		if (o == null) {
			return false;
		}
		Object[] q = queue;
		int mask = q.length - 1;
		for (int i = head, to = i + size; i < to; i++) {
			if (o.equals(q[i & mask])) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Object @NotNull [] toArray() {
		return this.toArray(new Object[size]);
	}

	@Override
	@SuppressWarnings({"unchecked", "SuspiciousSystemArraycopy"})
	public <T> T @NotNull [] toArray(T[] a) {
		int size = this.size;
		if (size > a.length) {
			a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
		}
		Object[] q = queue;
		int head = this.head;
		int tail = head + this.size;
		int capacity = q.length;
		if (tail <= capacity) {
			System.arraycopy(q, head, a, 0, this.size);
		} else {
			int l = capacity - head;
			System.arraycopy(q, head, a, 0, l);
			System.arraycopy(q, 0, a, l, tail - capacity);
		}
		if (size < a.length) {
			a[size] = null;
		}
		return a;
	}

	@Override
	public boolean remove(Object o) {
		if (o == null) {
			return false;
		}
		Object[] q = queue;
		int capacity = q.length;
		for (int i = this.head, to = this.size + i; i < to; i++) {
			int index = i & (capacity - 1);
			if (!o.equals(q[index])) {
				continue;
			}
			// shift to left
			removeIndex(q, i, to);
			return true;
		}
		return false;
	}

	private static int roundUpToPowerOfTwo(int n) {
		if (n <= 0) {
			throw new IllegalArgumentException("n must be positive");
		}
		n--;
		n |= n >> 1;
		n |= n >> 2;
		n |= n >> 4;
		n |= n >> 8;
		n |= n >> 16;
		return n + 1;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		Iterator<E> it = iterator();
		while (it.hasNext()) {
			sb.append(it.next());
			if (it.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append(']');
		return sb.toString();
	}
}

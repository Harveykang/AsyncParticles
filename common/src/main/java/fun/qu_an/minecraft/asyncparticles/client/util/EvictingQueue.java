package fun.qu_an.minecraft.asyncparticles.client.util;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.function.Predicate;

public class EvictingQueue<E> implements Queue<E> {
	protected Object[] queue;
	protected int head;
	protected int size;
	protected final int maxCapacity;

	public EvictingQueue(int initialCapacity, int maxCapacity) {
		if (initialCapacity <= 0 || maxCapacity <= 0 || initialCapacity > maxCapacity) {
			throw new IllegalArgumentException("Invalid capacities");
		}
		this.maxCapacity = roundUpToPowerOfTwo(maxCapacity);
		this.queue = new Object[roundUpToPowerOfTwo(initialCapacity)];
		this.head = 0;
		this.size = 0;
	}

	@Override
	public boolean offer(E item) {
		if (item == null) {
			throw new NullPointerException("Item cannot be null");
		}
		Object[] q = queue;
		int capacity = q.length;
		int size = this.size;
		if (capacity == size) {
			if (capacity >= maxCapacity) {
				// Remove the oldest element
				int head = this.head;
				this.head = (head + 1) & (capacity - 1);
				q[head] = item; // head is now tail
				return true;
			}
			resize(capacity << 1);
		}
		q[(head + size) & (capacity - 1)] = item;
		this.size++;
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public E poll() {
		if (size == 0) {
			return null;
		}
		E item = (E) queue[head];
		queue[head] = null;
		head = (head + 1) & (queue.length - 1);
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

	@Override
	public @NotNull Iterator<E> iterator() {
		return new QueueIterator();
	}

	private void resize(int newCapacity) {
		if (newCapacity > this.maxCapacity) {
			throw new IllegalStateException("Cannot increase capacity beyond max capacity");
		}
		Object[] q = this.queue;
		int head = this.head;
		int tail = head + this.size;
		int capacity = q.length;
		Object[] a = new Object[newCapacity];
		// We don't organize the array,
		// but we keep the position of the elements relative to the head and tail,
		// which ensures that the other threads are correct when iterating
		if (tail <= capacity) {
			System.arraycopy(q, head, a, head, this.size);
		} else {
			int l = capacity - head;
			System.arraycopy(q, 0, a, 0, tail - capacity);
			System.arraycopy(q, head, a, head + newCapacity - capacity, l);
		}
		this.queue = a;
		this.head = 0;
	}

	private class QueueIterator implements Iterator<E> {
		private final Object[] queueSnapshot;
		private final int snapshotHead;
		private final int snapshotSize;
		private int currentIndex;
		private Object next;

		public QueueIterator() {
			this.queueSnapshot = queue;
			this.snapshotHead = head;
			this.snapshotSize = size;
			this.currentIndex = 0;
		}

		@Override
		public boolean hasNext() {
			if (next != null) {
				return true;
			}
			if (currentIndex >= snapshotSize) {
				return false;
			}
			int index = (snapshotHead + currentIndex) & (queueSnapshot.length - 1);
			++currentIndex;
			next = queueSnapshot[index];
			return next != null;
		}

		@Override
		@SuppressWarnings("unchecked")
		public E next() {
			E next = (E) this.next;
			if (next == null) {
				throw new NoSuchElementException();
			}
			this.next = null;
			return next;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
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
		Object[] q = queue;
		int head = this.head;
		int size = this.size;
		int mask = q.length - 1;
		for (int i = head; i < size; i++) {
			Object o = q[i & mask];
			if (o == null) {
				// thread safety
				continue;
			}
			if (!c.contains(o)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean addAll(@NotNull Collection<? extends E> c) {
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
	public boolean removeIf(@NotNull Predicate<? super E> condition) {
		Object[] q = queue;
		for (int i = this.head, to = i + this.size; i < to; i++) {
			Object o = q[i & (q.length - 1)];
			if (o == null) {
				continue;
			}
			if (condition.test((E) o)) {
				removeModified(condition, q, i, to);
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private void removeModified(Predicate<? super E> condition, Object[] q, int from, int to) {
		// assert from should be removed
		int mask = q.length - 1;
		BitSet bits = new BitSet(to - from - 1);
		for (int i = from + 1; i < to; i++) {
			if (condition.test((E) q[i & mask])) {
				bits.set(i - from - 1);
			}
		}
		if (bits.isEmpty()) {
			removeIndex(q, from, to);
			return;
		}
		for (int write = from, read = from + 1; read < to; read++) {
			if (!bits.get(read - from - 1)) {
				q[write++ & mask] = q[read & mask];
			}
		}
		q[to - 1 & mask] = null;
	}

	private void removeIndex(Object[] q, int toRemove, int to) {
		--to;
		int mask = q.length - 1;
		for (int j = toRemove; j < to; j++) {
			q[j & mask] = q[j + 1 & mask];
		}
		q[to & mask] = null;
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
		for (int i = this.head, size = this.size; i < size; i++) {
			if (o.equals(q[i & (q.length - 1)])) {
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
			a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
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

	public static int sub(int i, int j, int modulus) {
		if ((i -= j) < 0) i += modulus;
		return i;
	}

	@Override
	public boolean remove(Object o) {
		if (o == null) {
			return false;
		}
		Object[] q = queue;
		int capacity = q.length;
		for (int i = this.head, to = this.size + i; i < to; i++) {
			int index = (head + i) & (capacity - 1);
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

	public static void main(String[] args) {
		EvictingQueue<Integer> queue = new EvictingQueue<>(2, 5);

		queue.offer(1);
		queue.offer(2);
		System.out.println(queue.peek()); // 输出: 1

		queue.offer(3); // 不需要扩容
		System.out.println(queue.peek()); // 输出: 1

		queue.offer(4); // 扩容到 4
		queue.offer(5); // 扩容到 8（超过最大容量 5，保持为 5）
		queue.offer(6); // 移除 1
		System.out.println(queue.peek()); // 输出: 2

		queue.poll(); // 移除 2
		queue.poll(); // 移除 3
		try {
			queue.remove(); // 移除 4
		} catch (NoSuchElementException e) {
			System.err.println(e.getMessage());
		}

		// 示例迭代器用法
		for (Integer num : queue) {
			System.out.println(num);
		}

		// 测试其他方法
		queue.add(7);
		queue.add(8);
		System.out.println(queue.contains(7)); // 输出: true
		System.out.println(queue.contains(9)); // 输出: false

		Collection<Integer> collection = List.of(7, 8);
		System.out.println(queue.containsAll(collection)); // 输出: true

		queue.addAll(List.of(9, 10));
		System.out.println(queue.size()); // 输出: 5

		queue.removeAll(List.of(7, 8));
		System.out.println(queue.size()); // 输出: 3

		queue.retainAll(List.of(9));
		System.out.println(queue.size()); // 输出: 1

		queue.clear();
		System.out.println(queue.isEmpty()); // 输出: true

		// 测试 remove(Object o)
		queue.offer(1);
		queue.offer(2);
		queue.offer(3);
		System.out.println(queue.remove((Object) 2)); // 输出: true
		System.out.println(queue.size()); // 输出: 2
		System.out.println(queue.peek()); // 输出: 1
	}
}




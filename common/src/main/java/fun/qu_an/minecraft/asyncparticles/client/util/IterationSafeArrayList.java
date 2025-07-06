package fun.qu_an.minecraft.asyncparticles.client.util;

import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Predicate;

public class IterationSafeArrayList<E> extends ObjectArrayList<E> {
	/**
	 * @apiNote This list does not allow the same object to be adjacent elements, which can cause issues with iteration.
	 */
	public IterationSafeArrayList() {
		super();
	}

	/**
	 * @apiNote This list does not allow the same object to be adjacent elements, which can cause issues with iteration.
	 */
	public IterationSafeArrayList(Collection<E> c) {
		super(c);
	}

	/**
	 * @apiNote This list does not allow the same object to be adjacent elements, which can cause issues with iteration.
	 */
	public IterationSafeArrayList(ObjectCollection<E> c) {
		super(c);
	}

	/**
	 * @apiNote This list does not allow the same object to be adjacent elements, which can cause issues with iteration.
	 */
	public IterationSafeArrayList(int i) {
		super(i);
	}

	protected IterationSafeArrayList(E[] a, @SuppressWarnings("unused") boolean wrapped) {
		super(a, wrapped);
	}

	/**
	 * @apiNote This list does not allow the same object to be adjacent elements, which can cause issues with iteration.
	 */
	public static <K> IterationSafeArrayList<K> wrap(final K[] a, final int length) {
		if (length > a.length) throw new IllegalArgumentException("The specified length (" + length + ") is greater than the array size (" + a.length + ")");
		final IterationSafeArrayList<K> l = new IterationSafeArrayList<>(a, true);
		l.size = length;
		return l;
	}

	/**
	 * @apiNote This list does not allow the same object to be adjacent elements, which can cause issues with iteration.
	 */
	public static <K> IterationSafeArrayList<K> wrap(final K[] a) {
		return wrap(a, a.length);
	}

	/**
	 * @apiNote This list does not allow the same object to be adjacent elements, which can cause issues with iteration.
	 */
	public static <K> IterationSafeArrayList<K> of() {
		return new IterationSafeArrayList<>();
	}

	/**
	 * @apiNote This list does not allow the same object to be adjacent elements, which can cause issues with iteration.
	 */
	@SafeVarargs
	public static <K> IterationSafeArrayList<K> of(final K... init) {
		return wrap(init);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void removeElements(final int from, final int to) {
		it.unimi.dsi.fastutil.Arrays.ensureFromTo(size, from, to);
		if (to != size) {
			final E[] b = (E[]) Array.newInstance(a.getClass().getComponentType(), a.length);
			System.arraycopy(a, 0, b, 0, from);
			System.arraycopy(a, to, b, from, size - to);
			this.a = b;
		} else {
			Arrays.fill(a, from, to, null);
		}
		size -= (to - from);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addElements(final int index, final E[] a, final int offset, final int length) {
		ensureIndex(index);
		ObjectArrays.ensureOffsetLength(a, offset, length);
		final E[] b;
		boolean addToTail = index == size;
		if (addToTail) {
			b = this.a;
		} else {
			b = (E[]) Array.newInstance(a.getClass().getComponentType(), a.length);
			System.arraycopy(this.a, 0, b, 0, index);
			System.arraycopy(this.a, index, b, index + length, size - index);
		}
		System.arraycopy(a, offset, b, index, length);
		size += length;
		if (!addToTail) {
			this.a = b;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean addAll(int index, final Collection<? extends E> c) {
		if (c instanceof ObjectList) {
			return addAll(index, (ObjectList<? extends E>) c);
		}
		ensureIndex(index);
		int n = c.size();
		if (n == 0) return false;
		final E[] b;
		boolean addToTail = index == size;
		if (addToTail) {
			b = this.a;
		} else {
			b = (E[]) Array.newInstance(a.getClass().getComponentType(), Math.max(a.length, size + n));
			System.arraycopy(a, 0, b, 0, index);
			System.arraycopy(a, index, b, index + n, size - index);
		}
		final Iterator<? extends E> i = c.iterator();
		size += n;
		while (n-- != 0) b[index++] = i.next();
		assert size <= b.length;
		if (!addToTail) {
			this.a = b;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean addAll(final int index, final ObjectList<? extends E> l) {
		ensureIndex(index);
		final int n = l.size();
		if (n == 0) return false;
		final E[] b;
		boolean addToTail = index == size;
		if (addToTail) {
			b = this.a;
		} else {
			b = (E[]) Array.newInstance(a.getClass().getComponentType(), Math.max(a.length, size + n));
			System.arraycopy(a, 0, b, 0, index);
			System.arraycopy(a, index, b, index + n, size - index);
		}
		l.getElements(0, b, index, n);
		size += n;
		assert size <= b.length;
		if (!addToTail) {
			this.a = b;
		}
		return true;
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		return removeIf(c::contains);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean removeIf(final @NotNull Predicate<? super E> filter) {
		final E[] a = this.a;
		int i = 0;
		int size = this.size;
		for (; i < size; i++) {
			if (filter.test(a[i])) {
				break;
			}
		}
		if (i == size) {
			return false;
		}
		final E[] b = (E[]) Array.newInstance(a.getClass().getComponentType(), a.length);
		System.arraycopy(a, 0, b, 0, i); // Copy the elements before the first removed one.
		int j = i++; // Index of the next element to copy.
		for (; i < size; i++) {
			E e = a[i];
			if (!filter.test(e)) {
				b[j++] = e;
			}
		}
		this.a = b;
		this.size = j;
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public @NotNull ObjectListIterator<E> listIterator(int i) {
		int s = size();
		if (i < 0 || i > s) {
			// FIXME: Thread-safe issue if i != 0
			throw new IndexOutOfBoundsException("Index: " + i + ", Size: " + s);
		}
		if (s == 0) {
			return ObjectIterators.EMPTY_ITERATOR;
		}
		return new ListItr(a, s, i);
	}

	@Override
	public Object @NotNull [] toArray() {
		E[] es = this.a;
		int size = Math.min(es.length, size());
		return Arrays.copyOf(es, size, Object[].class);
	}

	@SuppressWarnings({"unchecked", "SuspiciousSystemArraycopy"})
	@Override
	public <K> K @NotNull [] toArray(K[] a) {
		E[] es = this.a;
		int size = Math.min(es.length, size());
		if (a == null) {
			a = (K[]) new Object[size];
		} else if (a.length < size) {
			a = (K[]) Array.newInstance(a.getClass().getComponentType(), size);
		}
		System.arraycopy(es, 0, a, 0, size);
		if (a.length > size) {
			a[size] = null;
		}
		return a;
	}

	private class ListItr implements ObjectListIterator<E> {
		private final E[] a;
		private int size;
		private int cursor;
		private int index;
		private E curr;
		private E prev;
		private E next;

		public ListItr(E[] es, int size, int i) {
			a = es;
			this.size = size;
			cursor = i;
			index = i;
		}

		@Override
		public E previous() {
			if (!hasPrevious()) {
				throw new NoSuchElementException();
			}
			E prev = this.prev;
			this.next = null;
			this.prev = null;
			--index;
			return curr = prev;
		}

		@Override
		public boolean hasPrevious() {
			if (prev != null) {
				return true;
			}
			final E e = curr;
			while (cursor > 0) {
				if ((prev = a[--cursor]) != null && prev != e) {
					return true;
				}
			}
			prev = null;
			return false;
		}

		@Override
		public int nextIndex() {
			return index;
		}

		@Override
		public int previousIndex() {
			return index - 1;
		}

		@Override
		public boolean hasNext() {
			if (next != null) {
				return true;
			}
			final E e = curr;
			while (cursor < size) {
				if ((next = a[cursor++]) != null && next != e) {
					return true;
				}
			}
			next = null;
			return false;
		}

		@Override
		public E next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			E next = this.next;
			this.next = null;
			this.prev = null;
			++index;
			return curr = next;
		}

		/**
		 * NOTE: This method is not thread-safe and should not be used concurrently.
		 */
		@Override
		public void remove() {
			// 实现删除元素，不抛异常
			if (curr == null) {
				throw new IllegalStateException();
			}
			int i = cursor; // Do not subtract 1, as if we call previous() first, the cursor has already decremented.
			while (i >= 0 && a[i] != curr) {
				--i;
			}
			if (i < 0) {
				throw new IllegalStateException();
			}
			IterationSafeArrayList.this.remove(i);
			--size;
			curr = null;
		}
	}
}

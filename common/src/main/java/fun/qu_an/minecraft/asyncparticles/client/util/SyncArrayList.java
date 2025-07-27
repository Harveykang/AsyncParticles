package fun.qu_an.minecraft.asyncparticles.client.util;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.*;

public class SyncArrayList<E> extends ArrayList<E> {
	@Serial
	private static final long serialVersionUID = 4642872362798331634L;
	private final Object lock = new Object();

	public SyncArrayList(int initialCapacity) {
		super(initialCapacity);
	}

	public SyncArrayList() {
		super();
	}

	public SyncArrayList(Collection<? extends E> c) {
		super(c);
	}

	public void trimToSize() {
		synchronized (lock) {
			super.trimToSize();
		}
	}

	public void ensureCapacity(int minCapacity) {
		synchronized (lock) {
			super.ensureCapacity(minCapacity);
		}
	}

	public int indexOf(Object o) {
		synchronized (lock) {
			return super.indexOf(o);
		}
	}

	public int lastIndexOf(Object o) {
		synchronized (lock) {
			return super.lastIndexOf(o);
		}
	}

	public Object clone() {
		synchronized (lock) {
			return super.clone();
		}
	}

	public Object @NotNull [] toArray() {
		synchronized (lock) {
			return super.toArray();
		}
	}

	public <T> T @NotNull [] toArray(T[] a) {
		synchronized (lock) {
			return super.toArray(a);
		}
	}

	public E get(int index) {
		synchronized (lock) {
			return super.get(index);
		}
	}

	public E getFirst() {
		synchronized (lock) {
			return super.getFirst();
		}
	}

	public E getLast() {
		synchronized (lock) {
			return super.getLast();
		}
	}

	public E set(int index, E element) {
		synchronized (lock) {
			return super.set(index, element);
		}
	}

	public boolean add(E e) {
		synchronized (lock) {
			return super.add(e);
		}
	}

	public void add(int index, E element) {
		synchronized (lock) {
			super.add(index, element);
		}
	}

	public E remove(int index) {
		synchronized (lock) {
			return super.remove(index);
		}
	}

	public E removeFirst() {
		synchronized (lock) {
			return super.removeFirst();
		}
	}

	public E removeLast() {
		synchronized (lock) {
			return super.removeLast();
		}
	}

	public boolean equals(Object o) {
		synchronized (lock) {
			return super.equals(o);
		}
	}

	public int hashCode() {
		synchronized (lock) {
			return super.hashCode();
		}
	}

	public boolean remove(Object o) {
		synchronized (lock) {
			return super.remove(o);
		}
	}

	public void clear() {
		synchronized (lock) {
			super.clear();
		}
	}

	public boolean addAll(Collection<? extends E> c) {
		synchronized (lock) {
			return super.addAll(c);
		}
	}

	public boolean addAll(int index, Collection<? extends E> c) {
		synchronized (lock) {
			return super.addAll(index, c);
		}
	}

	public boolean removeAll(Collection<?> c) {
		synchronized (lock) {
			return super.removeAll(c);
		}
	}

	public boolean retainAll(Collection<?> c) {
		synchronized (lock) {
			return super.retainAll(c);
		}
	}
}

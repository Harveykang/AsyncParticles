package fun.qu_an.minecraft.asyncparticles.client.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterators;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.NoSuchElementException;

public class IterationSafeArrayList<T> extends ObjectArrayList<T> {
	public IterationSafeArrayList() {
	}

	public IterationSafeArrayList(Collection<T> c) {
		super(c);
	}

	@SuppressWarnings("unchecked")
	@Override
	public @NotNull ObjectListIterator<T> listIterator(int i) {
		final T[] es = elements();
		if (es.length == 0) {
			return ObjectIterators.EMPTY_ITERATOR;
		}
		int s = size();
		if (i < 0 || i > s) {
			throw new IndexOutOfBoundsException("Index: " + i + ", Size: " + s);
		}
		return new ObjectListIterator<>() {
			private final T[] a = es;
			private final int size = s;
			private int cursor = i;
			private int index = i;
			private T curr;
			private T prev;
			private T next;

			@Override
			public T previous() {
				if (!hasPrevious()) {
					throw new NoSuchElementException();
				}
				T prev = this.prev;
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
				final T t = curr;
				while (--cursor > 0) {
					prev = a[cursor];
					if (prev != null && prev != t) {
						return true;
					}
				}
				return false;
			}

			@Override
			public int nextIndex() {
				return index + 1;
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
				final T t = curr;
				while (cursor < size) {
					next = a[cursor++];
					if (next != null && next != t) {
						return true;
					}
				}
				return false;
			}

			@Override
			public T next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				T next = this.next;
				this.next = null;
				this.prev = null;
				++index;
				return curr = next;
			}
		};
	}

	public static void main(String[] args) throws InterruptedException {
		IterationSafeArrayList<Integer> list = new IterationSafeArrayList<>();
		Thread thread0 = new Thread(() -> {
			for (int i = 0; i < 1000000; i++) {
				list.add(i);
			}
		});
		thread0.setDaemon(true);
		thread0.start();
		Thread.sleep(1);
		// 多线程同时遍历测试
		Thread[] threads = new Thread[10];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(() -> {
				int j = 0;
				int prev = -1;
				for (Integer integer : list) {
					if (integer - prev != 1) {
						if (prev == integer) {
							throw new RuntimeException("Duplicated element: " + integer);
						} else {
							throw new RuntimeException("Element: " + integer + "and element: " + prev + "not consecutive");
						}
					}
					prev = integer;
					if (j++ % 77 == 76) {
						System.out.println(integer);
					}
				}
			});
		}
		for (Thread thread : threads) {
			thread.setDaemon(true);
			thread.start();
		}
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("ok");
	}
}

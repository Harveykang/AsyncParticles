package fun.qu_an.minecraft.asyncparticles.client.util;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public class CombinedIteration {
	public static <T> Iterable<T> of(Iterable<T> left, Iterable<T> right) {
		return new Iterable<>() {
			@Override
			public @NotNull Iterator<T> iterator() {
				return new Iterator<>() {
					private final Iterator<T> l = left.iterator();
					private final Iterator<T> r = right.iterator();
					private boolean isLeft;

					@Override
					public boolean hasNext() {
						return (isLeft && (isLeft = l.hasNext())) || r.hasNext();
					}

					@Override
					public T next() {
						return isLeft ? l.next() : r.next();
					}
				};
			}
		};
	}

	public static <T> Iterable<T> ofSet(Set<T> left, Set<T> right) {
		return new Iterable<>() {
			@Override
			public @NotNull Iterator<T> iterator() {
				return new Iterator<>() {
					private final Iterator<T> l = left.iterator();
					private final Iterator<T> r = right.iterator();
					private boolean isLeft = true;
					private T nextR;

					@Override
					public boolean hasNext() {
						if (isLeft && (isLeft = l.hasNext())) {
							return true;
						}
						while (r.hasNext()) {
							if (!left.contains(nextR = r.next())) {
								return true;
							}
						}
						nextR = null;
						return false;
					}

					@Override
					public T next() {
						if (isLeft) {
							return l.next();
						}
						if (nextR == null) {
							throw new NoSuchElementException();
						}
						return nextR;
					}
				};
			}
		};
	}
}

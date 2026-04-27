package fun.qu_an.minecraft.asyncparticles.client.util;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public record CombineIterable<T>(Iterable<T> left, Iterable<T> right) implements Iterable<T> {
	@Override
	public @NotNull Iterator<T> iterator() {
		return new Iterator<>() {
			private final Iterator<T> left = CombineIterable.this.left.iterator();
			private final Iterator<T> right = CombineIterable.this.right.iterator();
			private boolean isLeft;

			@Override
			public boolean hasNext() {
				return (isLeft = left.hasNext()) || right.hasNext();
			}

			@Override
			public T next() {
				return isLeft ? left.next() : right.next();
			}
		};
	}
}

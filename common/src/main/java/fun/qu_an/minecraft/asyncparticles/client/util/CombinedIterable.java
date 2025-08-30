package fun.qu_an.minecraft.asyncparticles.client.util;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class CombinedIterable<T> implements Iterable<T>, Iterator<T> {
	public static <T> CombinedIterable<T> combine(Iterable<T> firstIterable, Iterable<T> secondIterable) {
		return new CombinedIterable<>(firstIterable, secondIterable);
	}

	private final Iterator<T> firstItr;
	private final Iterator<T> secondItr;
	private boolean first = true;

	public CombinedIterable(Iterable<T> renderOrder, Iterable<T> particleRenderTypes) {
		firstItr = renderOrder.iterator();
		secondItr = particleRenderTypes.iterator();
	}

	@Override
	public boolean hasNext() {
		if (!first) {
			return secondItr.hasNext();
		} else if (firstItr.hasNext()) {
			return true;
		} else {
			first = false;
			return secondItr.hasNext();
		}
	}

	@Override
	public T next() {
		if (first) {
			return firstItr.next();
		} else {
			return secondItr.next();
		}
	}

	@Override
	public @NotNull Iterator<T> iterator() {
		return this;
	}

	public boolean isFirst() {
		return first;
	}
}

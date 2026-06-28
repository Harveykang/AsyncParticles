package fun.qu_an.minecraft.asyncparticles.client.util;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Set;

public class CombinedIterable<T> implements Iterable<T> {
	private final Iterable<T> left;
	private final Iterable<T> right;

	public CombinedIterable(Iterable<T> left, Iterable<T> right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public @NotNull CombinedIterator<T> iterator() {
		return new CombinedIterator<>(left, right);
	}

	public static class CombinedIterator<T> implements Iterator<T> {
		private final Iterator<T> l;
		private final Iterator<T> r;
		private boolean isLeft = true;

		public CombinedIterator(Iterable<T> left, Iterable<T> right) {
			l = left.iterator();
			r = right.iterator();
		}

		@Override
		public boolean hasNext() {
			return (isLeft && (isLeft = l.hasNext())) || r.hasNext();
		}

		@Override
		public T next() {
			return isLeft ? l.next() : r.next();
		}

		public boolean isLeft() {
			return isLeft;
		}
	}

	public static <T> CombinedIterable<T> of(Iterable<T> left, Iterable<T> right) {
		return new CombinedIterable<>(left, right);
	}

	public static <T> CombinedIterator<T> ofIterator(Iterable<T> left, Iterable<T> right) {
		return new CombinedIterator<>(left, right);
	}

	public static <T> Set<T> ofSet(Set<T> left, Set<T> right) {
		int sizeL = left.size();
		if (sizeL == 0) {
			return right;
		}
		int sizeR = right.size();
		if (sizeR == 0) {
			return left;
		}
		Set<T> merged = new ObjectArraySet<>(sizeL + sizeR);
		merged.addAll(left);
		merged.addAll(right);
		return merged;
	}

	public static <T> Set<T> ofIdentitySet(Set<T> left, Set<T> right) {
		int sizeL = left.size();
		if (sizeL == 0) {
			return right;
		}
		int sizeR = right.size();
		if (sizeR == 0) {
			return left;
		}
		Set<T> merged = new ReferenceArraySet<>(sizeL + sizeR);
		merged.addAll(left);
		merged.addAll(right);
		return merged;
	}
}

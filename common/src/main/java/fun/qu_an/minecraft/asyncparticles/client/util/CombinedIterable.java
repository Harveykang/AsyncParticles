package fun.qu_an.minecraft.asyncparticles.client.util;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Set;

public class CombinedIterable {
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

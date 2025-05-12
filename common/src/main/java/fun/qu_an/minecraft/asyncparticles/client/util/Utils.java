package fun.qu_an.minecraft.asyncparticles.client.util;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class Utils {
	public static final Iterator<?> DUMMY_ITERATOR = new Iterator<>() {
		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Object next() {
			return null;
		}

		@Override
		public void remove() {
		}

		@Override
		public void forEachRemaining(Consumer<? super Object> action) {
		}
	};
	public static final CompletableFuture<Object> NULL_FUTURE = CompletableFuture.completedFuture(null);

	@SuppressWarnings("unchecked")
	public static <T> Iterator<T> dummyIterator() {
		return (Iterator<T>) DUMMY_ITERATOR;
	}

	@SuppressWarnings("unchecked")
	public static <T> CompletableFuture<T> nullFuture() {
		return (CompletableFuture<T>) NULL_FUTURE;
	}
}

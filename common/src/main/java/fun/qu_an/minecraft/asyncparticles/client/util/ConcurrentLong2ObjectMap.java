package fun.qu_an.minecraft.asyncparticles.client.util;

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.LongFunction;

public class ConcurrentLong2ObjectMap<V> extends AbstractLong2ObjectMap<V> implements Long2ObjectMap<V> {
	private final ConcurrentHashMap<Long, V> delegate = new ConcurrentHashMap<>();

	public ConcurrentLong2ObjectMap() {
	}

	public ConcurrentLong2ObjectMap(Long2ObjectMap<V> needModelDataRefresh) {
		putAll(needModelDataRefresh);
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public boolean containsValue(Object value) {
		return delegate.containsValue(value);
	}

	@Override
	public void putAll(@NotNull Map<? extends Long, ? extends V> m) {
		delegate.putAll(m);
	}

	@Override
	public ObjectSet<Entry<V>> long2ObjectEntrySet() {
		Set<Map.Entry<Long, V>> entrySet = delegate.entrySet();
		return new AbstractObjectSet<>() {
			@Override
			public @NotNull ObjectIterator<Entry<V>> iterator() {
				Iterator<Map.Entry<Long, V>> iterator = entrySet.iterator();
				return new AbstractObjectIterator<>() {
					@Override
					public boolean hasNext() {
						return iterator.hasNext();
					}

					@Override
					public Entry<V> next() {
						Map.Entry<Long, V> next = iterator.next();
						return new BasicEntry<>(next.getKey(), next.getValue());
					}
				};
			}

			@Override
			public int size() {
				return entrySet.size();
			}
		};
	}

	@Override
	public @NotNull LongSet keySet() {
		ConcurrentHashMap.KeySetView<Long, V> keySet = delegate.keySet();
		return new AbstractLongSet() {
			@Override
			public @NotNull LongIterator iterator() {
				Iterator<Long> iterator = keySet.iterator();
				return new AbstractLongIterator() {
					@Override
					public long nextLong() {
						return iterator.next();
					}

					@Override
					public boolean hasNext() {
						return iterator.hasNext();
					}
				};
			}

			@Override
			public int size() {
				return keySet.size();
			}
		};
	}

	@Override
	public @NotNull ObjectCollection<V> values() {
		Collection<V> values = delegate.values();
		return new AbstractObjectCollection<V>() {
			@Override
			public @NotNull ObjectIterator<V> iterator() {
				Iterator<V> iterator = values.iterator();
				return new AbstractObjectIterator<>() {
					@Override
					public boolean hasNext() {
						return iterator.hasNext();
					}

					@Override
					public V next() {
						return iterator.next();
					}
				};
			}

			@Override
			public int size() {
				return values.size();
			}
		};
	}

	@Override
	public V get(long key) {
		return delegate.getOrDefault(key, defaultReturnValue());
	}

	@Override
	public V getOrDefault(long key, V defaultValue) {
		return delegate.getOrDefault(key, defaultValue);
	}

	@Override
	public boolean containsKey(long key) {
		return delegate.containsKey(key);
	}

	@Override
	public V remove(long key) {
		V remove = delegate.remove(key);
		return remove == null? defaultReturnValue() : remove;
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public V putIfAbsent(long key, V value) {
		return delegate.putIfAbsent(key, value);
	}

	@Override
	public boolean replace(long key, V oldValue, V newValue) {
		return delegate.replace(key, oldValue, newValue);
	}

	@Override
	public V replace(long key, V value) {
		return delegate.replace(key, value);
	}

	@Override
	public V computeIfAbsent(long key, LongFunction<? extends V> mappingFunction) {
		return delegate.computeIfAbsent(key, mappingFunction::apply);
	}

	@Override
	public V computeIfAbsent(long key, Long2ObjectFunction<? extends V> mappingFunction) {
		return delegate.computeIfAbsent(key, mappingFunction::apply);
	}

	@Override
	public V computeIfPresent(final long key, final BiFunction<? super Long, ? super V, ? extends V> remappingFunction) {
		return delegate.computeIfPresent(key, remappingFunction);
	}

	@Override
	public V compute(final long key, final BiFunction<? super Long, ? super V, ? extends V> remappingFunction) {
		return delegate.compute(key, remappingFunction);
	}

	@Override
	public V merge(final long key, final V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		return delegate.merge(key, value, remappingFunction);
	}

	@Override
	public void forEach(final BiConsumer<? super Long, ? super V> consumer) {
		delegate.forEach(consumer);
	}
}

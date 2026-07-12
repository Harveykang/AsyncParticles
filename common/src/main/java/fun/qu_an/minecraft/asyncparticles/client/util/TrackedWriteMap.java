package fun.qu_an.minecraft.asyncparticles.client.util;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;

public class TrackedWriteMap<K, V> implements Map<K, V> {
	private final Runnable onWrite;
	private final Map<K, V> delegated;

	@SuppressWarnings("unchecked")
	public TrackedWriteMap(Runnable onWrite, Map<? extends K, ? extends V> delegated) {
		this.onWrite = onWrite;
		this.delegated = (Map<K, V>) delegated;
	}

	private void onWrite() {
		onWrite.run();
	}

	@Override
	public int size() {
		return delegated.size();
	}

	@Override
	public boolean isEmpty() {
		return delegated.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return delegated.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return delegated.containsValue(value);
	}

	@Override
	public V get(Object key) {
		return delegated.get(key);
	}

	@Override
	public V put(K key, V value) {
		onWrite();
		return delegated.put(key, value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		onWrite();
		delegated.putAll(m);
	}

	@Override
	public V remove(Object key) {
		onWrite();
		return delegated.remove(key);
	}

	@Override
	public void clear() {
		onWrite();
		delegated.clear();
	}

	@Override
	public @NotNull Set<K> keySet() {
		return delegated.keySet();
	}

	@Override
	public @NotNull Collection<V> values() {
		return delegated.values();
	}

	@Override
	public @NotNull Set<Entry<K, V>> entrySet() {
		return delegated.entrySet();
	}

	@Override
	public V putIfAbsent(K key, V value) {
		onWrite();
		return delegated.putIfAbsent(key, value);
	}

	@Override
	public boolean remove(Object key, Object value) {
		onWrite();
		return delegated.remove(key, value);
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		onWrite();
		return delegated.replace(key, oldValue, newValue);
	}

	@Override
	public V replace(K key, V value) {
		onWrite();
		return delegated.replace(key, value);
	}

	@Override
	public void replaceAll(java.util.function.BiFunction<? super K, ? super V, ? extends V> function) {
		onWrite();
		delegated.replaceAll(function);
	}

	@Override
	public V computeIfAbsent(K key, java.util.function.Function<? super K, ? extends V> mappingFunction) {
		onWrite();
		return delegated.computeIfAbsent(key, mappingFunction);
	}

	@Override
	public V computeIfPresent(K key, java.util.function.BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		onWrite();
		return delegated.computeIfPresent(key, remappingFunction);
	}

	@Override
	public V compute(K key, java.util.function.BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		onWrite();
		return delegated.compute(key, remappingFunction);
	}

	@Override
	public V merge(K key, V value, java.util.function.BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		onWrite();
		return delegated.merge(key, value, remappingFunction);
	}

	@Override
	public void forEach(BiConsumer<? super K, ? super V> action) {
		delegated.forEach(action);
	}

	@Override
	public String toString() {
		return "TrackedWriteHashMap{" +
			"onWrite=" + onWrite +
			", delegated=" + delegated +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		TrackedWriteMap<?, ?> that = (TrackedWriteMap<?, ?>) o;
		return Objects.equals(onWrite, that.onWrite) && Objects.equals(delegated, that.delegated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(onWrite, delegated);
	}
}

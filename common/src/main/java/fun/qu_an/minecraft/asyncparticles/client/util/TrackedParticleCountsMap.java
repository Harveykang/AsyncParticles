package fun.qu_an.minecraft.asyncparticles.client.util;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.particles.ParticleGroup;

public class TrackedParticleCountsMap extends Object2IntOpenHashMap<ParticleGroup> {
	private final SpinLock spinLock = new ReentrantSpinLock();

	@Override
	public boolean containsKey(Object key) {
		spinLock.lock();
		try {
			return super.containsKey(key);
		} finally {
			spinLock.unlock();
		}
	}

	@Override
	public boolean containsValue(Object key) {
		spinLock.lock();
		try {
			return super.containsValue(key);
		} finally {
			spinLock.unlock();
		}
	}

	@Override
	public boolean containsValue(int key) {
		spinLock.lock();
		try {
			return super.containsValue(key);
		} finally {
			spinLock.unlock();
		}
	}

	@Override
	public int addTo(ParticleGroup key, int value) {
		spinLock.lock();
		try {
			return super.addTo(key, value);
		} finally {
			spinLock.unlock();
		}
	}

	public int put(ParticleGroup k, int v) {
		spinLock.lock();
		try {
			return super.put(k, v);
		} finally {
			spinLock.unlock();
		}
	}

	@Override
	public Integer get(Object key) {
		spinLock.lock();
		try {
			return super.get(key);
		} finally {
			spinLock.unlock();
		}
	}

	@Override
	public int getInt(Object key) {
		spinLock.lock();
		try {
			return super.getInt(key);
		} finally {
			spinLock.unlock();
		}
	}

	@Override
	public void clear() {
		spinLock.lock();
		try {
			super.clear();
		} finally {
			spinLock.unlock();
		}
	}
}

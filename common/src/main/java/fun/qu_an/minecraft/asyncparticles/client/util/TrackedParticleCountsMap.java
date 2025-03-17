package fun.qu_an.minecraft.asyncparticles.client.util;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.particles.ParticleGroup;

public class TrackedParticleCountsMap extends Object2IntOpenHashMap<ParticleGroup> {
	private final SpinLock spinLock = new SpinLock();
	@Override
	public int addTo(ParticleGroup key, int value) {
		spinLock.lock();
		try {
			return super.addTo(key, value);
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
}

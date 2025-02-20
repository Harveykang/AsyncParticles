package fun.qu_an.minecraft.asyncparticles.client.util;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.particles.ParticleGroup;

public class TrackedParticleCountsMap extends Object2IntOpenHashMap<ParticleGroup> {
	@Override
	public synchronized int addTo(ParticleGroup key, int value) {
		return super.addTo(key, value);
	}

	@Override
	public synchronized int getInt(Object key) {
		return super.getInt(key);
	}
}

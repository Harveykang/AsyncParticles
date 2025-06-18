package fun.qu_an.minecraft.asyncparticles.client.compat.particlerain;

import java.util.concurrent.atomic.AtomicInteger;

public class ParticleRainCompat {
	public static final AtomicInteger asyncparticles$particleCount = new AtomicInteger(0);
	public static final AtomicInteger asyncparticles$fogCount = new AtomicInteger(0);
	public static void clearCounters() {
		asyncparticles$particleCount.set(0);
		asyncparticles$fogCount.set(0);
	}
}

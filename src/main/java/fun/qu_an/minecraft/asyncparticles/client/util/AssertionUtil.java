package fun.qu_an.minecraft.asyncparticles.client.util;

import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;

public class AssertionUtil {
	public static void assertNotParticleThread() {
		if (Thread.currentThread().getName().startsWith(AsyncRenderer.THREAD_PREFIX)) {
			throw new IllegalStateException("Cannot call this method from the particle thread");
		}
	}
}

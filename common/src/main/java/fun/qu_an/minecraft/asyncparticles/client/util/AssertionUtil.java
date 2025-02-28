package fun.qu_an.minecraft.asyncparticles.client.util;

import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;

public class AssertionUtil {
	public static void assertNotParticleRendererThread() {
		if (Thread.currentThread().getName().startsWith(AsyncRenderer.THREAD_PREFIX)) {
			throw new IllegalStateException("Cannot call this method from the particle renderer thread");
		}
	}

	public static void assertNotParticleTickerThread() {
		if (Thread.currentThread().getName().startsWith(AsyncTicker.THREAD_PREFIX)) {
			throw new IllegalStateException("Cannot call this method from the particle ticker thread");
		}
	}

	public static void assertParticleTickerThread() {
		if (!Thread.currentThread().getName().startsWith(AsyncTicker.THREAD_PREFIX)) {
			throw new IllegalStateException("Cannot call this method from non particle ticker thread");
		}
	}
}

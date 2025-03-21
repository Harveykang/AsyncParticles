package fun.qu_an.minecraft.asyncparticles.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

public class ThreadUtil {
	public static void assertNotParticleThread() {
		if (isOnParticleThread()) {
			throw new IllegalStateException("Cannot call this method from particle thread");
		}
	}

	public static void assertNotParticleRendererThread() {
		if (isOnParticleRendererThread()) {
			throw new IllegalStateException("Cannot call this method from particle renderer thread");
		}
	}

	public static void assertParticleRendererThread() {
		if (!isOnParticleRendererThread()) {
			throw new IllegalStateException("Cannot call this method from NON particle renderer thread");
		}
	}

	public static void assertNotParticleTickerThread() {
		if (isOnParticleTickerThread()) {
			throw new IllegalStateException("Cannot call this method from particle ticker thread");
		}
	}

	public static void assertParticleTickerThread() {
		if (!isOnParticleTickerThread()) {
			throw new IllegalStateException("Cannot call this method from NON particle ticker thread");
		}
	}

	public static boolean isOnParticleThread() {
		ForkJoinPool pool;
		return Thread.currentThread() instanceof ForkJoinWorkerThread t &&
			   ((pool = t.getPool()) == AsyncRenderer.EXECUTOR || pool == AsyncTicker.EXECUTOR);
	}

	public static boolean isOnParticleRendererThread() {
		return Thread.currentThread() instanceof ForkJoinWorkerThread t && t.getPool() == AsyncRenderer.EXECUTOR;
	}

	public static boolean isOnParticleTickerThread() {
		return Thread.currentThread() instanceof ForkJoinWorkerThread t && t.getPool() == AsyncTicker.EXECUTOR;
	}

	public static boolean isOnClientTickThread() {
		return RenderSystem.isOnRenderThread() || isOnParticleTickerThread();
	}
}

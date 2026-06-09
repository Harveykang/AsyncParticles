package fun.qu_an.minecraft.asyncparticles.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_extract.AsyncRendererThread;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickerThread;
import net.minecraft.client.Minecraft;

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
		Class<? extends Thread> tClass = Thread.currentThread().getClass();
		return tClass == AsyncRendererThread.class || tClass == AsyncTickerThread.class;
	}

	public static boolean isOnParticleRendererThread() {
		return Thread.currentThread().getClass() == AsyncRendererThread.class;
	}

	public static boolean isOnParticleTickerThread() {
		return Thread.currentThread().getClass() == AsyncTickerThread.class;
	}

	public static boolean isOnClientTickThread() {
		return isOnMainThread() || isOnParticleTickerThread();
	}

	public static void runOnClient(Runnable runnable) {
		Minecraft.getInstance().execute(runnable);
	}

	public static void enqueueClientTask(Runnable runnable) {
		Minecraft.getInstance().pendingRunnables.add(runnable);
	}

	public static boolean isOnMainThread() {
		return RenderSystem.isOnRenderThread();
	}

	public static void assertOnMainThread() {
		RenderSystem.assertOnRenderThread();
	}
}

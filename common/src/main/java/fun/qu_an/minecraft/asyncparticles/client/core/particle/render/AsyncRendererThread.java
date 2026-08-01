package fun.qu_an.minecraft.asyncparticles.client.core.particle.render;

import fun.qu_an.minecraft.asyncparticles.client.util.AsyncParticleWorkerThread;

import java.util.concurrent.ForkJoinPool;

public class AsyncRendererThread extends AsyncParticleWorkerThread {
	public AsyncRendererThread(ForkJoinPool forkJoinPool) {
		super(forkJoinPool);
	}

	protected void onTermination(Throwable throwable) {
		if (throwable != null) {
			AsyncRenderBehavior.LOGGER.warn("{} died", this.getName(), throwable);
		} else {
			AsyncRenderBehavior.LOGGER.debug("{} shutdown", this.getName());
		}

		super.onTermination(throwable);
	}
}

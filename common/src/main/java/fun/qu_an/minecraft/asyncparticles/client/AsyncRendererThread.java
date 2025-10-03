package fun.qu_an.minecraft.asyncparticles.client;

import com.mojang.logging.LogUtils;
import fun.qu_an.minecraft.asyncparticles.client.util.AsyncParticleWorkerThread;
import org.slf4j.Logger;

import java.util.concurrent.ForkJoinPool;

public class AsyncRendererThread extends AsyncParticleWorkerThread {
	private static final Logger LOGGER = LogUtils.getLogger();

	public AsyncRendererThread(ForkJoinPool forkJoinPool) {
		super(forkJoinPool);
	}

	protected void onTermination(Throwable throwable) {
		if (throwable != null) {
			LOGGER.warn("{} died", this.getName(), throwable);
		} else {
			LOGGER.debug("{} shutdown", this.getName());
		}

		super.onTermination(throwable);
	}
}

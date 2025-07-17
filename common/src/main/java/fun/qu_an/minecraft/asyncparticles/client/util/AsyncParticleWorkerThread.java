package fun.qu_an.minecraft.asyncparticles.client.util;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

public class AsyncParticleWorkerThread extends ForkJoinWorkerThread {
	protected AsyncParticleWorkerThread(ForkJoinPool pool) {
		super(pool);
	}
}

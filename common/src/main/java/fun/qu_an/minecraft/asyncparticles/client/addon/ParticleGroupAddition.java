package fun.qu_an.minecraft.asyncparticles.client.addon;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface ParticleGroupAddition {
	void asyncparticles$cleanUp();

	void asyncparticles$setFuture(CompletableFuture<Void> future);

	CompletableFuture<Void> asyncparticles$getFuture();

	default void waitFuture() throws ExecutionException, InterruptedException {
		try {
			asyncparticles$getFuture().get();
		}finally {
			asyncparticles$setFuture(null);
		}
	}
}

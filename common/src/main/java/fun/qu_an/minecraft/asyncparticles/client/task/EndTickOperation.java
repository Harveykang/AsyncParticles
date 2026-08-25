package fun.qu_an.minecraft.asyncparticles.client.task;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/**
 * @apiNote Execution is not guaranteed.
 */
public interface EndTickOperation extends Runnable {
	/**
	 * @apiNote Execution is not guaranteed.
	 */
	static void schedule(EndTickOperation task) {
		AsyncTickBehavior.getInstance().scheduleOperation(task);
	}

	/**
	 * @apiNote Execution is not guaranteed.
	 */
	static void schedule(ResourceLocation id, boolean parallel, Runnable task) {
		schedule(new DefaultEndTickOperation(id, parallel, task));
	}

	/**
	 * @apiNote Execution is not guaranteed.
	 */
	static void schedule(ResourceLocation id, boolean parallel, Runnable task, Consumer<Exception> exceptionHandler) {
		schedule(new DefaultEndTickOperation(id, parallel, task, exceptionHandler));
	}

	/**
	 * @implSpec Must be thread-safe.
	 */
	default boolean isParallel() {
		return true;
	}

	/**
	 * @implSpec Must be thread-safe.
	 */
	ResourceLocation getId();

	@Override
	void run();
}

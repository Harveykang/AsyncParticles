package fun.qu_an.minecraft.asyncparticles.client.api;

import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public interface EndTickOperation extends Runnable {
	static void schedule(EndTickOperation task) {
		AsyncTicker.scheduleOperation(task);
	}

	static void schedule(ResourceLocation id, boolean parallel, Runnable task) {
		schedule(new DefaultEndTickOperation(id, parallel, task));
	}

	static void schedule(ResourceLocation id, Runnable task) {
		schedule(id, false, task);
	}

	static void schedule(ResourceLocation id, boolean parallel, MinecraftConsumer task) {
		schedule(new DefaultEndTickOperation(id, parallel, () -> task.accept(Minecraft.getInstance())));
	}

	static void schedule(ResourceLocation id, boolean parallel, ClientLevelConsumer task) {
		schedule(new DefaultEndTickOperation(id, parallel, () -> task.accept(Minecraft.getInstance().level)));
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

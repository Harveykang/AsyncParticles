package fun.qu_an.minecraft.asyncparticles.client.api;

import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public interface EndTickOperation extends Runnable {
	static void schedule(EndTickOperation task) {
		AsyncTicker.scheduleOperation(task);
	}

	static void schedule(ResourceLocation id, Runnable task, boolean parallel) {
		schedule(new DefaultEndTickOperation(id, task, parallel));
	}

	static void schedule(ResourceLocation id, Runnable task) {
		schedule(id, task, true);
	}

	static void schedule(ResourceLocation id, MinecraftConsumer task, boolean parallel) {
		schedule(new DefaultEndTickOperation(id, () -> task.accept(Minecraft.getInstance()), parallel));
	}

	static void schedule(ResourceLocation id, ClientLevelConsumer task, boolean parallel) {
		schedule(new DefaultEndTickOperation(id, () -> task.accept(Minecraft.getInstance().level), parallel));
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

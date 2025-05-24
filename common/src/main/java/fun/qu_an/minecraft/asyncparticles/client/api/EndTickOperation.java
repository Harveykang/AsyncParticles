package fun.qu_an.minecraft.asyncparticles.client.api;

import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public interface EndTickOperation extends Runnable {
	static void schedule(EndTickOperation task) {
		AsyncTicker.scheduleOperation(task);
	}

	static void schedule(ResourceLocation id, Runnable task, boolean ordered) {
		schedule(new DefaultEndTickOperation(id, task, ordered));
	}

	static void schedule(ResourceLocation id, Runnable task) {
		schedule(id, task, false);
	}

	static void schedule(ResourceLocation id, MinecraftConsumer task, boolean ordered) {
		schedule(new DefaultEndTickOperation(id, () -> task.accept(Minecraft.getInstance()), ordered));
	}

	static void schedule(ResourceLocation id, ClientLevelConsumer task, boolean ordered) {
		schedule(new DefaultEndTickOperation(id, () -> task.accept(Minecraft.getInstance().level), ordered));
	}

	/**
	 * @implSpec Must be thread-safe.
	 */
	default boolean isOrdered() {
		return false;
	}

	/**
	 * @implSpec Must be thread-safe.
	 */
	ResourceLocation getId();

	@Override
	void run();
}

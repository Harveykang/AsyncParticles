package fun.qu_an.minecraft.asyncparticles.client.api;

import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import net.minecraft.client.Minecraft;

public interface EndTickEvent extends Runnable {
	/**
	 * Equivalent to {@link EndTickEvent#register(boolean true, Runnable)}
	 */
	static void register(EndTickEvent task) {
		AsyncTicker.registerEvent(task);
	}

	static void register(boolean parallel, Runnable task) {
		register(new DefaultEndTickEvent(1000, parallel, task));
	}

	static void register(boolean parallel, MinecraftConsumer task) {
		register(new DefaultEndTickEvent(1000, parallel, () -> task.accept(Minecraft.getInstance())));
	}

	static void register(boolean parallel, ClientLevelConsumer task) {
		register(new DefaultEndTickEvent(1000, parallel, () -> task.accept(Minecraft.getInstance().level)));
	}

	default int getPriority() {
		return 1000;
	}

	default boolean isParallel() {
		return true;
	}
}

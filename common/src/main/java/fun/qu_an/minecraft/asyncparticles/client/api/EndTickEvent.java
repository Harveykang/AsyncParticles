package fun.qu_an.minecraft.asyncparticles.client.api;

import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import net.minecraft.client.Minecraft;

public interface EndTickEvent extends Runnable {
	/**
	 * Equivalent to {@link EndTickEvent#register(Runnable, boolean false)}
	 */
	static void register(EndTickEvent task) {
		AsyncTicker.registerEvent(task);
	}

	static void register(Runnable task, boolean ordered) {
		register(new DefaultEndTickEvent(task, 1000, ordered));
	}

	static void register(MinecraftConsumer task, boolean ordered) {
		register(new DefaultEndTickEvent(() -> task.accept(Minecraft.getInstance()), 1000, ordered));
	}

	static void register(ClientLevelConsumer task, boolean ordered) {
		register(new DefaultEndTickEvent(() -> task.accept(Minecraft.getInstance().level), 1000, ordered));
	}

	default int getPriority() {
		return 1000;
	}

	default boolean isOrdered() {
		return false;
	}
}

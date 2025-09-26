package fun.qu_an.minecraft.asyncparticles.client.task;

import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncTickBehavior;
import net.minecraft.client.Minecraft;

public interface EndTickEvent extends Runnable {
	/**
	 * Equivalent to {@link EndTickEvent#register(boolean false, Runnable)}
	 */
	static void register(EndTickEvent task) {
		AsyncTickBehavior.registerEvent(task);
	}

	/**
	 * Equivalent to {@link EndTickEvent#register(boolean false, MinecraftConsumer)}
	 */
	static void register(MinecraftConsumer task) {
		AsyncTickBehavior.registerEvent(() -> task.accept(Minecraft.getInstance()));
	}

	/**
	 * Equivalent to {@link EndTickEvent#register(boolean false, ClientLevelConsumer)}
	 */
	static void register(ClientLevelConsumer task) {
		AsyncTickBehavior.registerEvent(() -> task.accept(Minecraft.getInstance().level));
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
		return false;
	}
}

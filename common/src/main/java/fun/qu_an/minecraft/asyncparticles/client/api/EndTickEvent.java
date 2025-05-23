package fun.qu_an.minecraft.asyncparticles.client.api;

import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import net.minecraft.resources.ResourceLocation;

public interface EndTickEvent extends Runnable {
	static void register(EndTickEvent task) {
		AsyncTicker.registerEvent(task);
	}

	default int getPriority() {
		return 1000;
	}

	default boolean isOrdered() {
		return false;
	}

	ResourceLocation getId();
}

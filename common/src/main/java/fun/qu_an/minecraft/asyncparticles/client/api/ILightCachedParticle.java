package fun.qu_an.minecraft.asyncparticles.client.api;

import org.jetbrains.annotations.ApiStatus;

public interface ILightCachedParticle {
	@ApiStatus.NonExtendable
	default void asyncparticles$setLight(int light) {
		throw new AssertionError("Missing implementation.");
	}

	@ApiStatus.NonExtendable
	default int asyncparticles$getCachedLight() {
		throw new AssertionError("Missing implementation.");
	}

	void asyncparticles$refresh();
}

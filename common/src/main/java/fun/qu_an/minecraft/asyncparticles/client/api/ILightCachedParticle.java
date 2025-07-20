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

	default void asyncparticles$refresh() {
		throw new AssertionError("Missing implementation.");
	}

	@ApiStatus.NonExtendable
	default void asyncparticles$enableLightCache() {
		throw new AssertionError("Missing implementation.");
	}

	@ApiStatus.NonExtendable
	default void asyncparticles$disableLightCache() {
		throw new AssertionError("Missing implementation.");
	}
}

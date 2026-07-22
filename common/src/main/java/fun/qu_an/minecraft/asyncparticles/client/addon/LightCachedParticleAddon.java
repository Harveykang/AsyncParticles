package fun.qu_an.minecraft.asyncparticles.client.addon;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface LightCachedParticleAddon {
	byte INITIAL_LIGHT_CACHE = 0;

	static byte compress(int light) {
		return (byte) (light >> 4 & 0xF | light >> 16 & 0xF0);
	}

	static int decompress(byte lightCache) {
		return (lightCache & 0xF) << 4 | (lightCache & 0xF0) << 16;
	}

	void asyncparticles$setLight(int light);

	default boolean asyncparticles$isStaticLight() {
		return false;
	}

	void asyncparticles$refresh();

	default void asyncparticles$tickLightCache() {
		if (asyncparticles$isEnabledLightCache()) {
			asyncparticles$refresh();
		}
	}

	int asyncparticles$getCachedLight();

	void asyncparticles$enableLightCache(boolean enable);

	boolean asyncparticles$isEnabledLightCache();

	int asyncparticles$invoke_getLightCoords(float partialTickTime);
}

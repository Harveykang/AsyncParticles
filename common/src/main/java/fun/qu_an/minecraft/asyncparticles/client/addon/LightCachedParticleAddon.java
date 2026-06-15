package fun.qu_an.minecraft.asyncparticles.client.addon;

import fun.qu_an.minecraft.asyncparticles.client.util.GameUtil;
import net.minecraft.client.particle.Particle;
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

	static void doFirstRefresh(Particle particle) {
		// Enable the light only if the particle is added to the current ParticleEngine instance.
		((LightCachedParticleAddon) particle).asyncparticles$enableLightCache();
		Integer i = GameUtil.DESTRUCTION_LIGHT_CACHE.get();
		if (i == null){
			// refresh the light cache here since this method can run in other threads.
			// so it can avoid to slower the main thread.
			((LightCachedParticleAddon) particle).asyncparticles$refresh();
		} else {
			((LightCachedParticleAddon) particle).asyncparticles$setLight(i);
		}
	}

	void asyncparticles$setLight(int light);

	byte asyncparticles$getCompressedLight();

	default int asyncparticles$getCachedLight() {
		return decompress(this.asyncparticles$getCompressedLight());
	}

	void asyncparticles$refresh();

	int asyncparticles$invoke_getLightColor(float partialTick);

	void asyncparticles$enableLightCache();

	void asyncparticles$disableLightCache();

	boolean asyncparticles$isEnabledLightCache();
}

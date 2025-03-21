package fun.qu_an.minecraft.asyncparticles.client.addon;

public interface LightCachedParticleAddon {
	byte INITIAL_LIGHT_CACHE = 0;

	static byte compress(int light) {
		return (byte) (light >> 4 & 0xF | light >> 16 & 0xF0);
	}

	static int decompress(byte lightCache) {
		return (lightCache & 0xF) << 4 | (lightCache & 0xF0) << 16;
	}

	void asyncParticles$setLight(int light);

	byte asyncParticles$getCompressedLight();

	void asyncParticles$refresh();

	int asyncParticles$invoke_getLightColor(float partialTick);
}

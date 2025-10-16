package fun.qu_an.minecraft.asyncparticles.client.addon;

import fun.qu_an.minecraft.asyncparticles.client.api.ILightCachedParticle;
import fun.qu_an.minecraft.asyncparticles.client.util.ParticleThreadLocal;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface LightCachedParticleAddon extends ILightCachedParticle {
	byte INITIAL_LIGHT_CACHE = 0;
	ParticleThreadLocal<BlockPos.MutableBlockPos> SHARED_POS = ParticleThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

	static byte compress(int light) {
		return (byte) (light >> 4 & 0xF | light >> 16 & 0xF0);
	}

	static int decompress(byte lightCache) {
		return (lightCache & 0xF) << 4 | (lightCache & 0xF0) << 16;
	}

	@Override
	void asyncparticles$setLight(int light);

	byte asyncparticles$getCompressedLight();

	@Override
	default int asyncparticles$getCachedLight() {
		return decompress(this.asyncparticles$getCompressedLight());
	}

	@Override
	void asyncparticles$refresh();

	int asyncparticles$invoke_getLightColor(float partialTick);

	void asyncparticles$enableLightCache();

	void asyncparticles$disableLightCache();

	boolean asyncparticles$isEnabledLightCache();
}

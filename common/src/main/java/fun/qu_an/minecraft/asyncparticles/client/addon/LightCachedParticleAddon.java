package fun.qu_an.minecraft.asyncparticles.client.addon;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.util.ParticleThreadLocal;
import net.minecraft.core.BlockPos;

public interface LightCachedParticleAddon {
	byte INITIAL_LIGHT_CACHE = 0;
	ParticleThreadLocal<BlockPos.MutableBlockPos> SHARED_POS = ParticleThreadLocal.withInitial(RenderSystem::isOnRenderThread, BlockPos.MutableBlockPos::new);

	static byte compress(int light) {
		return (byte) (light >> 4 & 0xF | light >> 16 & 0xF0);
	}

	static int decompress(byte lightCache) {
		return (lightCache & 0xF) << 4 | (lightCache & 0xF0) << 16;
	}

	void asyncparticles$setLight(int light);

	byte asyncparticles$getCompressedLight();

	default int asyncparticles$getCachedLight() {
		return decompress(this.asyncparticles$getCompressedLight());
	}

	void asyncparticles$refresh();

	void asyncparticles$enableLightCache();

	void asyncparticles$disableLightCache();

	boolean asyncparticles$isEnabledLightCache();
}

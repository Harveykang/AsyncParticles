package fun.qu_an.minecraft.asyncparticles.client.addon;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.mixin.MixinParticle_LightCache;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.particle.Particle;

public interface LightCachedParticleAddon {
	short INITIAL_LIGHT_CACHE = -1;

	static byte compress(int light) {
		return (byte) (light >> 4 & 0xF | light >> 16 & 0xF0);
	}

	static int decompress(byte lightCache) {
		return (lightCache & 0xF) << 4 | (lightCache & 0xF0) << 16;
	}

	/**
	 * With some complex logic, we can avoid unnecessary calls to {@link Particle#getLightColor}<p>
	 * See {@link MixinParticle_LightCache#wrapGetLightColor},<p>
	 *     {@link MixinParticle_LightCache#asyncParticles$refresh}<p>
	 *     for more details.
	 */
	static boolean isLightCacheValid(short lightCache) {
		return lightCache >= 0 || (
			lightCache != INITIAL_LIGHT_CACHE
			&& (ThreadUtil.isOnParticleRendererThread()
				|| (RenderSystem.isOnRenderThread() && !AsyncTicker.isTickingSync()))
		);
	}

	void asyncParticles$setLight(int light);

	byte asyncParticles$getCompressedLight();

	void asyncParticles$refresh();

	int asyncParticles$invoke_getLightColor(float partialTick);
}

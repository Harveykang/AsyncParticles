package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.light_cache;

import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.opallight.OpalLightParticleAddon;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Particle.class)
public abstract class MixinParticle_LightCache_Int implements LightCachedParticleAddon, OpalLightParticleAddon {
	/**
	 * Low byte: vanilla block/sky light
	 * High 24 bits: For custom use.
 	 */
	@Unique
	private int asyncparticles$lightCache = INITIAL_LIGHT_CACHE;

	@Override
	public void asyncparticles$setLight(int light) {
		asyncparticles$lightCache = (asyncparticles$lightCache & 0xFFFFFF00)
			| (LightCachedParticleAddon.compress(light) & 0xFF);
	}

	@Override
	public int asyncparticles$getCachedLight() {
		return LightCachedParticleAddon.decompress((byte) (asyncparticles$lightCache & 0xFF));
	}

	@Override
	public int asyncparticles$getCachedColor() {
		return (asyncparticles$lightCache >>> 8) & 0xFFFFFF;
	}

	@Override
	public void asyncparticles$setCachedColor(int rgb) {
		asyncparticles$lightCache = (asyncparticles$lightCache & 0xFF) | ((rgb & 0xFFFFFF) << 8);
	}
}

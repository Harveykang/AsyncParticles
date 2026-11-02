package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.light_cache;

import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Particle.class)
public abstract class MixinParticle_LightCache_Byte implements LightCachedParticleAddon {
	@Unique
	private byte asyncparticles$lightCache = INITIAL_LIGHT_CACHE;

	@Override
	public void asyncparticles$setLight(int light) {
		asyncparticles$lightCache = LightCachedParticleAddon.compress(light);
	}

	@Override
	public int asyncparticles$getCachedLight() {
		return LightCachedParticleAddon.decompress(asyncparticles$lightCache);
	}
}

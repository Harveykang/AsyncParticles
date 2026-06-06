package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.fix_black_destruction_particle;

import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.fix_black_destruction_particle.TerrainParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.tick.MixinParticle_LightCache;
import net.minecraft.client.particle.TerrainParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TerrainParticle.class)
public abstract class MixinTerrainParticle_FixBlackDestroyParticle extends MixinParticle_LightCache implements LightCachedParticleAddon {
	@Unique
	private boolean asyncparticles$isFirstRefresh = true;

	@Override
	public void asyncparticles$refresh() {
		if (asyncparticles$isFirstRefresh) {
			asyncparticles$isFirstRefresh = false;
			Integer i = TerrainParticleBehavior.DESTRUCTION_LIGHT_CACHE.get();
			if (i != null) {
				asyncparticles$setLight(i);
			} else {
				super.asyncparticles$refresh();
			}
		} else {
			super.asyncparticles$refresh();
		}
	}
}

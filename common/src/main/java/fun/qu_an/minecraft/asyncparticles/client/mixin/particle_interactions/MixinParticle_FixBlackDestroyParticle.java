package fun.qu_an.minecraft.asyncparticles.client.mixin.particle_interactions;

import fun.qu_an.minecraft.asyncparticles.client.particle.GpuParticleBehavior;
import games.enchanted.blockplaceparticles.particle.dust.FloatingDust;
import games.enchanted.blockplaceparticles.particle.petal.FallingPetal;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.MixinParticle_LightCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = { FloatingDust.class, FallingPetal.class })
public abstract class MixinParticle_FixBlackDestroyParticle extends MixinParticle_LightCache implements LightCachedParticleAddon {

    @Unique
    private boolean asyncparticles$isFirstRefresh = true;

    @Override
    public void asyncparticles$refresh() {
        if (asyncparticles$isFirstRefresh) {
            asyncparticles$isFirstRefresh = false;
            Integer i = GpuParticleBehavior.INSTANCE.DESTROY_LIGHT_CACHE.get();
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

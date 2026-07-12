package fun.qu_an.minecraft.asyncparticles.client.mixin.conditional;

import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.MixinParticle_LightCache;
import net.minecraft.client.particle.*;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(value = {
	HugeExplosionParticle.class,
	AttackSweepParticle.class,
	SculkChargeParticle.class,
	SculkChargePopParticle.class,
	SimpleAnimatedParticle.class,
	FlyStraightTowardsParticle.class,
	GustParticle.class,
	ShriekParticle.class,
	TrialSpawnerDetectionParticle.class,
	VibrationSignalParticle.class,
}) // Will be replaced with the actual targets
public abstract class MixinParticles_LightCacheNoRefresh extends MixinParticle_LightCache {
	@Shadow
	public abstract int getLightColor(float f);

	@Override
	public boolean asyncparticles$isStaticLight() {
		return true;
	}

	@Override
	public void asyncparticles$refresh() {
		try {
			asyncparticles$setLight(getLightColor(0f));
		} catch (MissingPaletteEntryException ignore) {
			// chunk not loaded yet maybe, ignore
			asyncparticles$setLight(0);
		}
	}
}

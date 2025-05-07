package fun.qu_an.minecraft.asyncparticles.client.mixin;

import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import net.minecraft.client.particle.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(value = {
	HugeExplosionParticle.class,
	AttackSweepParticle.class,
	SculkChargeParticle.class,
	SculkChargePopParticle.class,
	SimpleAnimatedParticle.class,
	ShriekParticle.class,
	VibrationSignalParticle.class,
}) // Will be replaced by the actual targets
public abstract class MixinParticles_LightCacheNoRefresh implements LightCachedParticleAddon {
	@Override
	public void asyncparticles$refresh() {
	}
}

package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.chailotl.particular.particles.FireflyParticle;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import net.minecraft.client.particle.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin({
	HugeExplosionParticle.class,
	AttackSweepParticle.class,
	SculkChargeParticle.class,
	SculkChargePopParticle.class,
	SimpleAnimatedParticle.class,
	ShriekParticle.class,
	FireflyParticle.class,
	VibrationSignalParticle.class,
	// Add more particle classes here if needed
})
public abstract class MixinMixinParticle_NoRefreshLightCache implements LightCachedParticleAddon {
	@Override
	public void asyncParticles$refresh() {
	}
}

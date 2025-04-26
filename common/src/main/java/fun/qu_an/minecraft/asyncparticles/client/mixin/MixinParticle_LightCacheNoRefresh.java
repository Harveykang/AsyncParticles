package fun.qu_an.minecraft.asyncparticles.client.mixin;

import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import net.diebuddies.minecraft.weather.WeatherParticle;
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
	WeatherParticle.class,
	// Add more particle classes here if needed
}, targets = {
	"dev.shadowsoffire.gateways.client.GatewayParticle",
	"com.chailotl.particular.particles.FireflyParticle",
})
public abstract class MixinParticle_LightCacheNoRefresh implements LightCachedParticleAddon {
	@Override
	public void asyncparticles$refresh() {
	}
}

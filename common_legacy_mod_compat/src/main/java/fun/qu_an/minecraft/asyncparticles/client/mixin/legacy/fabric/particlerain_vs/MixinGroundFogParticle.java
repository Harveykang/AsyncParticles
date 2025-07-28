package fun.qu_an.minecraft.asyncparticles.client.mixin.legacy.fabric.particlerain_vs;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
import fun.qu_an.minecraft.asyncparticles.client.mixin.legacy.fabric.particlerain.MixinWeatherParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.particle.GroundFogParticle;

@Mixin(GroundFogParticle.class)
// extends MixinWeatherParticle instead of WeatherParticle to compile properly
// (to be compatible with Connector + Particle Rain)
public abstract class MixinGroundFogParticle extends MixinWeatherParticle implements ParticleAddon {
	@Shadow
	public abstract void remove();

	protected MixinGroundFogParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void onTick(CallbackInfo ci) {
		if (VSClientUtils.isEntityMovColShipOnly(
			Vec3.ZERO,
			getBoundingBox().inflate(6, 0, 6),
			level)) {
			remove();
		}
	}
}

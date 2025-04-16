package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.particlerain_vs;

import com.leclowndu93150.particlerain.particle.GroundFogParticle;
import com.leclowndu93150.particlerain.particle.WeatherParticle;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GroundFogParticle.class)
public abstract class MixinGroundFogParticle extends WeatherParticle implements ParticleAddon {
	@Shadow public abstract void remove();

	protected MixinGroundFogParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void onTick(CallbackInfo ci) {
		if (VSClientUtils.isEntityMovColShipOnly(null,
			Vec3.ZERO,
			getBoundingBox().inflate(6, 0, 6),
			level)) {
			remove();
		}
	}
}

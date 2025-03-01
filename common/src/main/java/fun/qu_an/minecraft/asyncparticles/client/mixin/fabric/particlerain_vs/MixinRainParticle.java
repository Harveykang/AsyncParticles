package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.particlerain_vs;

import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.fabric.ParticleRainUtils;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import pigcart.particlerain.particle.RainParticle;

@Mixin(value = RainParticle.class)
public abstract class MixinRainParticle extends MixinWeatherParticle {
	protected MixinRainParticle(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
	}

	static {
		Type.RAIN.register((level, location, v, aabb) -> {
			Vec3 shipMovement = VSClientUtils.entityMovColShipOnly(null, v, aabb, level);
			if (shipMovement == null) {
				return v;
			}
			ParticleRainUtils.onShipCollision(level, location, shipMovement, aabb);
			return shipMovement;
		});
	}
}

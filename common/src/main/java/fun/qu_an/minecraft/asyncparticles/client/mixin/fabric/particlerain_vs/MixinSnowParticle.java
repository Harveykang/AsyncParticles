package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.particlerain_vs;

import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import pigcart.particlerain.particle.SnowParticle;

@Mixin(value = SnowParticle.class)
public abstract class MixinSnowParticle extends MixinWeatherParticle {
	protected MixinSnowParticle(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
	}

	static {
		Type.SNOW.register((level, location, v, aabb) -> {
			Vec3 shipMovement = VSClientUtils.entityMovColShipOnly(null, v, aabb, level);
			return shipMovement == null ? v : shipMovement;
		});
	}
}

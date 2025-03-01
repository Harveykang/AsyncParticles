package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.particlerain_vs;

import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainUtils;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.WeatherParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
import fun.qu_an.minecraft.asyncparticles.client.mixin.vs2.InvokerEntityShipCollisionUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.valkyrienskies.mod.common.util.EntityShipCollisionUtils;
import pigcart.particlerain.particle.WeatherParticle;

@Mixin(value = WeatherParticle.class)
public abstract class MixinWeatherParticle extends TextureSheetParticle implements WeatherParticleAddon {
	protected MixinWeatherParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
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
		Type.SNOW.register((level, location, v, aabb) -> {
			Vec3 shipMovement = VSClientUtils.entityMovColShipOnly(null, v, aabb, level);
			return shipMovement == null ? v : shipMovement;
		});
		Type.OTHER.register((level, position, motion, aabb) -> {
			if (!((InvokerEntityShipCollisionUtils) (Object) EntityShipCollisionUtils.INSTANCE)
				.invoker_getShipPolygonsCollidingWithEntity(null, motion, aabb, level)
				.isEmpty()) {
				return null;
			}
			return motion;
		});
	}
}

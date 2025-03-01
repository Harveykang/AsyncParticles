package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.particlerain_create;

import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateUtils;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.WeatherParticleAddon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import pigcart.particlerain.particle.WeatherParticle;

@Mixin(value = WeatherParticle.class)
public abstract class MixinWeatherParticle extends TextureSheetParticle implements WeatherParticleAddon {
	protected MixinWeatherParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	static {
		CollisionFunction function = (level, position, motion, aabb) -> {
			Vec3 collide = CreateUtils.collideWithContraptions(level, position, motion, aabb);
			return collide == null || collide.equals(motion) ? null : motion;
		};
		Type.OTHER.register(function);
		Type.RAIN.register(function);
		Type.SNOW.register(function);
	}
}

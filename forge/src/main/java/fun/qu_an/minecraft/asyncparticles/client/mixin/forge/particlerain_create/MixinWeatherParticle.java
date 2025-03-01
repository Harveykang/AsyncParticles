package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.particlerain_create;

import com.leclowndu93150.particlerain.particle.WeatherParticle;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateUtils;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainUtils;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.WeatherParticleAddon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = WeatherParticle.class)
public abstract class MixinWeatherParticle extends TextureSheetParticle implements WeatherParticleAddon {
	protected MixinWeatherParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	static {
		Type.RAIN.register((level, position, motion, aabb) -> {
			Vec3 collide = CreateUtils.collideMotionWithContraptions(level, position, motion, aabb);
			if (collide == null) {
				return motion;
			}
			ParticleRainUtils.onCreateCollision(level, motion, collide, aabb);
			return collide;
		});
		CollisionFunction function = (level, position, motion, aabb) -> {
			Vec3 collide = CreateUtils.collideMotionWithContraptions(level, position, motion, aabb);
			return collide == null ? motion : collide;
		};
		Type.OTHER.register(function);
		Type.SNOW.register(function);
	}
}

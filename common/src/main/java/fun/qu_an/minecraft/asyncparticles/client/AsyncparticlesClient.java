package fun.qu_an.minecraft.asyncparticles.client;

import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.WeatherParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;

public class AsyncparticlesClient {
	public static final String MOD_ID = "asyncparticles";

	public static void init() {
		try {
			SimplePropertiesConfig.load();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (ModListHelper.PARTICLERAIN_LOADED) {
			if (ModListHelper.VS_LOADED) {
				WeatherParticleAddon.Type.RAIN.register((level, location, v, aabb) -> {
					Vec3 shipMovement = VSClientUtils.entityMovColShipOnly(null, v, aabb, level);
					if (shipMovement == null) {
						return v;
					}
					ParticleRainCompat.onShipCollision(level, location, shipMovement, aabb);
					return shipMovement;
				});
				WeatherParticleAddon.CollisionFunction function = (level, location, v, aabb) -> {
					Vec3 shipMovement = VSClientUtils.entityMovColShipOnly(null, v, aabb, level);
					return shipMovement == null ? v : shipMovement;
				};
				WeatherParticleAddon.Type.SNOW.register(function);
				WeatherParticleAddon.Type.OTHER.register(function);
			}
			if (ModListHelper.CREATE_LOADED) {
				WeatherParticleAddon.Type.RAIN.register((level, position, motion, aabb) -> {
					Vec3 collide = CreateCompat.collideMotionWithContraptions(level, position, motion, aabb);
					if (collide == null) {
						return motion;
					}
					ParticleRainCompat.onCreateCollision(level, motion, collide, aabb);
					return collide;
				});
				WeatherParticleAddon.CollisionFunction function = (level, position, motion, aabb) -> {
					Vec3 collide = CreateCompat.collideMotionWithContraptions(level, position, motion, aabb);
					return collide == null ? motion : collide;
				};
				WeatherParticleAddon.Type.SNOW.register(function);
				WeatherParticleAddon.Type.OTHER.register(function);
			}
		}
	}
}

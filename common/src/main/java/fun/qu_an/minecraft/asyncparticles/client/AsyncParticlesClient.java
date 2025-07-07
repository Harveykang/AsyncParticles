package fun.qu_an.minecraft.asyncparticles.client;

import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateUtil;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.WeatherParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;

public class AsyncParticlesClient {
	public static final String MOD_ID = "asyncparticles";
	public static final String ISSUE_URL = "https://github.com/Harveykang/AsyncParticles/issues";

	public static void init() {
		if (!ModListHelper.IS_CLIENT) {
			return;
		}
		if (ModListHelper.PARTICLERAIN_LOADED) {
			if (ModListHelper.VS_LOADED) {
				WeatherParticleAddon.Type.RAIN.register((level, location, v, aabb) -> {
					Vec3 shipMovement = VSClientUtils.entityMovColShipOnly(v, aabb, level);
					if (shipMovement == null) {
						return v;
					}
					ParticleRainCompat.onShipCollision(level, location, shipMovement, aabb);
					return shipMovement;
				});
				WeatherParticleAddon.CollisionFunction function = (level, location, v, aabb) -> {
					Vec3 shipMovement = VSClientUtils.entityMovColShipOnly(v, aabb, level);
					return shipMovement == null ? v : shipMovement;
				};
				WeatherParticleAddon.Type.SNOW.register(function);
				WeatherParticleAddon.Type.OTHER.register(function);
			}
			if (ModListHelper.CREATE_LOADED) {
				WeatherParticleAddon.Type.RAIN.register((level, position, motion, aabb) -> {
					Vec3 collide = CreateUtil.collideMotionWithContraptions(level, motion, aabb);
					if (collide == null) {
						return motion;
					}
					ParticleRainCompat.onCreateCollision(level, motion, collide, aabb);
					return collide;
				});
				WeatherParticleAddon.CollisionFunction function = (level, position, motion, aabb) -> {
					Vec3 collide = CreateUtil.collideMotionWithContraptions(level, motion, aabb);
					return collide == null ? motion : collide;
				};
				WeatherParticleAddon.Type.SNOW.register(function);
				WeatherParticleAddon.Type.OTHER.register(function);
			}
		}
	}
}

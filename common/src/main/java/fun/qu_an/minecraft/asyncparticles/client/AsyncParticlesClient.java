package fun.qu_an.minecraft.asyncparticles.client;

import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateUtil;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class AsyncParticlesClient {
	public static final String MOD_ID = "asyncparticles";
	public static final String ISSUE_URL = "https://github.com/Harveykang/AsyncParticles/issues";

	public static void init() {
		if (!ModListHelper.IS_CLIENT) {
			return;
		}
		try {
			ConfigHelper.load();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		if (ModListHelper.PARTICLERAIN_LOADED) {
			if (ModListHelper.VS_LOADED) {
				ParticleRainAddon.Type.RAIN.register((level, location, originalMovement, aabb) -> {
					Vec3 shipMovement = VSClientUtils.entityMovColShipOnly(null, originalMovement, aabb, level);
					if (shipMovement == null) {
						return originalMovement;
					}
					ParticleRainCompat.onShipCollision(level, location, shipMovement, aabb);
					return shipMovement;
//					boolean b = VSClientUtils.isEntityMovColShipOnly(null, originalMovement, aabb, level);
//					if (!b) {
//						return originalMovement;
//					}
//					ParticleRainCompat.onShipCollision(level, location, originalMovement, aabb);
//					return null;
				});
				ParticleRainAddon.CollisionFunction function = (level, location, v, aabb) -> {
					Vec3 shipMovement = VSClientUtils.entityMovColShipOnly(null, v, aabb, level);
					return shipMovement == null ? v : shipMovement;
//					boolean b = VSClientUtils.isEntityMovColShipOnly(null, v, aabb, level);
//					return b ? null : v;
				};
				ParticleRainAddon.Type.SNOW.register(function);
				ParticleRainAddon.Type.OTHER.register(function);
			}
			if (ModListHelper.CREATE_LOADED) {
				ParticleRainAddon.Type.RAIN.register((level, position, motion, aabb) -> {
					Vec3 collide = CreateUtil.collideMotionWithContraptions(level, motion, aabb);
					if (collide == null) {
						return motion;
					}
					ParticleRainCompat.onCreateCollision(level, motion, collide, aabb);
					return collide;
				});
				ParticleRainAddon.CollisionFunction function = (level, position, motion, aabb) -> {
					Vec3 collide = CreateUtil.collideMotionWithContraptions(level, motion, aabb);
					return collide == null ? motion : collide;
				};
				ParticleRainAddon.Type.SNOW.register(function);
				ParticleRainAddon.Type.OTHER.register(function);
			}
		}
	}
}

package fun.qu_an.minecraft.asyncparticles.client;

import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateUtil;
import fun.qu_an.minecraft.asyncparticles.client.compat.moreculling.MoreCullingCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.phys.Vec3;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.*;

@Environment(EnvType.CLIENT)
public class AsyncParticlesClient {
	public static final String MOD_ID = "asyncparticles";
	public static final String ISSUE_URL = "https://github.com/Harveykang/AsyncParticles/issues";

	public static void init() {
		if (!IS_CLIENT) {
			return;
		}
		if (MORE_CULLING_LOADED) {
			MoreCullingCompat.init();
		}
		if (ModListHelper.PARTICLERAIN_LOADED) {
			if (ModListHelper.VS_LOADED) {
				ParticleRainAddon.Type.RAIN.register((level, location, originalMovement, aabb) -> {
					Vec3 shipMovement = VSClientUtils.entityMovColShipOnly(originalMovement, aabb, level);
					if (shipMovement == null) {
						return originalMovement;
					}
					ParticleRainCompat.onShipCollision(level, location, shipMovement, aabb);
					return shipMovement;
				});
				ParticleRainAddon.CollisionFunction function = (level, location, v, aabb) -> {
					Vec3 shipMovement = VSClientUtils.entityMovColShipOnly(v, aabb, level);
					return shipMovement == null ? v : shipMovement;
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

package fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.v3.forge;

import com.leclowndu93150.particlerain.ParticleRegistry;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateUtil;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.v3.ParticleRainAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.v3.ParticleRainCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.v3.RippleParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.ShipHitResult;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import static com.leclowndu93150.particlerain.ParticleRainClient.config;
import static java.lang.Math.abs;

public class ParticleRainCompatImpl extends ParticleRainCompat {
	public static ParticleRainCompat init0() {
		if (ModListHelper.FABRIC_PARTICLERAIN_LOADED) {
			return fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.v3.fabric.ParticleRainCompatImpl.init0();
		}
		if (ModListHelper.VS_LOADED) {
			ParticleRainAddon.Type.RAIN.register((level, location, originalMovement, aabb) -> {
				Vec3 shipMovement = VSClientUtils.entityMovColShipOnly(originalMovement, aabb, level);
				if (shipMovement == null) {
					return originalMovement;
				}
				ParticleRainCompat.INSTANCE.onShipCollision(level, location, shipMovement, aabb);
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
				ParticleRainCompat.INSTANCE.onCreateCollision(level, motion, collide, aabb);
				return collide;
			});
			ParticleRainAddon.CollisionFunction function = (level, position, motion, aabb) -> {
				Vec3 collide = CreateUtil.collideMotionWithContraptions(level, motion, aabb);
				return collide == null ? motion : collide;
			};
			ParticleRainAddon.Type.SNOW.register(function);
			ParticleRainAddon.Type.OTHER.register(function);
		}
		return new ParticleRainCompatImpl();
	}

	public void onShipCollision(ClientLevel level, Vec3 location, Vec3 movement, AABB aabb) {
		if (!config.doRippleParticles && !config.doSplashParticles) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		ShipHitResult hit = VSClientUtils.clipShip(level, new ClipContext(location,
				location.add(movement.normalize().scale(aabb.getSize())),
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.ANY,
				mc.player),
			true);
		if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
			return;
		}
		Vec3 shipMotion = hit.shipMotion;
		if (!ConfigHelper.alwaysSpawnRainParticlesOnVsShips() && abs(shipMotion.lengthSqr()) > 0.01) {
			return;
		}
		Vec3 spawnPos = hit.getLocation().add(shipMotion);
		FluidState fluidState = level.getFluidState(hit.getBlockPos());
		if (config.doRippleParticles && fluidState.isSourceOfType(Fluids.WATER)) {
			Particle particle = mc.particleEngine.createParticle(ParticleRegistry.RIPPLE.get(),
				spawnPos.x,
				spawnPos.y,
				spawnPos.z,
				0,
				0,
				0);
			if (particle != null) {
				Vec3i normal = hit.getDirection().getNormal();
				Vector3f normal1 = hit.shipToWorld.transformDirection(new Vector3f(normal.getX(), normal.getY(), normal.getZ())).normalize();
				((RippleParticleAddon) particle).asyncedParticles$setNormal(normal1);
			}
			if (level.isThundering() && config.doSplashParticles) {
				mc.particleEngine.createParticle(ParticleTypes.RAIN, spawnPos.x, spawnPos.y, spawnPos.z, 0, 0, 0);
			}
		} else if (config.doSplashParticles && fluidState.isEmpty()) {
			mc.particleEngine.createParticle(ParticleTypes.RAIN, spawnPos.x, spawnPos.y, spawnPos.z, 0, 0, 0);
		}
	}

	@Override
	public void onCreateCollision(@NotNull ClientLevel level, Vec3 originalMotion, @NotNull Vec3 clipMotion, @NotNull AABB aabb) {
		if (config.doSplashParticles) {
			Vec3 center = aabb.getCenter();
			AABB aabb1 = new AABB(center.x, aabb.minY - 1, center.z, center.x, aabb.minY, center.z);
			Vec3 motion1 = originalMotion.scale(2);
			if (CreateUtil.isCollideWithContraption(level, motion1, aabb1, false)) {
				Vec3 startPos = new Vec3(center.x, aabb.minY, center.z);
				Vec3 spawnPos = startPos.add(clipMotion);
				Minecraft.getInstance().particleEngine
					.createParticle(ParticleTypes.RAIN, spawnPos.x, spawnPos.y, spawnPos.z, 0, 0, 0);
			}
		}
	}

}

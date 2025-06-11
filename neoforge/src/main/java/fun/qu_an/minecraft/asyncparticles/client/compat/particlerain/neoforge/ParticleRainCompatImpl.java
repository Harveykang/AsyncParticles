package fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.neoforge;

import com.leclowndu93150.particlerain.ParticleRainConfig;
import com.leclowndu93150.particlerain.ParticleRegistry;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateUtil;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.RippleParticleAddon;
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

import static java.lang.Math.abs;

@SuppressWarnings("unused")
public class ParticleRainCompatImpl {
	public static void onShipCollision(ClientLevel level, Vec3 location, Vec3 movement, AABB aabb) {
		if (ModListHelper.FABRIC_PARTICLERAIN_LOADED) {
			fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.fabric.ParticleRainCompatImpl
				.onShipCollision(level, location, movement, aabb);
			return;
		}
		if (!ParticleRainConfig.doRippleParticles && !ParticleRainConfig.doSplashParticles) {
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
		if (ParticleRainConfig.doRippleParticles && fluidState.isSourceOfType(Fluids.WATER)) {
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
			if (level.isThundering() && ParticleRainConfig.doSplashParticles) {
				Particle particle1 = mc.particleEngine.createParticle(ParticleTypes.RAIN, spawnPos.x, spawnPos.y, spawnPos.z, 0, 0, 0);
			}
		} else if (ParticleRainConfig.doSplashParticles && fluidState.isEmpty()) {
			Particle particle1 = mc.particleEngine.createParticle(ParticleTypes.RAIN, spawnPos.x, spawnPos.y, spawnPos.z, 0, 0, 0);
		}
	}

	public static boolean onCreateCollision0() {
		if (ModListHelper.FABRIC_PARTICLERAIN_LOADED) {
			return fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.fabric.ParticleRainCompatImpl
				.onCreateCollision0();
		}
		return ParticleRainConfig.doSplashParticles;
	}

	public static void onCreateCollision1(@NotNull ClientLevel level, Vec3 originalMotion, @NotNull Vec3 clipMotion, @NotNull AABB aabb) {
		Vec3 center = aabb.getCenter();
		AABB aabb1 = new AABB(center.x, aabb.minY - 1, center.z, center.x, aabb.minY, center.z);
		Vec3 startPos = new Vec3(center.x, aabb.minY, center.z);
		Vec3 motion1 = originalMotion.scale(2);
		if (CreateUtil.isCollideWithContraption(level, motion1, aabb1, false)) {
			Vec3 spawnPos = startPos.add(clipMotion);
			Minecraft.getInstance().particleEngine
				.createParticle(ParticleTypes.RAIN, spawnPos.x, spawnPos.y, spawnPos.z, 0, 0, 0);
		}
	}
}

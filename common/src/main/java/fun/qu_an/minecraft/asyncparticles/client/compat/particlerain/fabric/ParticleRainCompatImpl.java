package fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.fabric;

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
import org.spongepowered.asm.mixin.Unique;
import pigcart.particlerain.ParticleRainClient;

import static java.lang.Math.abs;
import static pigcart.particlerain.ParticleRainClient.config;

@SuppressWarnings("unused")
public class ParticleRainCompatImpl {
	public static void onShipCollision(ClientLevel level, Vec3 location, Vec3 movement, AABB aabb) {
		if (!config.doRippleParticles && !config.doSplashParticles) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
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
			Particle particle = mc.particleEngine.createParticle(ParticleRainClient.RIPPLE,
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
				Particle particle1 = mc.particleEngine.createParticle(ParticleTypes.RAIN, spawnPos.x, spawnPos.y, spawnPos.z, 0, 0, 0);
			}
		} else if (config.doSplashParticles && fluidState.isEmpty()) {
			Particle particle1 = mc.particleEngine.createParticle(ParticleTypes.RAIN, spawnPos.x, spawnPos.y, spawnPos.z, 0, 0, 0);
		}
	}

	public static boolean onCreateCollision0() {
		return config.doSplashParticles;
	}
}

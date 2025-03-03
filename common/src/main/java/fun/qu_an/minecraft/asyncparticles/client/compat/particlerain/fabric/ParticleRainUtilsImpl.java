package fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.fabric;

import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateUtils;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.RippleParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.ShipHitResult;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
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

import static pigcart.particlerain.ParticleRainClient.config;

@SuppressWarnings("unused")
public class ParticleRainUtilsImpl {
	@Unique
	public static void onShipCollision(ClientLevel level, Vec3 location, Vec3 movement, AABB aabb) {
		if (!config.doRippleParticles && !config.doSplashParticles) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		ShipHitResult hit = VSClientUtils.clipShip(level, new ClipContext(location,
				location.add(movement).add(movement.normalize().scale(aabb.getSize())),
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.ANY,
				mc.player),
			true);
		if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
			Vec3 spawnPos = hit.getLocation();
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
				if (level.isThundering() && config.doSplashParticles)
					mc.particleEngine.createParticle(ParticleTypes.RAIN, spawnPos.x, spawnPos.y, spawnPos.z, 0, 0, 0);
			} else if (config.doSplashParticles && fluidState.isEmpty()) {
				mc.particleEngine.createParticle(ParticleTypes.RAIN, spawnPos.x, spawnPos.y, spawnPos.z, 0, 0, 0);
			}
		}
	}

	public static void onCreateCollision(@NotNull ClientLevel level, Vec3 originalMotion, @NotNull Vec3 clipMotion, @NotNull AABB aabb) {
		if (!config.doSplashParticles || Math.abs(clipMotion.y) > 0.001) {
			return;
		}
		Vec3 center = aabb.getCenter();
		AABB aabb1 = new AABB(center.x, aabb.minY - 1, center.z, center.x, aabb.minY, center.z);
		Vec3 spawnPos = new Vec3(center.x, aabb.minY, center.z);
		Vec3 motion1 = originalMotion.scale(2);
		CreateUtils.forEachCollidingContraption(level, aabb1, contraptionEntity -> {
			if (CreateUtils.collideWithContraption(level, spawnPos, motion1, aabb1, contraptionEntity)) {
				Minecraft.getInstance().particleEngine
					.createParticle(ParticleTypes.RAIN, spawnPos.x, spawnPos.y, spawnPos.z, 0, 0, 0);
				return false;
			}
			return true;
		});
	}
}

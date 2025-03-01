package fun.qu_an.minecraft.asyncparticles.client.compat.particlerain;

import dev.architectury.injectables.annotations.ExpectPlatform;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateUtils;
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

public class ParticleRainUtils {
	@ExpectPlatform
	public static void onShipCollision(ClientLevel level, Vec3 location, Vec3 movement, AABB aabb) {
		throw new AssertionError();
	}

	public static void onCreateCollision(@NotNull ClientLevel level, Vec3 originalMotion, @NotNull Vec3 clipMotion, @NotNull AABB aabb) {
		if (Math.abs(clipMotion.y) > 0.001) {
			return;
		}
		Vec3 center = aabb.getCenter();
		AABB aabb1 = new AABB(center.x, aabb.minY - 1, center.z, center.x, aabb.minY, center.z);
		Vec3 spawnPos = new Vec3(center.x, aabb.minY, center.z);
		Vec3 motion1 = originalMotion.scale(2);
		boolean b = CreateUtils.contraptions(level).filter(contraption -> contraption.getBoundingBox().intersects(aabb1))
			.anyMatch(contraptionEntity ->
				CreateUtils.collideWithContraption(level, spawnPos, motion1, aabb1, contraptionEntity));
		if (b) {
			Minecraft.getInstance().particleEngine
				.createParticle(ParticleTypes.RAIN, spawnPos.x, spawnPos.y, spawnPos.z, 0, 0, 0);
		}
	}
}

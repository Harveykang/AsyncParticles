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

public class ParticleRainUtilsImpl {
	@Unique
	public static void onShipCollision(ClientLevel level, Vec3 location, Vec3 movement, AABB aabb) {
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
}

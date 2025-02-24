package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.particlerain_vs;

import fun.qu_an.minecraft.asyncparticles.client.RippleParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.ShipHitResult;
import fun.qu_an.minecraft.asyncparticles.client.VSClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pigcart.particlerain.ParticleRainClient;
import pigcart.particlerain.particle.RainParticle;

import java.util.List;

import static pigcart.particlerain.ParticleRainClient.config;

@Mixin(value = RainParticle.class)
public abstract class MixinRainParticle extends MixinWeatherPatricle {
	protected MixinRainParticle(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
	}

	@Override
	public void move(double d, double e, double f) {
		if (this.stoppedByCollision) {
			return;
		}
		double g = d;
		double h = e;
		double i = f;
		if (this.hasPhysics && (d != (double) 0.0F || e != (double) 0.0F || f != (double) 0.0F) && d * d + e * e + f * f < MAXIMUM_COLLISION_VELOCITY_SQUARED) {
			Vec3 vec3 = new Vec3(d, e, f);
			Vec3 location = new Vec3(this.x, this.y, this.z);
			Vec3 shipMovement = VSClientUtils.adjustEntityMovementForShipCollisions(null, vec3, asyncparticles$weathersAABB, level);
			if (shipMovement == null) {
				vec3 = Entity.collideBoundingBox(null, vec3, asyncparticles$weathersAABB, this.level, List.of());
			} else {
				asyncparticles$shipCollision(location, shipMovement);
				vec3 = shipMovement;
			}
			d = vec3.x;
			e = vec3.y;
			f = vec3.z;
		}

		if (d != (double) 0.0F || e != (double) 0.0F || f != (double) 0.0F) {
			asyncparticles$weathersAABB = asyncparticles$weathersAABB.move(d, e, f);
			this.setBoundingBox(this.getBoundingBox().move(d, e, f));
			this.setLocationFromBoundingbox();
		}

		if (Math.abs(h) >= (double) 1.0E-5F && Math.abs(e) < (double) 1.0E-5F) {
			this.stoppedByCollision = true;
		}

		if (h != e && h < (double) 0.0F){
			this.onGround = true;
		}

		if (g != d) {
			this.xd = 0.0;
		}

		if (i != f) {
			this.zd = 0.0;
		}

	}

	@Unique
	protected void asyncparticles$shipCollision(Vec3 location, Vec3 movement) {
		Minecraft mc = Minecraft.getInstance();
		ShipHitResult hit = VSClientUtils.clipShip(level, new ClipContext(location,
				location.add(movement).add(movement.normalize().scale(asyncparticles$weathersAABB.getSize())),
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.ANY,
				mc.player),
			true);
		if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
			Vec3 spawnPos = hit.getLocation();
//			BlockState blockState = level.getBlockState(hit.getBlockPos());
			FluidState fluidState = level.getFluidState(hit.getBlockPos());
			if (config.doRippleParticles && fluidState.isSourceOfType(Fluids.WATER)) {
//						System.out.println("RIPPLE!" + hit.getBlockPos() + " " + spawnPos + " " + blockState + " " + fluidState);
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
			} else
//					if (config.doSmokeParticles && (blockState.is(BlockTags.INFINIBURN_OVERWORLD) || blockState.is(BlockTags.STRIDER_WARM_BLOCKS))) {
//					Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.SMOKE, spawnPos.x, spawnPos.y, spawnPos.z, 0, 0, 0);
//					if (level.isThundering())
//						Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.LARGE_SMOKE, spawnPos.x, spawnPos.y, spawnPos.z, 0, 0, 0);
//				} else
				if (config.doSplashParticles && fluidState.isEmpty()) {
					mc.particleEngine.createParticle(ParticleTypes.RAIN, spawnPos.x, spawnPos.y, spawnPos.z, 0, 0, 0);
				}
		}
	}
}

package fun.qu_an.minecraft.asyncparticles.client.compat.physicsmod;

import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateUtil;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.ShipHitResult;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.RainEffect;
import fun.qu_an.minecraft.asyncparticles.client.util.CollisionType;
import net.diebuddies.physics.snow.math.AABB3D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import static java.lang.Math.abs;

public class PhysicsModCompat {
	public static boolean isCollideWithShip(ClientLevel level, Vec3 movement, AABB3D aabb) {
		Vector3d min = aabb.getMin();
		Vector3d max = aabb.getMax();
		return VSClientUtils.isEntityMovColShipOnly(
			movement,
			new AABB(min.x, min.y, min.z, max.x, max.y, max.z),
			level);
	}

	public static void onShipCollide(ClientLevel level, Vec3 location, Vec3 movement) {
		RainEffect vsRainEffect = ConfigHelper.getVSRainEffect();
		if (vsRainEffect == RainEffect.NONE || level.random.nextFloat() > 0.1) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		ShipHitResult hit = VSClientUtils.clipShip(level, new ClipContext(location,
				location.add(movement.scale(2)),
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.ANY,
				mc.player),
			true);
		if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
			return;
		}
		Vec3 shipMotion = hit.shipMotion;
		if (vsRainEffect != RainEffect.ALWAYS && abs(shipMotion.lengthSqr()) > 0.01) {
			return;
		}
		Vec3 spawnPos = hit.getLocation().add(shipMotion);
		FluidState fluidState = level.getFluidState(hit.getBlockPos());
		if (fluidState.isEmpty()) {
			mc.particleEngine.createParticle(ParticleTypes.RAIN, spawnPos.x, spawnPos.y, spawnPos.z, 0, 0, 0);
		}
	}

	public static CollisionType isCollideWithContraption(ClientLevel level, Vec3 movement, AABB aabb) {
		return CreateUtil.isCollideWithContraption(level, movement, aabb, true);
	}

	public static void onContraptionCollide(ClientLevel level, Vec3 location, Vec3 movement, CollisionType collisionType) {
		RainEffect createRainEffect = ConfigHelper.getCreateRainEffect();
		if (createRainEffect == RainEffect.NONE ||
			level.random.nextFloat() > 0.1) {
			return;
		}
		BlockHitResult hit = CreateUtil.clip(level, location, location.add(movement.scale(2)));
		if (hit == null || hit.getType() != HitResult.Type.BLOCK ||
			!collisionType.canSpawnRainEffect(createRainEffect)) {
			return;
		}
		Vec3 spawnPos = hit.location;
		Minecraft.getInstance().level.addParticle(
			ParticleTypes.RAIN,
			spawnPos.x,
			spawnPos.y,
			spawnPos.z,
			0,
			0,
			0);
	}
}

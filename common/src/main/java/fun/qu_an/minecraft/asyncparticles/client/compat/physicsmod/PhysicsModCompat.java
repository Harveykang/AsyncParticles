package fun.qu_an.minecraft.asyncparticles.client.compat.physicsmod;

import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.ShipHitResult;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
import net.diebuddies.physics.snow.math.AABB3D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class PhysicsModCompat {
	public static boolean isCollideWithShip(ClientLevel level, Vec3 movement, AABB3D aabb) {
		Vector3d min = aabb.getMin();
		Vector3d max = aabb.getMax();
		return VSClientUtils.isEntityMovColShipOnly(null,
			movement,
			new AABB(min.x, min.y, min.z, max.x, max.y, max.z),
			level);
	}

	public static void onShipCollide(ClientLevel level, Vec3 location, Vec3 movement) {
		if (level.random.nextFloat() > 0.1) {
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
		if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
			Vec3 spawnPos = hit.getLocation();
			FluidState fluidState = level.getFluidState(hit.getBlockPos());
			if (fluidState.isEmpty()) {
				mc.particleEngine.createParticle(ParticleTypes.RAIN, spawnPos.x, spawnPos.y, spawnPos.z, 0, 0, 0);
			}
		}
	}

	public static boolean collideWithContraptions(ClientLevel level, Vec3 movement, AABB3D aabb, boolean rain) {
		Vector3d min = aabb.getMin();
		Vector3d max = aabb.getMax();
		Vec3 clipMotion = CreateCompat.collideMotionWithContraptions(level,
			movement,
			new AABB(min.x - 0.1, min.y - 0.1, min.z - 0.1, max.x + 0.1, max.y + 0.1, max.z + 0.1));
		if (clipMotion == null) {
			return false;
		}
		if (!rain || level.random.nextFloat() > 0.1) {
			return true;
		}
		double centerX = min.x + aabb.getWidth() / 2;
		double centerZ = min.z + aabb.getDepth() / 2;
		Vec3 startPos = new Vec3(centerX, min.y, centerZ);
		Vec3 spawnPos = startPos.add(clipMotion);
		Minecraft.getInstance().particleEngine.createParticle(
			ParticleTypes.RAIN,
			spawnPos.x,
			spawnPos.y,
			spawnPos.z,
			0,
			0,
			0);
		return true;
	}
}

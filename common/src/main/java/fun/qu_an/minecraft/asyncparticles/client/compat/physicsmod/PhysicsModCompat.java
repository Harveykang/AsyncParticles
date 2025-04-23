package fun.qu_an.minecraft.asyncparticles.client.compat.physicsmod;

import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateUtil;
import net.diebuddies.physics.snow.math.AABB3D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class PhysicsModCompat {
	public static boolean collideWithContraptions(ClientLevel level, Vec3 movement, AABB3D aabb, boolean rain) {
		Vector3d min = aabb.getMin();
		Vector3d max = aabb.getMax();
		Vec3 clipMotion = CreateUtil.collideMotionWithContraptions(level,
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

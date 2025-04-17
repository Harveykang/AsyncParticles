package fun.qu_an.minecraft.asyncparticles.client.compat.physicsmod;

import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateCompat;
import net.diebuddies.physics.snow.math.AABB3D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class PhysicsModCompat {

	public static void onContraptionCollision(ClientLevel level, Vec3 pos, Vec3 movement, AABB3D aabb) {
		if (level.random.nextFloat() > 0.1) {
			return;
		}
		Vector3d min = aabb.getMin();
		Vector3d max = aabb.getMax();
		Vec3 clipMotion = CreateCompat.collideMotionWithContraptions(level,
			pos,
			movement,
			new AABB(min.x, min.y, min.z, max.x, max.y, max.z));
		if (clipMotion == null) {
			return;
		}
		double centerX = min.x + aabb.getWidth() / 2;
		double centerZ = min.z + aabb.getDepth() / 2;
		Vec3 startPos = new Vec3(centerX, min.y, centerZ);
		Vec3 spawnPos = startPos.add(clipMotion);
		Minecraft.getInstance().particleEngine
			.createParticle(ParticleTypes.RAIN, spawnPos.x, spawnPos.y, spawnPos.z, 0, 0, 0);
	}

	public static boolean isCollideWithContraptions(ClientLevel level, Vec3 pos, Vec3 movement, AABB3D aabb) {
		Vector3d min = aabb.getMin();
		Vector3d max = aabb.getMax();
		return CreateCompat.isCollideWithContraption(level,
			pos,
			movement,
			new AABB(min.x, min.y, min.z, max.x, max.y, max.z));
	}
}

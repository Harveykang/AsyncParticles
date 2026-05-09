package fun.qu_an.minecraft.asyncparticles.client.compat.create;

import dev.architectury.injectables.annotations.ExpectPlatform;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.Iterator;

import static java.lang.Math.abs;

public class CollideUtil {
	public static CollisionType isCollideWithContraptions(ClientLevel level, Vec3 motion, AABB bb) {
		return isCollideWithContraptions(level, motion, bb, true);
	}

	public static CollisionType isCollideWithContraptions(ClientLevel level, Vec3 motion, AABB bb, boolean estimate) {
		for (Iterator<Entity> it = CreateUtil.forEachContraption(level); it.hasNext(); ) {
			Entity contraptionEntity = it.next();
			CollisionType collisionType = CollideUtil.isCollideWithContraption(motion, bb, contraptionEntity, estimate);
			if (collisionType != CollisionType.NONE) {
				return collisionType;
			}
		}
		return CollisionType.NONE;
	}

	@Nullable
	@ExpectPlatform
	public static CollisionType isCollideWithContraption(Vec3 motion, AABB bb, Entity contraptionEntity, boolean estimate) {
		ExceptionUtil.throwAssertionError();
		return null;
	}

	@Nullable
	public static Vec3 collideMotionWithContraptions(ClientLevel level, Vec3 motion, AABB bounds) {
		Vector3d result = new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
		AABB finalBounds = bounds.inflate(0.1);
		for (Iterator<Entity> it = CreateUtil.forEachContraption(level); it.hasNext(); ) {
			Entity entity = it.next();
			if (!((ContraptionEntityAddon) entity).asyncparticles$isParticleCollision()) {
				continue;
			}
			Vec3 vec3 = collideMotionWithContraption(motion, finalBounds, entity, false);
			if (vec3 != null) {
				result.set(abs(result.x) < abs(vec3.x) ? result.x : vec3.x,
					abs(result.y) < abs(vec3.y) ? result.y : vec3.y,
					abs(result.z) < abs(vec3.z) ? result.z : vec3.z);
			}
		}
		if (result.x == Double.MAX_VALUE
			|| (motion.x == result.x && motion.y == result.y && motion.z == result.z)) {
			return null;
		}
		return new Vec3(result.x, result.y, result.z);
	}

	@Nullable
	public static Vec3 collideMotionWithContraption(Vec3 originalMotion,
	                                                AABB particleBounds,
	                                                Entity entity) {
		return collideMotionWithContraption(originalMotion, particleBounds, entity, true);
	}

	@Nullable
	@ExpectPlatform
	public static Vec3 collideMotionWithContraption(Vec3 originalMotion,
	                                                AABB particleBounds,
	                                                Entity entity,
	                                                boolean estimate) {
		ExceptionUtil.throwAssertionError();
		return null;
	}

	@Nullable
	@ExpectPlatform
	public static BlockHitResult rayCast(ClientLevel level, Vec3 start, Vec3 end) {
		ExceptionUtil.throwAssertionError();
		return null;
	}

	@Nullable
	@ExpectPlatform
	public static ContraptionHitResult rayCastWithContactPointMotion(ClientLevel level, Vec3 start, Vec3 end) {
		ExceptionUtil.throwAssertionError();
		return null;
	}
}
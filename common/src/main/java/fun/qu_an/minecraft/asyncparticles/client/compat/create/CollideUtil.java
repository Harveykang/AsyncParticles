package fun.qu_an.minecraft.asyncparticles.client.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import com.simibubi.create.content.contraptions.ContraptionHandlerClient;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.collision.Matrix3d;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static java.lang.Math.*;

public class CollideUtil {
	@Nullable
	public static Vec3 collideMotionWithContraptions(ClientLevel level, Vec3 motion, AABB bounds) {
		Vector3d result = new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
		AABB finalBounds = bounds.inflate(0.1);
		for (Iterator<AbstractContraptionEntity> it = CreateUtil.forEachContraption(level); it.hasNext(); ) {
			AbstractContraptionEntity entity = it.next();
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

	public static CollisionType isCollideWithContraption(Vec3 originalMotion,
	                                                     AABB particleBound,
	                                                     AbstractContraptionEntity contraptionEntity,
	                                                     boolean estimate) {
		// bro why the train's contraption out of bounds?
		AABB bb0;
		AABB entityBoundingBox = contraptionEntity instanceof CarriageContraptionEntity
			? (bb0 = CreateUtil.getBoundingBox(contraptionEntity)).inflate(0, max(max(bb0.getXsize(), bb0.getZsize()) - bb0.getYsize() * 0.3, 0), 0)
			: CreateUtil.getBoundingBox(contraptionEntity);
		if (!particleBound.expandTowards(originalMotion).intersects(entityBoundingBox)) {
			return CollisionType.NONE;
		}

		// Init matrix
		AbstractContraptionEntity.ContraptionRotationState rotation = contraptionEntity.getRotationState();
		Matrix3d rotationMatrix = CreateUtil.asMatrix(contraptionEntity, rotation);

		// Transform entity position and motion to local space
		Vec3 center = particleBound.getCenter();
		float yawOffset = rotation.getYawOffset();
		Vec3 anchorVec = CreateUtil.getAnchorVec(contraptionEntity);
		Vec3 toLocalTranslation = getWorldToLocalTranslation(center, anchorVec, rotationMatrix, yawOffset);

		Vec3 contactPointMotion = CreateUtil.getContactPointMotion(contraptionEntity, center);
		Vec3 localMotion = rotationMatrix.transform(originalMotion.subtract(contactPointMotion));

		// Prepare entity bounds
		AABB localBB = particleBound.move(toLocalTranslation).inflate(1.0E-7D);

		// Use simplified bbs when present
//		final Vec3 motionCopy = motion;
		Contraption contraption = contraptionEntity.getContraption();

		// Use simplified bbs if present
		Optional<List<AABB>> collisionShapes = contraption.getSimplifiedEntityColliders();
		List<AABB> collidableBBs;
		if (collisionShapes.isPresent()) {
			collidableBBs = collisionShapes.get();
		} else if (estimate) {
			return abs(contactPointMotion.x) + abs(contactPointMotion.y) + abs(contactPointMotion.z) > CreateUtil.LENGTH_SQR_EPSILON ?
				CollisionType.MOVING : CollisionType.STATIONARY;
		} else {
			collidableBBs = ((ContraptionAddon) contraption).asyncparticles$getAabbs();
		}

		AABB localExpanded = localBB.expandTowards(localMotion);
		// Incorrect but good performance
		for (AABB bb : collidableBBs) {
			if (localExpanded.intersects(bb)) {
				return abs(contactPointMotion.x) + abs(contactPointMotion.y) + abs(contactPointMotion.z) > 0.005d ?
					CollisionType.MOVING : CollisionType.STATIONARY;
			}
		}

		return CollisionType.NONE;
	}

	@Nullable
	public static Vec3 collideMotionWithContraption(Vec3 originalMotion,
	                                                AABB particleBounds,
	                                                AbstractContraptionEntity contraptionEntity,
	                                                boolean estimate) {
		// bro why the train's contraption out of bounds?
		AABB bb0;
		AABB entityBoundingBox = contraptionEntity instanceof CarriageContraptionEntity
			? (bb0 = CreateUtil.getBoundingBox(contraptionEntity)).inflate(0, max(max(bb0.getXsize(), bb0.getZsize()) - bb0.getYsize() * 0.3, 0), 0)
			: CreateUtil.getBoundingBox(contraptionEntity);
		if (!particleBounds.expandTowards(originalMotion).intersects(entityBoundingBox)) {
			return null;
		}

		// Init matrix
		AbstractContraptionEntity.ContraptionRotationState rotation = contraptionEntity.getRotationState();
		Matrix3d rotationMatrix = CreateUtil.asMatrix(contraptionEntity, rotation);

		// Transform entity position and motion to local space
		Vec3 center = particleBounds.getCenter();
		float yawOffset = rotation.getYawOffset();
		Vec3 anchorVec = CreateUtil.getAnchorVec(contraptionEntity);
		Vec3 toLocalTranslation = getWorldToLocalTranslation(center, anchorVec, rotationMatrix, yawOffset);

		Vec3 contactPointMotion = CreateUtil.getContactPointMotion(contraptionEntity, center);
		Vec3 localMotion = rotationMatrix.transform(originalMotion.subtract(contactPointMotion));

		// Prepare entity bounds
		AABB localBB = particleBounds.move(toLocalTranslation).inflate(1.0E-7D);

		// Use simplified bbs when present
//		final Vec3 motionCopy = motion;
		Contraption contraption = contraptionEntity.getContraption();

		// Use simplified bbs if present
		Optional<List<AABB>> collisionShapes = contraption.getSimplifiedEntityColliders();
		List<AABB> collidableBBs;
		if (collisionShapes.isPresent()) {
			collidableBBs = collisionShapes.get();
		} else if (estimate) {
			return Vec3.ZERO;
		} else {
			collidableBBs = ((ContraptionAddon) contraption).asyncparticles$getAabbs();
		}

		Vec3 localCenter = localBB.getCenter();
		double cx = localMotion.x;
		double cy = localMotion.y;
		double cz = localMotion.z;
		double sx = 0;
		double sy = 0;
		double sz = 0;
		boolean squeezed = false;
		double localXsize = localBB.getXsize();
		double localYsize = localBB.getYsize();
		double localZsize = localBB.getZsize();
		// this is buggy, but works
		AABB localExpanded = localBB.expandTowards(localMotion);
		for (AABB bb : collidableBBs) {
			if (!localExpanded.intersects(bb)) {
				continue;
			}

			if (localBB.intersects(bb)) {
				Vec3 bbCenter = bb.getCenter();
				squeezed = true;
				AABB intersect = localBB.intersect(bb);

				double intersectXsize = intersect.getXsize();
				double intersectYsize = intersect.getYsize();
				double intersectZsize = intersect.getZsize();
				Direction.Axis squeezedAxis = getSqueezedAxis(intersectXsize, intersectYsize, intersectZsize);

				switch (squeezedAxis) {
					case X -> sx = getSqueezed(localCenter.x, bbCenter.x, intersectXsize, sx);
					case Y -> sy = getSqueezed(localCenter.y, bbCenter.y, intersectYsize, originalMotion.y > 0 ? cy : sy);
					case Z -> sz = getSqueezed(localCenter.z, bbCenter.z, intersectZsize, sz);
				}
			} else if (!squeezed) {
				Vec3 bbCenter = bb.getCenter();
				Vec3 relative = bbCenter.subtract(localCenter);

				double halfXsum = (bb.getXsize() + localXsize) * 0.5;
				double halfYsum = (bb.getYsize() + localYsize) * 0.5;
				double halfZsum = (bb.getZsize() + localZsize) * 0.5;
				Direction.Axis collidedAxis = getCollideAxis(halfXsum, halfYsum, halfZsum, relative);

				switch (collidedAxis) {
					case X -> cx = getCollided(relative.x, halfXsum, cx);
					case Y -> cy = getCollided(relative.y, halfYsum, cy);
					case Z -> cz = getCollided(relative.z, halfZsum, cz);
				}
			}
		}

		Vec3 clippedLocal;
		if (squeezed) {
			clippedLocal = new Vec3(sx, sy, sz);
		} else {
			clippedLocal = new Vec3(cx, cy, cz);
			if (localMotion.equals(clippedLocal)) {
				return null;
			}
		}
		rotationMatrix.transpose();
		Vec3 clipped = rotationMatrix.transform(clippedLocal);
		rotationMatrix.transpose();
		double x = signum(contactPointMotion.x) != signum(originalMotion.x) ||
			abs(clipped.x) < abs(contactPointMotion.x) ?
			contactPointMotion.x * 3 : contactPointMotion.x;
		double y = signum(contactPointMotion.y) != signum(originalMotion.y) ||
			abs(clipped.y) < abs(contactPointMotion.y) ?
			contactPointMotion.y * 3 : contactPointMotion.y;
		double z = signum(contactPointMotion.z) != signum(originalMotion.z) ||
			abs(clipped.z) < abs(contactPointMotion.z) ?
			contactPointMotion.z * 3 : contactPointMotion.z;
		return clipped.add(x, y, z);
	}

	public static Vec3 getWorldToLocalTranslation(Vec3 entityCenter,
	                                              Vec3 anchorVec,
	                                              Matrix3d rotationMatrix,
	                                              float yawOffset) {
		Vec3 position = ContraptionCollider.worldToLocalPos(entityCenter, anchorVec, rotationMatrix, yawOffset);
		return position.subtract(entityCenter);
	}

	private static Direction.@NotNull Axis getCollideAxis(double halfXsum, double halfYsum, double halfZsum, Vec3 relative) {
		double sx = halfXsum - abs(relative.x);
		double sy = halfYsum - abs(relative.y);
		double sz = halfZsum - abs(relative.z);
		if (sx < sy) {
			if (sx < sz) {
				return Direction.Axis.X;
			} else {
				return Direction.Axis.Z;
			}
		} else {
			if (sy < sz) {
				return Direction.Axis.Y;
			} else {
				return Direction.Axis.Z;
			}
		}
	}

	private static double getCollided(double relative, double halfXsum, double mx) {
		double dx = relative > 0 ? relative - halfXsum : relative + halfXsum;
		if (Math.abs(mx) > Math.abs(dx)) {
			mx = dx;
		}
		return mx;
	}

	private static double getSqueezed(double localCenter, double bbCenter, double intersectSize, double currentSqueezed) {
		double diff = localCenter - bbCenter;
		double halfIntersectSize = intersectSize * 0.5;
		if (diff < -halfIntersectSize) {
			return Math.min(currentSqueezed, -halfIntersectSize - diff);
		} else if (diff > halfIntersectSize) {
			return Math.max(currentSqueezed, halfIntersectSize - diff);
		} else {
			return currentSqueezed;
		}
	}

	private static Direction.Axis getSqueezedAxis(double xsize, double ysize, double zsize) {
		if (xsize < ysize) {
			if (xsize < zsize) {
				return Direction.Axis.X;
			} else {
				return Direction.Axis.Z;
			}
		} else {
			if (ysize < zsize) {
				return Direction.Axis.Y;
			} else {
				return Direction.Axis.Z;
			}
		}
	}

	public static CollisionType isCollideWithContraptions(ClientLevel level, Vec3 motion, AABB bb) {
		return isCollideWithContraptions(level, motion, bb, true);
	}

	public static CollisionType isCollideWithContraptions(ClientLevel level, Vec3 motion, AABB bb, boolean estimate) {
		for (Iterator<AbstractContraptionEntity> it = CreateUtil.forEachContraption(level); it.hasNext(); ) {
			AbstractContraptionEntity contraptionEntity = it.next();
			CollisionType collisionType = CollideUtil.isCollideWithContraption(motion, bb, contraptionEntity, estimate);
			if (collisionType != CollisionType.NONE) {
				return collisionType;
			}
		}
		return CollisionType.NONE;
	}


	public static BlockHitResult rayCast(ClientLevel level, Vec3 start, Vec3 end) {
		double shortestDistance = Double.MAX_VALUE;
		BlockHitResult hitResult = null;
		Vec3 hit = null;
		for (Iterator<AbstractContraptionEntity> it = CreateUtil.forEachContraption(level); it.hasNext(); ) {
			AbstractContraptionEntity entity = it.next();
			BlockHitResult hitResult1 = ContraptionHandlerClient.rayTraceContraption(start, end, entity);
			if (hitResult1 != null && hitResult1.getType() != HitResult.Type.MISS) {
				Vec3 hit1 = entity.toGlobalVector(hitResult1.getLocation(), 1.0F);
				double hitDiff = start.y - hit1.y;
				if (shortestDistance > hitDiff) {
					hitResult = hitResult1;
					hit = hit1;
				}
			}
		}
		if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
			return null;
		}
		return new BlockHitResult(hit, hitResult.getDirection(), BlockPos.containing(hit), hitResult.isInside());
	}

	@Nullable
	public static ContraptionHitResult rayCastWithContactPointMotion(ClientLevel level, Vec3 start, Vec3 end) {
		double shortestDistance = Double.MAX_VALUE;
		BlockHitResult hitResult = null;
		Vec3 hit = null;
		AbstractContraptionEntity entity = null;
		for (Iterator<AbstractContraptionEntity> it = CreateUtil.forEachContraption(level); it.hasNext(); ) {
			AbstractContraptionEntity entity1 = it.next();
			AABB bb0;
			AABB entity1Bb = entity1 instanceof CarriageContraptionEntity
				? (bb0 = CreateUtil.getBoundingBox(entity1)).inflate(0, max(max(bb0.getXsize(), bb0.getZsize()) - bb0.getYsize() * 0.3, 0), 0)
				: CreateUtil.getBoundingBox(entity1);
			if (!entity1Bb.intersects(start, end)) {
				continue;
			}
			BlockHitResult hitResult1 = ContraptionHandlerClient.rayTraceContraption(start, end, entity1);
			if (hitResult1 != null) {
				Vec3 hit1 = entity1.toGlobalVector(hitResult1.getLocation(), 1.0F);
				double hitDiff = start.y - hit1.y;
				if (shortestDistance > hitDiff) {
					hitResult = hitResult1;
					hit = hit1;
					entity = entity1;
				}
			}
		}
		if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
			return null;
		}
		return new ContraptionHitResult(CreateUtil.getContactPointMotion(entity, hit),
			hit,
			hitResult.getDirection(),
			BlockPos.containing(hit),
			hitResult.isInside());
	}
}

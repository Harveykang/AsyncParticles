package fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.collision.CollisionList;
import com.simibubi.create.foundation.collision.Matrix3d;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CollisionType;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.ContraptionEntityAddon;
import fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.create.InvokerContraptionCollider;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.Iterator;

import static java.lang.Math.abs;

public class CollideUtil {
	private static final float LENGTH_SQR_EPSILON = 0.01f;

	@Nullable
	public static Vec3 collideMotionWithContraptions(ClientLevel level, Vec3 motion, AABB bounds) {
		Vector3d result = new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
		AABB finalBounds = bounds.inflate(0.1);
		for (Iterator<AbstractContraptionEntity> it = CreateUtilImpl.forEachContraption(level); it.hasNext(); ) {
			AbstractContraptionEntity entity = it.next();
			if (!((ContraptionEntityAddon) entity).asyncparticles$doParticleCollision()) {
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

	public static CollisionType isCollideWithContraption(Vec3 originalMotion, AABB particleBound, AbstractContraptionEntity contraptionEntity, boolean estimate) {
		// 第一阶段：出界及基础判断
		AABB bb0;
		AABB entityBoundingBox = contraptionEntity instanceof CarriageContraptionEntity
			? (bb0 = contraptionEntity.getBoundingBox()).inflate(0, Math.max(Math.max(bb0.getXsize(), bb0.getZsize()) - bb0.getYsize() * 0.3, 0), 0)
			: contraptionEntity.getBoundingBox();

		if (!particleBound.expandTowards(originalMotion).intersects(entityBoundingBox)) {
			return CollisionType.NONE;
		}

		// 第二阶段：初始化矩阵与局部坐标转换
		Level world = contraptionEntity.level();
		Contraption contraption = contraptionEntity.getContraption();
		if (contraption == null) return CollisionType.NONE;

		AbstractContraptionEntity.ContraptionRotationState rotation = contraptionEntity.getRotationState();
		Matrix3d rotationMatrix = rotation.asMatrix();

		Vec3 particleBoundsCenter = particleBound.getCenter();
		float yawOffset = rotation.getYawOffset();
		Vec3 anchorVec = contraptionEntity.getAnchorVec();
		Vec3 toLocalTranslation = TransformUtil.worldToLocalDisplacement(particleBoundsCenter, anchorVec, rotationMatrix, yawOffset);

		Vec3 contactPointMotion = contraptionEntity.getContactPointMotion(particleBoundsCenter);
		Vec3 localMotion = rotationMatrix.transform(originalMotion.subtract(contactPointMotion));

		AABB localBB = particleBound.move(toLocalTranslation).inflate(1.0E-7D);

		// 第三阶段：获取碰撞体数据
		CollisionList collidableBBs = contraption.getSimplifiedEntityColliders();

		if (collidableBBs == null) {
			if (estimate) {
				// 估算模式下，如果有接触点运动则认为是移动碰撞，否则是静止碰撞
				return contactPointMotion.lengthSqr() > LENGTH_SQR_EPSILON ? CollisionType.MOVING : CollisionType.STATIONARY;
			}
			collidableBBs = new CollisionList();
			InvokerContraptionCollider.invoker_getPotentiallyCollidedShapes(world, contraption, localBB.expandTowards(localMotion), new CollisionList.Populate(collidableBBs));
		}

		// 第四阶段：快速碰撞检测
		double localMinX = localBB.minX;
		double localMinY = localBB.minY;
		double localMinZ = localBB.minZ;
		double localMaxX = localBB.maxX;
		double localMaxY = localBB.maxY;
		double localMaxZ = localBB.maxZ;

		double lMotionX = localMotion.x;
		double lMotionY = localMotion.y;
		double lMotionZ = localMotion.z;
		double lexpMinX = lMotionX < 0 ? localMinX + lMotionX : localMinX;
		double lexpMaxX = lMotionX > 0 ? localMaxX + lMotionX : localMaxX;
		double lexpMinY = lMotionY < 0 ? localMinY + lMotionY : localMinY;
		double lexpMaxY = lMotionY > 0 ? localMaxY + lMotionY : localMaxY;
		double lexpMinZ = lMotionZ < 0 ? localMinZ + lMotionZ : localMinZ;
		double lexpMaxZ = lMotionZ > 0 ? localMaxZ + lMotionZ : localMaxZ;

		boolean hasCollision = false;
		for (int i = 0; i < collidableBBs.size; i++) {
			double bbCx = collidableBBs.centerX[i];
			double bbCy = collidableBBs.centerY[i];
			double bbCz = collidableBBs.centerZ[i];
			double ex = collidableBBs.extentsX[i];
			double ey = collidableBBs.extentsY[i];
			double ez = collidableBBs.extentsZ[i];

			double bbMinX = bbCx - ex;
			double bbMaxX = bbCx + ex;
			double bbMinY = bbCy - ey;
			double bbMaxY = bbCy + ey;
			double bbMinZ = bbCz - ez;
			double bbMaxZ = bbCz + ez;

			// 检查是否相交
			if (!(lexpMinX < bbMaxX && lexpMaxX > bbMinX &&
				lexpMinY < bbMaxY && lexpMaxY > bbMinY &&
				lexpMinZ < bbMaxZ && lexpMaxZ > bbMinZ)) {
				continue;
			}

			hasCollision = true;
			break;
		}

		if (!hasCollision) {
			return CollisionType.NONE;
		}

		// 有碰撞时，根据接触点运动判断是移动还是静止碰撞
		return contactPointMotion.lengthSqr() > LENGTH_SQR_EPSILON ? CollisionType.MOVING : CollisionType.STATIONARY;
	}

	@Nullable
	public static Vec3 collideMotionWithContraption(Vec3 originalMotion,
	                                                AABB particleBounds,
	                                                AbstractContraptionEntity contraptionEntity,
	                                                boolean estimate) {
		AABB bb0;
		AABB entityBoundingBox = contraptionEntity instanceof CarriageContraptionEntity
			? (bb0 = contraptionEntity.getBoundingBox()).inflate(0, Math.max(Math.max(bb0.getXsize(), bb0.getZsize()) - bb0.getYsize() * 0.3, 0), 0)
			: contraptionEntity.getBoundingBox();

		if (!particleBounds.expandTowards(originalMotion).intersects(entityBoundingBox)) {
			return null;
		}

		Level world = contraptionEntity.level();
		Contraption contraption = contraptionEntity.getContraption();
		if (contraption == null) return null;

		AbstractContraptionEntity.ContraptionRotationState rotation = contraptionEntity.getRotationState();
		Matrix3d rotationMatrix = rotation.asMatrix();

		Vec3 particleBoundsCenter = particleBounds.getCenter();
		float yawOffset = rotation.getYawOffset();
		Vec3 anchorVec = contraptionEntity.getAnchorVec();
		Vec3 toLocalTranslation = TransformUtil.worldToLocalDisplacement(particleBoundsCenter, anchorVec, rotationMatrix, yawOffset);

		Vec3 contactPointMotion = contraptionEntity.getContactPointMotion(particleBoundsCenter);
		Vec3 localMotion = rotationMatrix.transform(originalMotion.subtract(contactPointMotion));

		AABB localBB = particleBounds.move(toLocalTranslation).inflate(1.0E-7D);

		CollisionList collidableBBs = contraption.getSimplifiedEntityColliders();

		if (collidableBBs == null) {
			if (estimate) {
				return Vec3.ZERO;
			}
			collidableBBs = new CollisionList();
			InvokerContraptionCollider.invoker_getPotentiallyCollidedShapes(world, contraption, localBB.expandTowards(localMotion), new CollisionList.Populate(collidableBBs));
		}

		double localMinX = localBB.minX;
		double localMinY = localBB.minY;
		double localMinZ = localBB.minZ;
		double localMaxX = localBB.maxX;
		double localMaxY = localBB.maxY;
		double localMaxZ = localBB.maxZ;

		double halfLocalX = (localMaxX - localMinX) * 0.5;
		double halfLocalY = (localMaxY - localMinY) * 0.5;
		double halfLocalZ = (localMaxZ - localMinZ) * 0.5;

		double lcX = localMinX + halfLocalX;
		double lcY = localMinY + halfLocalY;
		double lcZ = localMinZ + halfLocalZ;

		double lMotionX = localMotion.x;
		double lMotionY = localMotion.y;
		double lMotionZ = localMotion.z;

		// localBB.expandTowards(localMotion)
		double lexpMinX = lMotionX < 0 ? localMinX + lMotionX : localMinX;
		double lexpMaxX = lMotionX > 0 ? localMaxX + lMotionX : localMaxX;
		double lexpMinY = lMotionY < 0 ? localMinY + lMotionY : localMinY;
		double lexpMaxY = lMotionY > 0 ? localMaxY + lMotionY : localMaxY;
		double lexpMinZ = lMotionZ < 0 ? localMinZ + lMotionZ : localMinZ;
		double lexpMaxZ = lMotionZ > 0 ? localMaxZ + lMotionZ : localMaxZ;

		double cx = lMotionX;
		double cy = lMotionY;
		double cz = lMotionZ;
		double sx = 0;
		double sy = 0;
		double sz = 0;
		boolean squeezed = false;

		for (int i = 0; i < collidableBBs.size; i++) {
			double bbCx = collidableBBs.centerX[i];
			double bbCy = collidableBBs.centerY[i];
			double bbCz = collidableBBs.centerZ[i];
			double ex = collidableBBs.extentsX[i];
			double ey = collidableBBs.extentsY[i];
			double ez = collidableBBs.extentsZ[i];

			double bbMinX = bbCx - ex;
			double bbMaxX = bbCx + ex;
			double bbMinY = bbCy - ey;
			double bbMaxY = bbCy + ey;
			double bbMinZ = bbCz - ez;
			double bbMaxZ = bbCz + ez;

			// !localExpanded.intersects(bb)
			if (!(lexpMinX < bbMaxX && lexpMaxX > bbMinX &&
				lexpMinY < bbMaxY && lexpMaxY > bbMinY &&
				lexpMinZ < bbMaxZ && lexpMaxZ > bbMinZ)) {
				continue;
			}

			// localBB.intersects(bb)
			boolean intersectsLocal = localMinX < bbMaxX && localMaxX > bbMinX &&
				localMinY < bbMaxY && localMaxY > bbMinY &&
				localMinZ < bbMaxZ && localMaxZ > bbMinZ;

			if (intersectsLocal) {
				squeezed = true;

				// 计算交叉区域的大小 (等价于 localBB.intersect(bb).getSize())
				double intersectXsize = Math.min(localMaxX, bbMaxX) - Math.max(localMinX, bbMinX);
				double intersectYsize = Math.min(localMaxY, bbMaxY) - Math.max(localMinY, bbMinY);
				double intersectZsize = Math.min(localMaxZ, bbMaxZ) - Math.max(localMinZ, bbMinZ);

				Direction.Axis squeezedAxis = getSqueezedAxis(intersectXsize, intersectYsize, intersectZsize);

				switch (squeezedAxis) {
					case X -> sx = getSqueezed(lcX, bbCx, intersectXsize, sx);
					case Y -> sy = getSqueezed(lcY, bbCy, intersectYsize, lMotionY > 0 ? lMotionY : sy);
					case Z -> sz = getSqueezed(lcZ, bbCz, intersectZsize, sz);
				}
			} else if (!squeezed) {
				// 获取相对坐标 (等价于 Vec3 relative = bbCenter.subtract(localCenter))
				double relX = bbCx - lcX;
				double relY = bbCy - lcY;
				double relZ = bbCz - lcZ;

				double halfXsum = ex + halfLocalX;
				double halfYsum = ey + halfLocalY;
				double halfZsum = ez + halfLocalZ;

				Direction.Axis collidedAxis = getCollideAxis(halfXsum, halfYsum, halfZsum, relX, relY, relZ);

				switch (collidedAxis) {
					case X -> cx = getCollided(relX, halfXsum, cx);
					case Y -> cy = getCollided(relY, halfYsum, cy);
					case Z -> cz = getCollided(relZ, halfZsum, cz);
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

		Vec3 clipped = rotationMatrix.transformTransposed(clippedLocal);
		double x = Math.signum(contactPointMotion.x) != Math.signum(originalMotion.x) ||
			Math.abs(clipped.x) < Math.abs(contactPointMotion.x) ?
			contactPointMotion.x * 3 : contactPointMotion.x;
		double y = Math.signum(contactPointMotion.y) != Math.signum(originalMotion.y) ||
			Math.abs(clipped.y) < Math.abs(contactPointMotion.y) ?
			contactPointMotion.y * 3 : contactPointMotion.y;
		double z = Math.signum(contactPointMotion.z) != Math.signum(originalMotion.z) ||
			Math.abs(clipped.z) < Math.abs(contactPointMotion.z) ?
			contactPointMotion.z * 3 : contactPointMotion.z;

		return clipped.add(x, y, z);
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

	private static Direction.@NotNull Axis getCollideAxis(double halfXsum, double halfYsum, double halfZsum, double relX, double relY, double relZ) {
		double sx = halfXsum - Math.abs(relX);
		double sy = halfYsum - Math.abs(relY);
		double sz = halfZsum - Math.abs(relZ);
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

	public static CollisionType isCollideWithContraptions(ClientLevel level, Vec3 motion, AABB bb) {
		return isCollideWithContraptions(level, motion, bb, true);
	}

	public static CollisionType isCollideWithContraptions(ClientLevel level, Vec3 motion, AABB bb, boolean estimate) {
		for (Iterator<AbstractContraptionEntity> it = CreateUtilImpl.forEachContraption(level); it.hasNext(); ) {
			AbstractContraptionEntity contraptionEntity = it.next();
			CollisionType collisionType = CollideUtil.isCollideWithContraption(motion, bb, contraptionEntity, estimate);
			if (collisionType != CollisionType.NONE) {
				return collisionType;
			}
		}
		return CollisionType.NONE;
	}
}
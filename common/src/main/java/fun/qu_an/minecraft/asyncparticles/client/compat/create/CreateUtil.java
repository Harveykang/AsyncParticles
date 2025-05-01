package fun.qu_an.minecraft.asyncparticles.client.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.collision.Matrix3d;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.mixin.create.InvokerContraptionCollider;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.lang.Math.*;

/**
 * See {@link ContraptionCollider}
 */
public class CreateUtil {
	public static Collection<WeakReference<AbstractContraptionEntity>> contraptions(LevelAccessor level) {
		return loadedContraptions(level).values();
	}

	/**
	 * @return null if no collision
	 */
	@Nullable
	public static Vec3 collideMotionWithContraptions(ClientLevel level, Vec3 motion, AABB bounds) {
		Vector3d result = new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
		AABB finalBounds = bounds.inflate(0.1);
		forEachContraption(level, contraptionEntity -> {
			Vec3 vec3 = collideMotionWithContraption(level, motion, finalBounds, contraptionEntity);
			if (vec3 != null) {
				result.set(abs(result.x) < abs(vec3.x) ? result.x : vec3.x,
					abs(result.y) < abs(vec3.y) ? result.y : vec3.y,
					abs(result.z) < abs(vec3.z) ? result.z : vec3.z);
			}
			return true;
		});
		if (result.x == Double.MAX_VALUE
			|| (motion.x == result.x && motion.y == result.y && motion.z == result.z)) {
			return null;
		}
		return new Vec3(result.x, result.y, result.z);
	}

	public static void forEachContraption(LevelAccessor level, Predicate<AbstractContraptionEntity> consumer) {
		try {
			for (WeakReference<AbstractContraptionEntity> r : contraptions(level)) {
				AbstractContraptionEntity contraptionEntity = r.get();
				if (contraptionEntity == null || !contraptionEntity.isAliveOrStale()) {
					continue;
				}
				if (!consumer.test(contraptionEntity)) {
					return;
				}
			}
		} catch (ConcurrentModificationException ignored) {
			// Ignore as they are not critical
		}
	}

	public static Iterator<AbstractContraptionEntity> forEachContraption(LevelAccessor level) {
		Iterator<WeakReference<AbstractContraptionEntity>> iterator = contraptions(level).iterator();
		return new Iterator<>() {
			private AbstractContraptionEntity next;

			@Override
			public boolean hasNext() {
				if (next != null) {
					return true;
				}
				while (iterator.hasNext()) {
					try {
						next = iterator.next().get();
					} catch (ConcurrentModificationException ignored) {
						// Ignore as they are not critical
						return false;
					}
					if (next != null && next.isAliveOrStale()) {
						break;
					}
				}
				return next != null;
			}

			@Override
			public AbstractContraptionEntity next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				AbstractContraptionEntity result = next;
				next = null;
				return result;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void forEachRemaining(Consumer<? super AbstractContraptionEntity> action) {
				while (hasNext()) {
					action.accept(next);
				}
			}
		};
	}

	public static Vec3 getWorldToLocalTranslation(Vec3 entityCenter,
												  Vec3 anchorVec,
												  Matrix3d rotationMatrix,
												  float yawOffset) {
		Vec3 position = ContraptionCollider.worldToLocalPos(entityCenter, anchorVec, rotationMatrix, yawOffset);
		return position.subtract(entityCenter);
	}

	/**
	 * 完全没搞懂，先能用，后面再优化吧
	 */
	public static boolean isCollideWithContraption(ClientLevel level,
												   Vec3 originalMotion,
												   AABB bounds,
												   AbstractContraptionEntity contraptionEntity) {
		return isCollideWithContraption(level, originalMotion, bounds, contraptionEntity, false);
	}

	/**
	 * 完全没搞懂，先能用，后面再优化吧
	 */
	public static boolean isCollideWithContraption(ClientLevel level,
												   Vec3 originalMotion,
												   AABB entityBounds,
												   AbstractContraptionEntity contraptionEntity,
												   boolean estimate) {
		// bro why the train's contraption out of bounds?
		AABB bb0;
		AABB entityBoundingBox = contraptionEntity instanceof CarriageContraptionEntity
			? (bb0 = contraptionEntity.getBoundingBox()).inflate(0, max(max(bb0.getXsize(), bb0.getZsize()) - bb0.getYsize() * 0.3, 0), 0)
			: contraptionEntity.getBoundingBox();
		if (!entityBounds.expandTowards(originalMotion).intersects(entityBoundingBox)) {
			return false;
		}

		// Init matrix
		AbstractContraptionEntity.ContraptionRotationState rotation = contraptionEntity.getRotationState();
		Matrix3d rotationMatrix = rotation.asMatrix();

		// Transform entity position and motion to local space
		Vec3 center = entityBounds.getCenter();
		Vec3 entityPosition = entityBounds.getCenter().add(0, entityBounds.getYsize() * 0.5, 0);
		float yawOffset = rotation.getYawOffset();
		Vec3 anchorVec = contraptionEntity.getAnchorVec();
		Vec3 toLocalTranslation = getWorldToLocalTranslation(center, anchorVec, rotationMatrix, yawOffset);

		Vec3 contactPointMotion = contraptionEntity.getContactPointMotion(entityPosition);
		Vec3 localMotion = rotationMatrix.transform(originalMotion.subtract(contactPointMotion));

		// Prepare entity bounds
		AABB localBB = entityBounds.move(toLocalTranslation).inflate(1.0E-7D);

		// Use simplified bbs when present
//		final Vec3 motionCopy = motion;
		Contraption contraption = contraptionEntity.getContraption();

		// Use simplified bbs if present
		AABB localExpanded = localBB.expandTowards(localMotion);
		Optional<List<AABB>> collisionShapes = contraption.getSimplifiedEntityColliders();
		List<AABB> collidableBBs;
		if (collisionShapes.isPresent()) {
			collidableBBs = collisionShapes.get();
		} else if (estimate) {
			return true; // No simplified bbs, use full entity bounds, this is a fallback for better performance
		} else {
			List<VoxelShape> shapes = InvokerContraptionCollider.invoker_getPotentiallyCollidedShapes(
				level, contraption, localExpanded);
			// TODO 这里完全不需要高精度形状，重写一个类似方法
			collidableBBs = new ArrayList<>();
			shapes.forEach(shape ->
				shape.forAllBoxes((d, e, f, g, h, i) ->
					collidableBBs.add(new AABB(d, e, f, g, h, i))));
		}

		// Incorrect but good performance
		for (AABB bb : collidableBBs) {
			if (localExpanded.intersects(bb)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * 完全没搞懂，先能用，后面再优化吧
	 */
	@Nullable
	public static Vec3 collideMotionWithContraption(ClientLevel level,
													Vec3 originalMotion,
													AABB bounds,
													AbstractContraptionEntity contraptionEntity) {
		return collideMotionWithContraption(level, originalMotion, bounds, contraptionEntity, false);
	}

	/**
	 * 完全没搞懂，先能用，后面再优化吧
	 */
	@Nullable
	public static Vec3 collideMotionWithContraption(ClientLevel level,
													Vec3 originalMotion,
													AABB entityBounds,
													AbstractContraptionEntity contraptionEntity,
													boolean estimate) {
		// bro why the train's contraption out of bounds?
		AABB bb0;
		AABB entityBoundingBox = contraptionEntity instanceof CarriageContraptionEntity
			? (bb0 = contraptionEntity.getBoundingBox()).inflate(0, max(max(bb0.getXsize(), bb0.getZsize()) - bb0.getYsize() * 0.3, 0), 0)
			: contraptionEntity.getBoundingBox();
		if (!entityBounds.expandTowards(originalMotion).intersects(entityBoundingBox)) {
			return null;
		}

		// Init matrix
		AbstractContraptionEntity.ContraptionRotationState rotation = contraptionEntity.getRotationState();
		Matrix3d rotationMatrix = rotation.asMatrix();

		// Transform entity position and motion to local space
		Vec3 center = entityBounds.getCenter();
		Vec3 entityPosition = entityBounds.getCenter().add(0, entityBounds.getYsize() * 0.5, 0);
		float yawOffset = rotation.getYawOffset();
		Vec3 anchorVec = contraptionEntity.getAnchorVec();
		Vec3 toLocalTranslation = getWorldToLocalTranslation(center, anchorVec, rotationMatrix, yawOffset);

		Vec3 contactPointMotion = contraptionEntity.getContactPointMotion(entityPosition);
		Vec3 localMotion = rotationMatrix.transform(originalMotion.subtract(contactPointMotion));

		// Prepare entity bounds
		AABB localBB = entityBounds.move(toLocalTranslation).inflate(1.0E-7D);

		// Use simplified bbs when present
//		final Vec3 motionCopy = motion;
		Contraption contraption = contraptionEntity.getContraption();

		// Use simplified bbs if present
		AABB localExpanded = localBB.expandTowards(localMotion);
		Optional<List<AABB>> collisionShapes = contraption.getSimplifiedEntityColliders();
		List<AABB> collidableBBs;
		if (collisionShapes.isPresent()) {
			collidableBBs = collisionShapes.get();
		} else if (estimate) {
			return Vec3.ZERO; // No simplified bbs, use full entity bounds, this is a fallback for better performance
		} else {
			List<VoxelShape> shapes = InvokerContraptionCollider.invoker_getPotentiallyCollidedShapes(
				level, contraption, localExpanded);
			// TODO 这里完全不需要高精度形状，重写一个类似方法
			collidableBBs = new ArrayList<>();
			shapes.forEach(shape ->
				shape.forAllBoxes((d, e, f, g, h, i) ->
					collidableBBs.add(new AABB(d, e, f, g, h, i))));
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
					case Y -> sy = getSqueezed(localCenter.y, bbCenter.y, intersectYsize, cy > 0 ? cy : sy); // fix stuck on roof
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
		Vec3 clipped = rotationMatrix.transpose().transform(clippedLocal);
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

	private static double getCollided(double relative, double halfXsum, double mx) {
		double dx = relative > 0 ? relative - halfXsum : relative + halfXsum;
		if (abs(mx) > abs(dx)) {
			mx = dx;
		}
		return mx;
	}

	private static double getSqueezed(double localCenter, double bbCenter, double intersectSize, double currentSqueezed) {
		double diff = localCenter - bbCenter;
		double halfIntersectSize = intersectSize * 0.5;
		if (diff < -halfIntersectSize) {
			return min(currentSqueezed, -halfIntersectSize - diff);
		} else if (diff > halfIntersectSize) {
			return max(currentSqueezed, halfIntersectSize - diff);
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

	public static Vec3 bounceEntity(Vec3 originalMotion, Vec3 contactPointMotion, Vec3 normal, double factor) {
		if (factor == 0) {
			return null;
		}
		Vec3 motion = originalMotion.subtract(contactPointMotion);
		Vec3 deltav = normal.scale(factor * 2 * motion.dot(normal));
		if (deltav.dot(deltav) < 0.1f) {
			return null;
		}
		return originalMotion.subtract(deltav);
	}

	public static Map<Integer, WeakReference<AbstractContraptionEntity>> loadedContraptions(LevelAccessor level) {
		return ModListHelper.IS_LEGACY_CREATE
			? Create5Util.loadedContraptions(level)
			: Create6Util.loadedContraptions(level);
	}

	public static boolean isUnderContraption(ClientLevel level, double x, double y, double z, double size) {
		AABB bounds = new AABB(x - size, y - size, z - size, x + size, y + size, z + size);
		return isCollideWithContraption(level, new Vec3(0, Math.max(16, level.getMaxBuildHeight() - y), 0), bounds);
	}

	public static boolean isUnderContraption(ClientLevel level, Vec3 pos, double size) {
		AABB bounds = new AABB(pos.x - size, pos.y - size, pos.z - size, pos.x + size, pos.y + size, pos.z + size);
		return isCollideWithContraption(level, new Vec3(0, Math.max(16, level.getMaxBuildHeight() - pos.y), 0), bounds);
	}

	public static boolean isCollideWithContraption(ClientLevel level, Vec3 motion, AABB bb) {
		return isCollideWithContraption(level, motion, bb, true);
	}

	public static boolean isCollideWithContraption(ClientLevel level, Vec3 motion, AABB bb, boolean estimate) {
		for (Iterator<AbstractContraptionEntity> it = forEachContraption(level); it.hasNext(); ) {
			AbstractContraptionEntity contraptionEntity = it.next();
			boolean b1 = isCollideWithContraption(level, motion, bb, contraptionEntity, estimate);
			if (b1) {
				return true;
			}
		}
		return false;
	}

	@Nullable
	public static Vec3 getContraptionDeltaMovement(Entity entity) {
		Entity rootEntity = entity.getRootVehicle();
		if (rootEntity instanceof AbstractContraptionEntity ace) {
			return ace.getContactPointMotion(entity.position());
		}
		for (Iterator<AbstractContraptionEntity> iterator = forEachContraption(rootEntity.level()); iterator.hasNext(); ) {
			AbstractContraptionEntity contraptionEntity = iterator.next();
			if (contraptionEntity.collidingEntities.containsKey(rootEntity)) {
				return contraptionEntity.getContactPointMotion(entity.position());
			}
		}
		return null;
	}
}

package fun.qu_an.minecraft.asyncparticles.client.compat.create;

import com.simibubi.create.content.contraptions.*;
import com.simibubi.create.foundation.collision.ContinuousOBBCollider;
import com.simibubi.create.foundation.collision.Matrix3d;
import com.simibubi.create.foundation.collision.OrientedBB;
import com.simibubi.create.foundation.utility.BlockHelper;
import fun.qu_an.minecraft.asyncparticles.client.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.mixin.create.InvokerContraptionCollider;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Predicate;

/**
 * See {@link ContraptionCollider}
 */
public class CreateUtils {
	public static final boolean[] trueAndFalse = {true, false};

	public static Collection<WeakReference<AbstractContraptionEntity>> contraptions(ClientLevel level) {
		return loadedContraptions(level).values();
	}

	/**
	 * 完全没搞懂，先能用，后面再优化吧
	 */
	public static boolean collideWithContraption(ClientLevel level,
												 Vec3 originalPosition,
												 Vec3 originalMotion,
												 AABB bounds,
												 AbstractContraptionEntity contraptionEntity) {
		return collideWithContraption(level, originalPosition, originalMotion, bounds, contraptionEntity, false);
	}

	/**
	 * 完全没搞懂，先能用，后面再优化吧
	 */
	public static boolean collideWithContraption(ClientLevel level,
												 Vec3 originalPosition,
												 Vec3 originalMotion,
												 AABB bounds,
												 AbstractContraptionEntity contraptionEntity,
												 boolean estimate) {
		if (!bounds.intersects(contraptionEntity.getBoundingBox())) {
			return false;
		}

		Vec3 motion = originalMotion;
		Contraption contraption = contraptionEntity.getContraption();
		Vec3 contraptionPosition = contraptionEntity.position();
		Vec3 contraptionMotion = contraptionPosition.subtract(contraptionEntity.getPrevPositionVec());
		// Init matrix
		AbstractContraptionEntity.ContraptionRotationState rotation = contraptionEntity.getRotationState();
		Matrix3d rotationMatrix = rotation.asMatrix();

		// Transform entity position and motion to local space
		float yawOffset = rotation.getYawOffset();
		Vec3 anchorVec = contraptionEntity.getAnchorVec();
		Vec3 position = getWorldToLocalTranslation(originalPosition, bounds, anchorVec, rotationMatrix, yawOffset);
		motion = motion.subtract(contraptionMotion);
		motion = rotationMatrix.transform(motion);

		// Prepare entity bounds
		AABB localBB = bounds.move(position)
			.inflate(1.0E-7D);

		OrientedBB obb = new OrientedBB(localBB);
		obb.setRotation(rotationMatrix);

		// Use simplified bbs when present
		Optional<List<AABB>> optionalAABBList = contraption.getSimplifiedEntityColliders();
		List<AABB> collidableBBs;
		if (optionalAABBList.isPresent()) {
			collidableBBs = optionalAABBList.get();
		} else if (estimate) {
			return true; // No simplified bbs, use full entity bounds, this is a fallback for better performance
		} else {
			collidableBBs = new ArrayList<>();
			List<VoxelShape> potentialHits =
				// TODO 这里完全不需要高精度形状，重写一个类似方法
				InvokerContraptionCollider.invoker_getPotentiallyCollidedShapes(
					level, contraption, localBB.expandTowards(motion));
			potentialHits.forEach(shape -> collidableBBs.addAll(shape.toAabbs()));
		}

		Vec3 obbCenter = obb.getCenter();
		for (AABB bb : collidableBBs) {
			if (Math.abs(obbCenter.x - bb.getCenter().x) - bounds.getXsize() - 1 > bb.getXsize() / 2)
				continue;
			if (Math.abs((obbCenter.y + motion.y) - bb.getCenter().y) - bounds.getYsize()
				- 1 > bb.getYsize() / 2)
				continue;
			if (Math.abs(obbCenter.z - bb.getCenter().z) - bounds.getZsize() - 1 > bb.getZsize() / 2)
				continue;

			obb.setCenter(obbCenter);
			ContinuousOBBCollider.ContinuousSeparationManifold intersect = obb.intersect(bb, motion);

			if (intersect == null)
				continue;

			double timeOfImpact = intersect.getTimeOfImpact();
			if (timeOfImpact > 0 && timeOfImpact < 1) {
				return true;
			}

			Vec3 separation = intersect.asSeparationVec(0);
			if (separation != null && !separation.equals(Vec3.ZERO)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return null if no collision
	 * TODO 用一个假实体直接调用原函数？
	 */
	@Nullable
	public static Vec3 collideMotionWithContraptions(ClientLevel level, Vec3 position, Vec3 motion, AABB bounds) {
		AABB bounds1 = bounds.inflate(0.1).move(motion);
		Vector3d result = new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
		forEachContraption(level, bounds1, contraptionEntity -> {
			Vec3 vec3 = collideMotionWithContraption(level, position, motion, bounds1, contraptionEntity);
			if (vec3 == null) {
				return true;
			}
			result.set(Math.min(result.x, vec3.x), Math.min(result.y, vec3.y), Math.min(result.z, vec3.z));
			return true;
		});
		if (result.x == Double.MAX_VALUE
			|| (motion.x == result.x && motion.y == result.y && motion.z == result.z)) {
			return null;
		}
		return new Vec3(result.x, result.y, result.z);
	}

	public static void forEachContraption(ClientLevel level, AABB bounds1, Predicate<AbstractContraptionEntity> consumer) {
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

	private static Vec3 getWorldToLocalTranslation(Vec3 entityPosition,
												   AABB bounds,
												   Vec3 anchorVec,
												   Matrix3d rotationMatrix,
												   float yawOffset) {
		Vec3 centerY = new Vec3(0, bounds.getYsize() / 2, 0);
		Vec3 position = entityPosition;
		position = position.add(centerY);
		position = ContraptionCollider.worldToLocalPos(position, anchorVec, rotationMatrix, yawOffset);
		position = position.subtract(centerY);
		position = position.subtract(entityPosition);
		return position;
	}

	/**
	 * 完全没搞懂，先能用，后面再优化吧
	 */
	@Nullable
	public static Vec3 collideMotionWithContraption(ClientLevel level,
													Vec3 originalPosition,
													Vec3 originalMotion,
													AABB bounds,
													AbstractContraptionEntity contraptionEntity) {
		return collideMotionWithContraption(level, originalPosition, originalMotion, bounds, contraptionEntity, false);
	}

	/**
	 * 完全没搞懂，先能用，后面再优化吧
	 */
	@Nullable
	public static Vec3 collideMotionWithContraption(ClientLevel level,
													Vec3 originalPosition,
													Vec3 originalMotion,
													AABB bounds,
													AbstractContraptionEntity contraptionEntity,
													boolean estimate) {
		if (!bounds.intersects(contraptionEntity.getBoundingBox())) {
			return null;
		}

		Vec3 motion = originalMotion;
		Contraption contraption = contraptionEntity.getContraption();
		Vec3 contraptionPosition = contraptionEntity.position();
		Vec3 contraptionMotion = contraptionPosition.subtract(contraptionEntity.getPrevPositionVec());
		// Init matrix
		AbstractContraptionEntity.ContraptionRotationState rotation = contraptionEntity.getRotationState();
		Matrix3d rotationMatrix = rotation.asMatrix();

		// Transform entity position and motion to local space
		float yawOffset = rotation.getYawOffset();
		Vec3 anchorVec = contraptionEntity.getAnchorVec();
		Vec3 position = getWorldToLocalTranslation(originalPosition, bounds, anchorVec, rotationMatrix, yawOffset);
		motion = motion.subtract(contraptionMotion);
		motion = rotationMatrix.transform(motion);

		// Prepare entity bounds
		AABB localBB = bounds.move(position)
			.inflate(1.0E-7D);

		OrientedBB obb = new OrientedBB(localBB);
		obb.setRotation(rotationMatrix);

		// Use simplified bbs when present
		Optional<List<AABB>> optionalAABBList = contraption.getSimplifiedEntityColliders();
		List<AABB> collidableBBs;
		if (optionalAABBList.isPresent()) {
			collidableBBs = optionalAABBList.get();
		} else if (estimate) {
			return Vec3.ZERO; // No simplified bbs, use full entity bounds, this is a fallback for better performance
		} else {
			collidableBBs = new ArrayList<>();
			List<VoxelShape> potentialHits =
				// TODO 这里完全不需要高精度形状，重写一个类似方法
				InvokerContraptionCollider.invoker_getPotentiallyCollidedShapes(
					level, contraption, localBB.expandTowards(motion));
			potentialHits.forEach(shape -> collidableBBs.addAll(shape.toAabbs()));
		}

		MutableObject<Vec3> collisionResponse = new MutableObject<>(Vec3.ZERO);
		MutableObject<Vec3> normal = new MutableObject<>(Vec3.ZERO);
		MutableObject<Vec3> location = new MutableObject<>(Vec3.ZERO);
		MutableFloat temporalResponse = new MutableFloat(1);
		Vec3 obbCenter = obb.getCenter();

		// Apply separation maths
		boolean doHorizontalPass = !rotation.hasVerticalRotation();
		for (boolean horizontalPass : trueAndFalse) {
			boolean verticalPass = !horizontalPass || !doHorizontalPass;

			for (AABB bb : collidableBBs) {
				Vec3 currentResponse = collisionResponse.getValue();
				Vec3 currentCenter = obbCenter.add(currentResponse);

				if (Math.abs(currentCenter.x - bb.getCenter().x) - bounds.getXsize() - 1 > bb.getXsize() / 2)
					continue;
				if (Math.abs((currentCenter.y + motion.y) - bb.getCenter().y) - bounds.getYsize()
					- 1 > bb.getYsize() / 2)
					continue;
				if (Math.abs(currentCenter.z - bb.getCenter().z) - bounds.getZsize() - 1 > bb.getZsize() / 2)
					continue;

				obb.setCenter(currentCenter);
				ContinuousOBBCollider.ContinuousSeparationManifold intersect = obb.intersect(bb, motion);

				if (intersect == null)
					continue;

				double timeOfImpact = intersect.getTimeOfImpact();
				boolean isTemporal = timeOfImpact > 0 && timeOfImpact < 1;
				Vec3 collidingNormal = intersect.getCollisionNormal();
				Vec3 collisionPosition = intersect.getCollisionPosition();

				if (!isTemporal) {
					Vec3 separation = intersect.asSeparationVec(0);
					if (separation != null && !separation.equals(Vec3.ZERO)) {
						collisionResponse.setValue(currentResponse.add(separation));
						timeOfImpact = 0;
					}
				}

				boolean nearest = timeOfImpact >= 0 && temporalResponse.getValue() > timeOfImpact;
				if (collidingNormal != null && nearest)
					normal.setValue(collidingNormal);
				if (collisionPosition != null && nearest)
					location.setValue(collisionPosition);

				if (isTemporal) {
					if (temporalResponse.getValue() > timeOfImpact)
						temporalResponse.setValue(timeOfImpact);
				}
			}

			if (verticalPass)
				break;

			boolean noVerticalMotionResponse = temporalResponse.getValue() == 1;
			boolean noVerticalCollision = collisionResponse.getValue().y == 0;
			if (noVerticalCollision && noVerticalMotionResponse)
				break;

			// Re-run collisions with horizontal offset
			collisionResponse.setValue(collisionResponse.getValue()
				.multiply(129 / 128f, 0, 129 / 128f));
		}

		// Resolve collision
		Vec3 entityMotion = originalMotion;
		Vec3 entityMotionNoTemporal = entityMotion;
		Vec3 collisionNormal = normal.getValue();
		Vec3 collisionLocation = location.getValue();
		Vec3 totalResponse = collisionResponse.getValue();
		boolean hardCollision = !totalResponse.equals(Vec3.ZERO);
		boolean temporalCollision = temporalResponse.getValue() != 1;
		Vec3 motionResponse = !temporalCollision ? motion
			: motion.normalize()
			.scale(motion.length() * temporalResponse.getValue());

		rotationMatrix.transpose();
		motionResponse = rotationMatrix.transform(motionResponse)
			.add(contraptionMotion);
		totalResponse = rotationMatrix.transform(totalResponse);
		totalResponse = rotate(totalResponse, yawOffset, Direction.Axis.Y);
		collisionNormal = rotationMatrix.transform(collisionNormal);
		collisionNormal = rotate(collisionNormal, yawOffset, Direction.Axis.Y);
		collisionNormal = collisionNormal.normalize();
		collisionLocation = rotationMatrix.transform(collisionLocation);
		collisionLocation = rotate(collisionLocation, yawOffset, Direction.Axis.Y);
		rotationMatrix.transpose();

		double bounce = 0;
		double slide = 0;

		if (!collisionLocation.equals(Vec3.ZERO)) {
			BlockPos pos = BlockPos.containing(contraptionEntity.toLocalVector(originalPosition, 0));
			if (contraption.getBlocks()
				.containsKey(pos)) {
				BlockState blockState = contraption.getBlocks()
					.get(pos).state();
				if (blockState.is(BlockTags.CLIMBABLE)) {
					totalResponse = totalResponse.add(0, .1f, 0);
				}
			}

			pos = BlockPos.containing(contraptionEntity.toLocalVector(collisionLocation, 0));
			if (contraption.getBlocks()
				.containsKey(pos)) {
				BlockState blockState = contraption.getBlocks()
					.get(pos).state();

				bounce = BlockHelper.getBounceMultiplier(blockState.getBlock());
				slide = Math.max(0, blockState.getBlock().getFriction()) - .6f;
			}
		}

		boolean hasNormal = !collisionNormal.equals(Vec3.ZERO);
		boolean anyCollision = hardCollision || temporalCollision;

		if (temporalCollision) {
			double idealVerticalMotion = motionResponse.y;
			if (idealVerticalMotion != entityMotion.y) {
				entityMotion = entityMotion.multiply(1, 0, 1)
					.add(0, idealVerticalMotion, 0);
			}
		}

		if (hardCollision) {
			double motionX = entityMotion.x();
			double motionY = entityMotion.y();
			double motionZ = entityMotion.z();
			double intersectX = totalResponse.x();
			double intersectY = totalResponse.y();
			double intersectZ = totalResponse.z();

			double horizonalEpsilon = 1 / 128f;
			if (motionX != 0 && Math.abs(intersectX) > horizonalEpsilon && motionX > 0 == intersectX < 0)
				entityMotion = entityMotion.multiply(0, 1, 1);
			if (motionY != 0 && intersectY != 0 && motionY > 0 == intersectY < 0)
				entityMotion = entityMotion.multiply(1, 0, 1)
					.add(0, contraptionMotion.y, 0);
			if (motionZ != 0 && Math.abs(intersectZ) > horizonalEpsilon && motionZ > 0 == intersectZ < 0)
				entityMotion = entityMotion.multiply(1, 1, 0);

		}

		if (bounce == 0 && slide > 0 && hasNormal && anyCollision && rotation.hasVerticalRotation()) {
			double slideFactor = collisionNormal.multiply(1, 0, 1)
									 .length() * 1.25f;
			Vec3 motionIn = entityMotionNoTemporal.multiply(0, .9, 0)
				.add(0, -.01f, 0);
			Vec3 slideNormal = collisionNormal.cross(motionIn.cross(collisionNormal))
				.normalize();
			entityMotion = entityMotion.multiply(.85, 0, .85)
				.add(slideNormal.scale((.2f + slide) * motionIn.length() * slideFactor)
					.add(0, -.1f - collisionNormal.y * .125f, 0));
		}

		return entityMotion;
	}

	public static Vec3 rotate(Vec3 collisionLocation, float yawOffset, Direction.Axis axis) {
		return ModListHelper.CREATE_MAJOR_VERSION < 6
			? Create5Utils.rotate(collisionLocation, yawOffset, axis)
			: Create6Utils.rotate(collisionLocation, yawOffset, axis);
	}

	public static Vec3 getCenterOf(BlockPos blockPos) {
		return ModListHelper.CREATE_MAJOR_VERSION < 6
			? Create5Utils.getCenterOf(blockPos)
			: Create6Utils.getCenterOf(blockPos);
	}

	public static Map<Integer, WeakReference<AbstractContraptionEntity>> loadedContraptions(ClientLevel level) {
		return ModListHelper.CREATE_MAJOR_VERSION < 6
			? Create5Utils.loadedContraptions(level)
			: Create6Utils.loadedContraptions(level);
	}

	public static boolean isUnderContraption(ClientLevel level, double x, double y, double z) {
		boolean[] b = {false};
		Vec3 pos = new Vec3(x, y, z);
		AABB bounds = new AABB(x - 1, y - 1, z - 1, x + 1, Math.max(y + 16, level.getMaxBuildHeight()), z + 1);
		forEachContraption(level, bounds, contraptionEntity -> {
			boolean b1 = collideWithContraption(level, pos, Vec3.ZERO, bounds, contraptionEntity, true);
			// estimate = true for a better performance
			if (!b1) {
				return true;
			}
			b[0] = true;
			return false;
		});
		return b[0];
	}
}

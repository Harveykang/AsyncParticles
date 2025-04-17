package fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.collision.ContinuousOBBCollider;
import com.simibubi.create.foundation.collision.Matrix3d;
import com.simibubi.create.foundation.collision.OrientedBB;
import com.simibubi.create.foundation.utility.BlockHelper;
import fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.create.InvokerContraptionCollider;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
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
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.lang.Math.max;

/**
 * See {@link ContraptionCollider}
 */
public class CreateCompatImpl {
	public static final boolean[] trueAndFalse = {true, false};

	public static Collection<WeakReference<AbstractContraptionEntity>> contraptions(ClientLevel level) {
		return loadedContraptions(level).values();
	}

	/**
	 * 完全没搞懂，先能用，后面再优化吧
	 */
	public static boolean collideWithContraption(ClientLevel level,
												 Vec3 originalMotion,
												 AABB bounds,
												 AbstractContraptionEntity contraptionEntity) {
		return collideWithContraption(level, originalMotion, bounds, contraptionEntity, false);
	}

	/**
	 * 完全没搞懂，先能用，后面再优化吧
	 */
	public static boolean collideWithContraption(ClientLevel level,
												 Vec3 originalMotion,
												 AABB bounds,
												 AbstractContraptionEntity contraptionEntity,
												 boolean estimate) {
		// bro why the train's contraption out of bounds?
		AABB bb0;
		AABB entityBoundingBox = contraptionEntity instanceof CarriageContraptionEntity
			? (bb0 = contraptionEntity.getBoundingBox()).inflate(0, (max(bb0.getXsize(), bb0.getZsize()) - bb0.getYsize()) * 0.3, 0)
			: contraptionEntity.getBoundingBox();
		if (!bounds.expandTowards(originalMotion).intersects(entityBoundingBox)) {
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
		Vec3 position = getWorldToLocalTranslation(bounds.getCenter(), anchorVec, rotationMatrix, yawOffset);
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
			Vec3 bbCenter = bb.getCenter();
			if ((Math.abs(obbCenter.x - bbCenter.x) - bounds.getXsize() - 1) * 2 > bb.getXsize())
				continue;
			if ((Math.abs((obbCenter.y + motion.y) - bbCenter.y) - bounds.getYsize() - 1) * 2 > bb.getYsize())
				continue;
			if ((Math.abs(obbCenter.z - bbCenter.z) - bounds.getZsize() - 1) * 2 > bb.getZsize())
				continue;

			obb.setCenter(obbCenter);
			ContinuousOBBCollider.ContinuousSeparationManifold intersect = obb.intersect(bb, motion);

			if (intersect != null) {
				return true;
			}
		}
		return false;
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
				result.set(Math.min(result.x, vec3.x), Math.min(result.y, vec3.y), Math.min(result.z, vec3.z));
			}
			return true;
		});
		if (result.x == Double.MAX_VALUE
			|| (motion.x == result.x && motion.y == result.y && motion.z == result.z)) {
			return null;
		}
		return new Vec3(result.x, result.y, result.z);
	}

	public static void forEachContraption(ClientLevel level, Predicate<AbstractContraptionEntity> consumer) {
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

	public static Iterator<AbstractContraptionEntity> forEachContraption(ClientLevel level) {
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
													AABB bounds,
													AbstractContraptionEntity contraptionEntity,
													boolean estimate) {
		// bro why the train's contraption out of bounds?
		AABB bb0;
		AABB entityBoundingBox = contraptionEntity instanceof CarriageContraptionEntity
			? (bb0 = contraptionEntity.getBoundingBox()).inflate(0, (max(bb0.getXsize(), bb0.getZsize()) - bb0.getYsize()) * 0.3, 0)
			: contraptionEntity.getBoundingBox();
		if (!bounds.expandTowards(originalMotion).intersects(entityBoundingBox)) {
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
		Vec3 position = getWorldToLocalTranslation(bounds.getCenter(), anchorVec, rotationMatrix, yawOffset);
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

				Vec3 bbCenter = bb.getCenter();
				if ((Math.abs(currentCenter.x - bbCenter.x) - bounds.getXsize() - 1) * 2 > bb.getXsize())
					continue;
				if ((Math.abs((currentCenter.y + motion.y) - bbCenter.y) - bounds.getYsize() - 1) * 2 > bb.getYsize())
					continue;
				if ((Math.abs(currentCenter.z - bbCenter.z) - bounds.getZsize() - 1) * 2 > bb.getZsize())
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
			BlockPos pos = BlockPos.containing(contraptionEntity.toLocalVector(bounds.getCenter(), 0));
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

		if (entityMotion.equals(originalMotion)) {
			return null;
		}
		return entityMotion;
	}

	public static Vec3 rotate(Vec3 collisionLocation, float yawOffset, Direction.Axis axis) {
		return VecHelper.rotate(collisionLocation, yawOffset, axis);
	}

	public static Vec3 getCenterOf(BlockPos blockPos) {
		if (blockPos.equals(Vec3i.ZERO))
			return VecHelper.CENTER_OF_ORIGIN;
		return Vec3.atLowerCornerWithOffset(blockPos, 0.5, 0.5, 0.5);
	}

	public static Map<Integer, WeakReference<AbstractContraptionEntity>> loadedContraptions(ClientLevel level) {
		return ContraptionHandler.loadedContraptions.get(level);
	}

	public static boolean isCollideWithContraption(ClientLevel level, Vec3 motion, AABB bb) {
		return isCollideWithContraption(level, motion, bb, true);
	}

	public static boolean isCollideWithContraption(ClientLevel level, Vec3 motion, AABB bb, boolean estimate) {
		for (Iterator<AbstractContraptionEntity> it = forEachContraption(level); it.hasNext(); ) {
			AbstractContraptionEntity contraptionEntity = it.next();
			boolean b1 = collideWithContraption(level, motion, bb, contraptionEntity, estimate);
			if (b1) {
				return true;
			}
		}
		return false;
	}
}

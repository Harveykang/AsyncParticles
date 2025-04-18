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
import fun.qu_an.minecraft.asyncparticles.client.util.CollisionResult;
import fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.create.InvokerContraptionCollider;
import net.createmod.catnip.data.Iterate;
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
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.lang.Math.abs;
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
												   AABB bounds,
												   AbstractContraptionEntity contraptionEntity,
												   boolean estimate) {
		// bro why the train's contraption out of bounds?
		AABB bb0;
		AABB entityBoundingBox = contraptionEntity instanceof CarriageContraptionEntity
			? (bb0 = contraptionEntity.getBoundingBox()).inflate(0, max(max(bb0.getXsize(), bb0.getZsize()) - bb0.getYsize() * 0.3, 0), 0)
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
		AABB localBBExpanded = localBB.expandTowards(motion);
		if (optionalAABBList.isPresent()) {
			collidableBBs = optionalAABBList.get();
		} else if (estimate) {
			return true; // No simplified bbs, use full entity bounds, this is a fallback for better performance
		} else {
			collidableBBs = new ArrayList<>();
			List<VoxelShape> potentialHits =
				// TODO 这里完全不需要高精度形状，重写一个类似方法
				InvokerContraptionCollider.invoker_getPotentiallyCollidedShapes(
					level, contraption, localBBExpanded);
			potentialHits.forEach(shape -> collidableBBs.addAll(shape.toAabbs()));
		}

		Vec3 localBBCenter = localBBExpanded.getCenter();
		double expandedXsize = localBBExpanded.getXsize();
		double expandedYsize = localBBExpanded.getYsize();
		double expandedZsize = localBBExpanded.getZsize();
		for (AABB bb : collidableBBs) {
			Vec3 bbCenter = bb.getCenter();
			if (abs(localBBCenter.x - bbCenter.x) > (bb.getXsize() + expandedXsize) * 0.5)
				continue;
			if (abs(localBBCenter.y - bbCenter.y) > (bb.getYsize() + expandedYsize) * 0.5)
				continue;
			if (abs(localBBCenter.z - bbCenter.z) > (bb.getZsize() + expandedZsize) * 0.5)
				continue;
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

	public static CollisionResult collideWithContraptions(ClientLevel level, Vec3 motion, AABB bounds) {
		Vector3d clipped = new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
		Vector3d contactPointMotion = new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
		AABB finalBounds = bounds.inflate(0.1);
		forEachContraption(level, contraptionEntity -> {
			Vec3 vec3 = collideMotionWithContraption(level, motion, finalBounds, contraptionEntity);
			if (vec3 != null) {
				clipped.set(Math.min(clipped.x, vec3.x), Math.min(clipped.y, vec3.y), Math.min(clipped.z, vec3.z));
				Vec3 vec31 = contraptionEntity.getContactPointMotion(finalBounds.getCenter().add(vec3));
				contactPointMotion.set(Math.min(contactPointMotion.x, vec31.x), Math.min(contactPointMotion.y, vec31.y), Math.min(contactPointMotion.z, vec31.z));
			}
			return true;
		});
		if (clipped.x == Double.MAX_VALUE
			|| (motion.x == clipped.x && motion.y == clipped.y && motion.z == clipped.z)) {
			return null;
		}
		return new CollisionResult(new Vec3(clipped.x, clipped.y, clipped.z), new Vec3(contactPointMotion.x, contactPointMotion.y, contactPointMotion.z));
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
													AABB entityBounds,
													AbstractContraptionEntity contraptionEntity,
													boolean estimate) {
		// Init matrix
		AbstractContraptionEntity.ContraptionRotationState rotation = contraptionEntity.getRotationState();
		Matrix3d rotationMatrix = rotation.asMatrix();

		// Transform entity position and motion to local space
		Vec3 center = entityBounds.getCenter();
		Vec3 entityPosition = entityBounds.getBottomCenter();
		Vec3 motion = originalMotion;
		float yawOffset = rotation.getYawOffset();
		Vec3 anchorVec = contraptionEntity.getAnchorVec();
		Vec3 toLocalTranslation = getWorldToLocalTranslation(center, anchorVec, rotationMatrix, yawOffset);

		Vec3 contraptionPosition = contraptionEntity.position();
		Vec3 contraptionMotion = contraptionPosition.subtract(contraptionEntity.getPrevPositionVec());
		motion = motion.subtract(contraptionMotion);
		motion = rotationMatrix.transform(motion);

		// Prepare entity bounds
		AABB localBB = entityBounds.move(toLocalTranslation)
			.inflate(1.0E-7D);

		OrientedBB obb = new OrientedBB(localBB);
		obb.setRotation(rotationMatrix);

		// Use simplified bbs when present
//		final Vec3 motionCopy = motion;
		Contraption contraption = contraptionEntity.getContraption();

		// Use simplified bbs if present
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
		MutableBoolean surfaceCollision = new MutableBoolean(false);
		MutableFloat temporalResponse = new MutableFloat(1);
		Vec3 obbCenter = obb.getCenter();

		// Apply separation maths
		boolean doHorizontalPass = !rotation.hasVerticalRotation();
		for (boolean horizontalPass : Iterate.trueAndFalse) {
			boolean verticalPass = !horizontalPass || !doHorizontalPass;

			for (AABB bb : collidableBBs) {
				Vec3 currentResponse = collisionResponse.getValue();
				Vec3 currentCenter = obbCenter.add(currentResponse);

				Vec3 bbCenter = bb.getCenter();
				if ((Math.abs(currentCenter.x - bbCenter.x) - entityBounds.getXsize() - 1) * 2 > bb.getXsize())
					continue;
				if ((Math.abs((currentCenter.y + motion.y) - bbCenter.y) - entityBounds.getYsize() - 1) * 2 > bb.getYsize())
					continue;
				if ((Math.abs(currentCenter.z - bbCenter.z) - entityBounds.getZsize() - 1) * 2 > bb.getZsize())
					continue;

				obb.setCenter(currentCenter);
				ContinuousOBBCollider.ContinuousSeparationManifold intersect = obb.intersect(bb, motion);

				if (intersect == null)
					continue;
				if (verticalPass && surfaceCollision.isFalse())
					surfaceCollision.setValue(intersect.isSurfaceCollision());

				double timeOfImpact = intersect.getTimeOfImpact();
				boolean isTemporal = timeOfImpact > 0 && timeOfImpact < 1;
				Vec3 collidingNormal = intersect.getCollisionNormal();
				Vec3 collisionPosition = intersect.getCollisionPosition();

				if (!isTemporal) {
//					Vec3 separation = intersect.asSeparationVec(10); // fixed stuck on walls
					Vec3 separation = intersect.asSeparationVec(0); // fixed stuck on walls
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
//		Vec3 entityMotionNoTemporal = entityMotion;
//		Vec3 collisionNormal = normal.getValue();
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
		totalResponse = VecHelper.rotate(totalResponse, yawOffset, Direction.Axis.Y);
//		collisionNormal = rotationMatrix.transform(collisionNormal);
//		collisionNormal = VecHelper.rotate(collisionNormal, yawOffset, Direction.Axis.Y);
//		collisionNormal = collisionNormal.normalize();
		collisionLocation = rotationMatrix.transform(collisionLocation);
		collisionLocation = VecHelper.rotate(collisionLocation, yawOffset, Direction.Axis.Y);
		rotationMatrix.transpose();

//		double bounce = 0;
//		double slide = 0;

		if (!collisionLocation.equals(Vec3.ZERO)) {
			collisionLocation = collisionLocation.add(entityPosition.add(center).scale(.5f));
//			if (temporalCollision)
//				collisionLocation = collisionLocation.add(0, motionResponse.y, 0);

			BlockPos pos = BlockPos.containing(contraptionEntity.toLocalVector(entityPosition, 0));
			if (contraption.getBlocks()
				.containsKey(pos)) {
				BlockState blockState = contraption.getBlocks()
					.get(pos).state();
				if (blockState.is(BlockTags.CLIMBABLE)) {
					surfaceCollision.setTrue();
					totalResponse = totalResponse.add(0, .1f, 0);
				}
			}

//			pos = BlockPos.containing(contraptionEntity.toLocalVector(collisionLocation, 0));
//			if (contraption.getBlocks()
//				.containsKey(pos)) {
//				BlockState blockState = contraption.getBlocks()
//					.get(pos).state();
//
//				bounce = BlockHelper.getBounceMultiplier(blockState.getBlock());
//				slide = Math.max(0, blockState.getFriction(level, pos, null) - .6f);
//			}
		}

//		boolean hasNormal = !collisionNormal.equals(Vec3.ZERO);
//		boolean anyCollision = hardCollision || temporalCollision;

//		if (bounce > 0 && hasNormal && anyCollision) {
//			Vec3 bounced = bounceEntity(originalMotion, contraptionEntity.getContactPointMotion(entityPosition), collisionNormal, bounce);
//			if (bounced != null) {
//				return originalMotion.equals(bounced) ? null : bounced;
//			}
//		}

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
				entityMotion = entityMotion.multiply(0, 1, 1)
					.add(contraptionMotion.x, 0, 0);
			if (motionY != 0 && intersectY != 0 && motionY > 0 == intersectY < 0)
				entityMotion = entityMotion.multiply(1, 0, 1)
					.add(0, contraptionMotion.y, 0);
			if (motionZ != 0 && Math.abs(intersectZ) > horizonalEpsilon && motionZ > 0 == intersectZ < 0)
				entityMotion = entityMotion.multiply(1, 1, 0)
					.add(0, 0, contraptionMotion.z);

		}

//		if (bounce == 0 && slide > 0 && hasNormal && anyCollision && rotation.hasVerticalRotation()) {
//			double slideFactor = collisionNormal.multiply(1, 0, 1)
//									 .length() * 1.25f;
//			Vec3 motionIn = entityMotionNoTemporal.multiply(0, .9, 0)
//				.add(0, -.01f, 0);
//			Vec3 slideNormal = collisionNormal.cross(motionIn.cross(collisionNormal))
//				.normalize();
//			entityMotion = entityMotion.multiply(.85, 0, .85)
//				.add(slideNormal.scale((.2f + slide) * motionIn.length() * slideFactor)
//					.add(0, -.1f - collisionNormal.y * .125f, 0));
//		}

		if (!hardCollision && surfaceCollision.isFalse())
			return originalMotion.equals(entityMotion) ? null : entityMotion;

		if (surfaceCollision.isTrue()) {
			entityMotion = entityMotion.add(contraptionEntity.getContactPointMotion(entityPosition));
		}

		return originalMotion.equals(entityMotion) ? null : entityMotion;
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
			boolean b1 = isCollideWithContraption(level, motion, bb, contraptionEntity, estimate);
			if (b1) {
				return true;
			}
		}
		return false;
	}
}

package fun.qu_an.minecraft.asyncparticles.client.compat.create;

import com.simibubi.create.content.contraptions.*;
import com.simibubi.create.foundation.collision.ContinuousOBBCollider;
import com.simibubi.create.foundation.collision.Matrix3d;
import com.simibubi.create.foundation.collision.OrientedBB;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.VecHelper;
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

import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * See {@link ContraptionCollider}
 */
public class CreateUtils {
	public static Stream<AbstractContraptionEntity> contraptions(ClientLevel level) {
		return ContraptionHandler.loadedContraptions.get(level)
			.values()
			.stream()
			.map(Reference::get);
	}

	@Nullable
	public static Vec3 collideWithContraptions(ClientLevel level, Vec3 position, Vec3 motion, AABB bounds) {
		AABB bounds1 = bounds.inflate(0.1);
		Vector3d collect = contraptions(level)
			.filter(c -> c.getContraption().bounds.intersects(bounds1))
			.map(c -> {
				Vec3 vec3 = collideWithContraption(level, position, motion, bounds1, c);
				return new Vector3d(vec3.x, vec3.y, vec3.z);
			})
			.reduce(new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE), Vector3d::min);
		if (collect.x == Double.MAX_VALUE) {
			return null;
		}
		return new Vec3(collect.x, collect.y, collect.z);
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

	private static Vec3 collideWithContraption(ClientLevel level,
												   Vec3 originalPosition,
												   Vec3 originalMotion,
												   AABB bounds,
												   AbstractContraptionEntity contraptionEntity) {
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
		final Vec3 motionCopy = motion;
		List<AABB> collidableBBs = contraption.getSimplifiedEntityColliders()
			.orElseGet(() -> {
				// Else find 'nearby' individual block shapes to collide with
				List<AABB> bbs = new ArrayList<>();
				List<VoxelShape> potentialHits =
					InvokerContraptionCollider.invoker_getPotentiallyCollidedShapes(
						level, contraption, localBB.expandTowards(motionCopy));
				potentialHits.forEach(shape -> bbs.addAll(shape.toAabbs()));
				return bbs;
			});

		MutableObject<Vec3> collisionResponse = new MutableObject<>(Vec3.ZERO);
		MutableObject<Vec3> normal = new MutableObject<>(Vec3.ZERO);
		MutableObject<Vec3> location = new MutableObject<>(Vec3.ZERO);
		MutableFloat temporalResponse = new MutableFloat(1);
		Vec3 obbCenter = obb.getCenter();

		// Apply separation maths
		boolean doHorizontalPass = !rotation.hasVerticalRotation();
		for (boolean horizontalPass : Iterate.trueAndFalse) {
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
		totalResponse = VecHelper.rotate(totalResponse, yawOffset, Direction.Axis.Y);
		collisionNormal = rotationMatrix.transform(collisionNormal);
		collisionNormal = VecHelper.rotate(collisionNormal, yawOffset, Direction.Axis.Y);
		collisionNormal = collisionNormal.normalize();
		collisionLocation = rotationMatrix.transform(collisionLocation);
		collisionLocation = VecHelper.rotate(collisionLocation, yawOffset, Direction.Axis.Y);
		rotationMatrix.transpose();

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
		}

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

		boolean hasNormal = !collisionNormal.equals(Vec3.ZERO);
		boolean anyCollision = hardCollision || temporalCollision;
		if (hasNormal && anyCollision && rotation.hasVerticalRotation()) {
			double slideFactor = collisionNormal.multiply(1, 0, 1)
									 .length() * 1.25f;
			Vec3 motionIn = entityMotionNoTemporal.multiply(0, .9, 0)
				.add(0, -.01f, 0);
			Vec3 slideNormal = collisionNormal.cross(motionIn.cross(collisionNormal))
				.normalize();
			entityMotion = entityMotion.multiply(.85, 0, .85)
				.add(slideNormal.scale(.2f * motionIn.length() * slideFactor)
					.add(0, -.1f - collisionNormal.y * .125f, 0));
		}

		return entityMotion;
	}
}

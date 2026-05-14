package fun.qu_an.minecraft.asyncparticles.client.compat.sable.neoforge;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.collision.Matrix3d;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge.CreateUtilImpl;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;

public class SableCompat {
	public static Vec3 getContactPointMotion(Vec3 original,
	                                         AbstractContraptionEntity contraptionEntity,
	                                         Vec3 contactPoint) {
		SubLevel subLevel = Sable.HELPER.getContaining(contraptionEntity);
		if (subLevel == null) {
			return original;
		}
		Pose3d pose = subLevel.logicalPose();
		Vec3 localContactPoint = pose.transformPositionInverse(contactPoint); // world to sublevel
		return pose.transformNormal(contraptionEntity.getContactPointMotion(localContactPoint))
			.add(contactPoint.subtract(subLevel.lastPose().transformPosition(localContactPoint))); // sublevel to world
	}

	public static AABB getBoundingBox(AABB original,
	                                  AbstractContraptionEntity contraptionEntity) {
		SubLevel subLevel = Sable.HELPER.getContaining(contraptionEntity);
		if (subLevel == null) {
			return original;
		}
		BoundingBox3d worldBB = new BoundingBox3d(original);
		worldBB.transform(subLevel.logicalPose(), worldBB); // sublevel to world
		return worldBB.toMojang();
	}

	public static AABB expandTowards(AABB original,
	                                 AbstractContraptionEntity contraptionEntity) {
		SubLevel subLevel = Sable.HELPER.getContaining(contraptionEntity);
		if (subLevel == null) {
			return original;
		}
		BoundingBox3d globalBB = new BoundingBox3d(original.inflate(2.0));
		globalBB.transform(subLevel.logicalPose(), globalBB); // sublevel to world
		return globalBB.toMojang();
	}

	public static Vec3 getAnchorVec(Vec3 original,
	                                AbstractContraptionEntity contraptionEntity) {
		SubLevel subLevel = Sable.HELPER.getContaining(contraptionEntity);
		if (subLevel == null) {
			return original;
		}
		// sublevel to world
		return subLevel.logicalPose().transformPosition(original.add(0.5, 0.5, 0.5)).subtract(0.5, 0.5, 0.5);
	}

	public static Matrix3d asMatrix(Matrix3d original,
	                                AbstractContraptionEntity contraptionEntity) {
		SubLevel subLevel = Sable.HELPER.getContaining(contraptionEntity);
		if (subLevel == null) {
			return original;
		}
		Pose3d pose = subLevel.logicalPose();
		org.joml.Matrix3d jomlMatrix = CreateUtilImpl.toJOML(original);
		jomlMatrix.rotateLocal(pose.orientation()); // sublevel to world
		return CreateUtilImpl.toCreate(jomlMatrix);
	}
}

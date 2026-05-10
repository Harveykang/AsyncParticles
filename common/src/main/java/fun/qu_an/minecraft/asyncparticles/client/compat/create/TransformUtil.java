//package fun.qu_an.minecraft.asyncparticles.client.compat.create;
//
//import com.simibubi.create.foundation.collision.Matrix3d;
//import net.minecraft.core.Direction;
//import net.minecraft.world.phys.Vec3;
//
//import java.util.Map;
//
//public class TransformUtil {
//	public static Vec3 worldToLocalPos(Vec3 worldPos, Vec3 anchorVec, Matrix3d rotationMatrix, float yawOffset) {
//		Vec3 localPos = worldPos.subtract(anchorVec);
//		localPos = localPos.subtract(0.5, 0.5, 0.5);
//		localPos = CreateUtil.vecRotate(localPos, -yawOffset, Direction.Axis.Y);
//		localPos = rotationMatrix.transform(localPos);
//		localPos = localPos.add(0.5, 0.5, 0.5);
//		return localPos;
//	}
//
//	public static Vec3 localToWorldPos(Vec3 localPos, Vec3 anchorVec, Matrix3d rotationMatrix, float yawOffset) {
//		Vec3 worldPos = localPos.subtract(0.5, 0.5, 0.5);
//		rotationMatrix.transpose();
//		worldPos = rotationMatrix.transform(worldPos);
//		rotationMatrix.transpose();
//		worldPos = CreateUtil.vecRotate(worldPos, yawOffset, Direction.Axis.Y);
//		worldPos = worldPos.add(0.5, 0.5, 0.5);
//		worldPos = worldPos.add(anchorVec);
//		return worldPos;
//	}
//
//	public static Vec3 worldToLocalRotation(Vec3 worldRotation, Matrix3d rotationMatrix, float yawOffset) {
//		Vec3 localRotation = CreateUtil.vecRotate(worldRotation, -yawOffset, Direction.Axis.Y);
//		localRotation = rotationMatrix.transform(localRotation);
//		return localRotation;
//	}
//
//	public static Vec3 localToWorldRotation(Vec3 localRotation, Matrix3d rotationMatrix, float yawOffset) {
//		rotationMatrix.transpose();
//		Vec3 worldRotation = rotationMatrix.transform(localRotation);
//		rotationMatrix.transpose();
//		worldRotation = CreateUtil.vecRotate(worldRotation, yawOffset, Direction.Axis.Y);
//		return worldRotation;
//	}
//
//	public static Map.Entry<Vec3, Vec3> worldToLocalTransform(
//		Vec3 worldPos,
//		Vec3 worldRotation,
//		Vec3 anchorVec,
//		Matrix3d rotationMatrix,
//		float yawOffset
//	) {
//		return Map.entry(worldToLocalPos(worldPos, anchorVec, rotationMatrix, yawOffset),
//			worldToLocalRotation(worldRotation, rotationMatrix, yawOffset));
//	}
//
//	public static Map.Entry<Vec3, Vec3> localToWorldTransform(
//		Vec3 localPos,
//		Vec3 localRotation,
//		Vec3 anchorVec,
//		Matrix3d rotationMatrix,
//		float yawOffset
//	) {
//		return Map.entry(
//			localToWorldPos(localPos, anchorVec, rotationMatrix, yawOffset),
//			localToWorldRotation(localRotation, rotationMatrix, yawOffset)
//		);
//	}
//
//	public static Vec3 worldToLocalDisplacement(Vec3 worldPos,
//	                                            Vec3 anchorVec,
//	                                            Matrix3d rotationMatrix,
//	                                            float yawOffset) {
//		Vec3 localPos = worldToLocalPos(worldPos, anchorVec, rotationMatrix, yawOffset);
//		return localPos.subtract(worldPos);
//	}
//
//	public static Vec3 localToWorldDisplacement(Vec3 localPos,
//	                                           Vec3 anchorVec,
//	                                           Matrix3d rotationMatrix,
//	                                           float yawOffset) {
//		Vec3 worldPos = localToWorldPos(localPos, anchorVec, rotationMatrix, yawOffset);
//		return worldPos.subtract(localPos);
//	}
//}

package fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge;

import com.simibubi.create.foundation.collision.Matrix3d;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class CoordinateTransform {

    public record TransformData(Vec3 position, Vec3 rotation) {}

    private final Vec3 anchorVec;
    private final Matrix3d rotationMatrix;
    private final float yawOffset;

    public CoordinateTransform(Vec3 anchorVec, Matrix3d rotationMatrix, float yawOffset) {
        this.anchorVec = anchorVec;
        this.rotationMatrix = rotationMatrix;
        this.yawOffset = yawOffset;
    }

    public Vec3 worldToLocalPos(Vec3 worldPos) {
        Vec3 localPos = worldPos.subtract(anchorVec);
        localPos = localPos.subtract(VecHelper.CENTER_OF_ORIGIN);
        localPos = VecHelper.rotate(localPos, (double) (-yawOffset), Direction.Axis.Y);
        localPos = rotationMatrix.transform(localPos);
        localPos = localPos.add(VecHelper.CENTER_OF_ORIGIN);
        return localPos;
    }

    public Vec3 localToWorldPos(Vec3 localPos) {
        Vec3 worldPos = localPos.subtract(VecHelper.CENTER_OF_ORIGIN);
        worldPos = rotationMatrix.transformTransposed(worldPos);
        worldPos = VecHelper.rotate(worldPos, (double) yawOffset, Direction.Axis.Y);
        worldPos = worldPos.add(VecHelper.CENTER_OF_ORIGIN);
        worldPos = worldPos.add(anchorVec);
        return worldPos;
    }

    /**
     * rotation 是方向矢量，不是欧拉角
     */
    public Vec3 worldToLocalRotation(Vec3 worldRotation) {
        Vec3 localRotation = VecHelper.rotate(worldRotation, (double) (-yawOffset), Direction.Axis.Y);
        localRotation = rotationMatrix.transform(localRotation);
        return localRotation;
    }

    /**
     * rotation 是方向矢量，不是欧拉角
     */
    public Vec3 localToWorldRotation(Vec3 localRotation) {
        Vec3 worldRotation = rotationMatrix.transformTransposed(localRotation);
        worldRotation = VecHelper.rotate(worldRotation, (double) yawOffset, Direction.Axis.Y);
        return worldRotation;
    }

    public TransformData worldToLocalTransform(Vec3 worldPos, Vec3 worldRotation) {
        return new TransformData(
                worldToLocalPos(worldPos),
                worldToLocalRotation(worldRotation)
        );
    }

    public TransformData localToWorldTransform(Vec3 localPos, Vec3 localRotation) {
        return new TransformData(
                localToWorldPos(localPos),
                localToWorldRotation(localRotation)
        );
    }

    public Vec3 getAnchorVec() {
        return anchorVec;
    }

    public Matrix3d getRotationMatrix() {
        return rotationMatrix;
    }

    public float getYawOffset() {
        return yawOffset;
    }
}
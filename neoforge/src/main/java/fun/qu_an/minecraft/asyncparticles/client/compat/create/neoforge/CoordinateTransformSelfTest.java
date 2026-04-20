package fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge;

import com.simibubi.create.foundation.collision.Matrix3d;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.world.phys.Vec3;

public class CoordinateTransformSelfTest {
    public static void main(String[] args) {
        CoordinateTransformSelfTest.runAll();
    }

    public static void runAll() {
        testIdentityLikeCase();
        testRoundTrip();
        testDirectionLengthPreserved();
        testBasisDirections();
        System.out.println("CoordinateTransform self-test passed.");
    }

    private static void testIdentityLikeCase() {
        Vec3 anchor = new Vec3(10.0, 64.0, -3.0);
        Matrix3d rotationMatrix = new Matrix3d().asIdentity();
        float yawOffset = 0.0f;

        CoordinateTransform transform = new CoordinateTransform(anchor, rotationMatrix, yawOffset);

        Vec3 worldPos = new Vec3(11.2, 65.5, -1.8);
        Vec3 localPos = transform.worldToLocalPos(worldPos);
        Vec3 restoredWorldPos = transform.localToWorldPos(localPos);

        TransformTestUtil.assertVecNear("identity-like position", worldPos, restoredWorldPos, TransformTestUtil.EPS);

        Vec3 worldRot = new Vec3(0.0, 0.0, 1.0).normalize();
        Vec3 localRot = transform.worldToLocalRotation(worldRot);
        Vec3 restoredWorldRot = transform.localToWorldRotation(localRot);

        TransformTestUtil.assertVecNear("identity-like rotation", worldRot, restoredWorldRot, TransformTestUtil.EPS);
    }

    private static void testRoundTrip() {
        Vec3 anchor = new Vec3(5.25, 70.0, -12.75);
        Matrix3d rotationMatrix = createExampleRotationMatrix();
        float yawOffset = 37.5f;

        CoordinateTransform transform = new CoordinateTransform(anchor, rotationMatrix, yawOffset);

        Vec3[] positions = new Vec3[] {
                new Vec3(5.5, 70.5, -12.5),
                new Vec3(8.2, 64.3, -9.7),
                new Vec3(-3.1, 120.0, 44.8),
                new Vec3(0.0, 0.0, 0.0)
        };

        Vec3[] directions = new Vec3[] {
                new Vec3(1, 0, 0).normalize(),
                new Vec3(0, 1, 0).normalize(),
                new Vec3(0, 0, 1).normalize(),
                new Vec3(1, 2, 3).normalize(),
                new Vec3(-4, 1, 0.5).normalize()
        };

        for (int i = 0; i < positions.length; i++) {
            Vec3 worldPos = positions[i];
            Vec3 localPos = transform.worldToLocalPos(worldPos);
            Vec3 restoredWorldPos = transform.localToWorldPos(localPos);

            TransformTestUtil.assertVecNear(
                    "round-trip position[" + i + "]",
                    worldPos,
                    restoredWorldPos,
                    TransformTestUtil.EPS
            );
        }

        for (int i = 0; i < directions.length; i++) {
            Vec3 worldDir = directions[i];
            Vec3 localDir = transform.worldToLocalRotation(worldDir);
            Vec3 restoredWorldDir = transform.localToWorldRotation(localDir);

            TransformTestUtil.assertVecNear(
                    "round-trip direction[" + i + "]",
                    worldDir,
                    restoredWorldDir,
                    TransformTestUtil.EPS
            );
        }

        for (int i = 0; i < Math.min(positions.length, directions.length); i++) {
            CoordinateTransform.TransformData local = transform.worldToLocalTransform(positions[i], directions[i]);
            CoordinateTransform.TransformData world = transform.localToWorldTransform(local.position(), local.rotation());

            TransformTestUtil.assertVecNear(
                    "round-trip transform.position[" + i + "]",
                    positions[i],
                    world.position(),
                    TransformTestUtil.EPS
            );
            TransformTestUtil.assertVecNear(
                    "round-trip transform.rotation[" + i + "]",
                    directions[i],
                    world.rotation(),
                    TransformTestUtil.EPS
            );
        }
    }

    private static void testDirectionLengthPreserved() {
        Vec3 anchor = new Vec3(0, 0, 0);
        Matrix3d rotationMatrix = createExampleRotationMatrix();
        float yawOffset = -63.0f;

        CoordinateTransform transform = new CoordinateTransform(anchor, rotationMatrix, yawOffset);

        Vec3 worldDir = new Vec3(3.0, -4.0, 12.0);
        double len = worldDir.length();

        Vec3 localDir = transform.worldToLocalRotation(worldDir);
        Vec3 restoredWorldDir = transform.localToWorldRotation(localDir);

        TransformTestUtil.assertLengthNear("direction local length", localDir, len, TransformTestUtil.EPS);
        TransformTestUtil.assertLengthNear("direction restored length", restoredWorldDir, len, TransformTestUtil.EPS);
        TransformTestUtil.assertVecNear("direction restored", worldDir, restoredWorldDir, TransformTestUtil.EPS);
    }

    private static void testBasisDirections() {
        Vec3 anchor = new Vec3(0, 0, 0);
        Matrix3d rotationMatrix = createExampleRotationMatrix();
        float yawOffset = 90.0f;

        CoordinateTransform transform = new CoordinateTransform(anchor, rotationMatrix, yawOffset);

        Vec3[] basis = new Vec3[] {
                new Vec3(1, 0, 0),
                new Vec3(0, 1, 0),
                new Vec3(0, 0, 1)
        };

        for (int i = 0; i < basis.length; i++) {
            Vec3 local = transform.worldToLocalRotation(basis[i]);
            Vec3 restored = transform.localToWorldRotation(local);

            TransformTestUtil.assertVecNear("basis[" + i + "]", basis[i], restored, TransformTestUtil.EPS);
        }
    }

    /**
     * 这里只是示例。你项目里如果本来就有 rotationMatrix 的构造方式，直接换掉即可。
     * 必须保证这是“纯旋转正交矩阵”，这样 transformTransposed 才能作为逆变换。
     */
    private static Matrix3d createExampleRotationMatrix() {
        Matrix3d m = new Matrix3d().asIdentity();

        // 这里请替换成你项目里真正可用的旋转矩阵构造逻辑
        // 例如：绕 X / Y / Z 轴的若干组合
        // 下面仅作占位示意
        m = m.multiply(new Matrix3d().asXRotation(AngleHelper.rad(30)));
        m = m.multiply(new Matrix3d().asZRotation(AngleHelper.rad(-20)));

        return m;
    }
}

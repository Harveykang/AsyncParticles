package fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge;

import net.minecraft.world.phys.Vec3;

public class TransformTestUtil {

    public static final double EPS = 1e-2;

    public static boolean nearlyEqual(double a, double b, double eps) {
        return Math.abs(a - b) <= eps;
    }

    public static boolean nearlyEqual(Vec3 a, Vec3 b, double eps) {
        return nearlyEqual(a.x, b.x, eps)
                && nearlyEqual(a.y, b.y, eps)
                && nearlyEqual(a.z, b.z, eps);
    }

    public static void assertVecNear(String name, Vec3 expected, Vec3 actual, double eps) {
        if (!nearlyEqual(expected, actual, eps)) {
            throw new AssertionError(
                    name + " mismatch\nexpected: " + expected + "\nactual:   " + actual
            );
        }
    }

    public static void assertLengthNear(String name, Vec3 vec, double expectedLength, double eps) {
        double len = vec.length();
        if (!nearlyEqual(len, expectedLength, eps)) {
            throw new AssertionError(
                    name + " length mismatch\nexpected: " + expectedLength + "\nactual:   " + len
            );
        }
    }
}
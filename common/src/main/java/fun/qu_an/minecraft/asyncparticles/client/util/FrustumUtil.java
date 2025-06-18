package fun.qu_an.minecraft.asyncparticles.client.util;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.joml.FrustumIntersection;

public class FrustumUtil {
	public static boolean isVisible(Frustum frustum, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		FrustumIntersection intersection = frustum.intersection;
		double camX = frustum.camX;
		double camY = frustum.camY;
		double camZ = frustum.camZ;
		return intersection.testAab(
			(float) (minX - camX),
			(float) (minY - camY),
			(float) (minZ - camZ),
			(float) (maxX - camX),
			(float) (maxY - camY),
			(float) (maxZ - camZ));
	}

	public static boolean isVisible(Frustum frustum, @NotNull AABB renderBoundingBox) {
		return isVisible(frustum,
			renderBoundingBox.minX,
			renderBoundingBox.minY,
			renderBoundingBox.minZ,
			renderBoundingBox.maxX,
			renderBoundingBox.maxY,
			renderBoundingBox.maxZ);
	}
}

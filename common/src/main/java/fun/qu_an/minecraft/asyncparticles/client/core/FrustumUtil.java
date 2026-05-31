package fun.qu_an.minecraft.asyncparticles.client.core;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.joml.FrustumIntersection;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.FloatBuffer;

import static java.lang.Math.max;

public class FrustumUtil {
	private static final VarHandle nxX, nxY, nxZ, nxW;
	private static final VarHandle pxX, pxY, pxZ, pxW;
	private static final VarHandle nyX, nyY, nyZ, nyW;
	private static final VarHandle pyX, pyY, pyZ, pyW;
	private static final VarHandle nzX, nzY, nzZ, nzW;
	private static final VarHandle pzX, pzY, pzZ, pzW;

	static {
		try {
			MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(FrustumIntersection.class, MethodHandles.lookup());
			nxX = lookup.findVarHandle(FrustumIntersection.class, "nxX", Float.TYPE);
			nxY = lookup.findVarHandle(FrustumIntersection.class, "nxY", Float.TYPE);
			nxZ = lookup.findVarHandle(FrustumIntersection.class, "nxZ", Float.TYPE);
			nxW = lookup.findVarHandle(FrustumIntersection.class, "nxW", Float.TYPE);
			pxX = lookup.findVarHandle(FrustumIntersection.class, "pxX", Float.TYPE);
			pxY = lookup.findVarHandle(FrustumIntersection.class, "pxY", Float.TYPE);
			pxZ = lookup.findVarHandle(FrustumIntersection.class, "pxZ", Float.TYPE);
			pxW = lookup.findVarHandle(FrustumIntersection.class, "pxW", Float.TYPE);
			nyX = lookup.findVarHandle(FrustumIntersection.class, "nyX", Float.TYPE);
			nyY = lookup.findVarHandle(FrustumIntersection.class, "nyY", Float.TYPE);
			nyZ = lookup.findVarHandle(FrustumIntersection.class, "nyZ", Float.TYPE);
			nyW = lookup.findVarHandle(FrustumIntersection.class, "nyW", Float.TYPE);
			pyX = lookup.findVarHandle(FrustumIntersection.class, "pyX", Float.TYPE);
			pyY = lookup.findVarHandle(FrustumIntersection.class, "pyY", Float.TYPE);
			pyZ = lookup.findVarHandle(FrustumIntersection.class, "pyZ", Float.TYPE);
			pyW = lookup.findVarHandle(FrustumIntersection.class, "pyW", Float.TYPE);
			nzX = lookup.findVarHandle(FrustumIntersection.class, "nzX", Float.TYPE);
			nzY = lookup.findVarHandle(FrustumIntersection.class, "nzY", Float.TYPE);
			nzZ = lookup.findVarHandle(FrustumIntersection.class, "nzZ", Float.TYPE);
			nzW = lookup.findVarHandle(FrustumIntersection.class, "nzW", Float.TYPE);
			pzX = lookup.findVarHandle(FrustumIntersection.class, "pzX", Float.TYPE);
			pzY = lookup.findVarHandle(FrustumIntersection.class, "pzY", Float.TYPE);
			pzZ = lookup.findVarHandle(FrustumIntersection.class, "pzZ", Float.TYPE);
			pzW = lookup.findVarHandle(FrustumIntersection.class, "pzW", Float.TYPE);
		} catch (Throwable t) {
			throw new ExceptionInInitializerError(t);
		}
	}

	public static void getFrustumPlanes(FrustumIntersection intersection, FloatBuffer buffer) {
		buffer.put((float) nxX.get(intersection))
			.put((float) nxY.get(intersection))
			.put((float) nxZ.get(intersection))
			.put((float) nxW.get(intersection))
			.put((float) pxX.get(intersection))
			.put((float) pxY.get(intersection))
			.put((float) pxZ.get(intersection))
			.put((float) pxW.get(intersection))
			.put((float) nyX.get(intersection))
			.put((float) nyY.get(intersection))
			.put((float) nyZ.get(intersection))
			.put((float) nyW.get(intersection))
			.put((float) pyX.get(intersection))
			.put((float) pyY.get(intersection))
			.put((float) pyZ.get(intersection))
			.put((float) pyW.get(intersection))
			.put((float) nzX.get(intersection))
			.put((float) nzY.get(intersection))
			.put((float) nzZ.get(intersection))
			.put((float) nzW.get(intersection))
			.put((float) pzX.get(intersection))
			.put((float) pzY.get(intersection))
			.put((float) pzZ.get(intersection))
			.put((float) pzW.get(intersection));
	}

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

	public static boolean isColumnVisible(Frustum frustum, int x, int z, int bY, int tY) {
		FrustumIntersection intersection = frustum.intersection;
		double camX = frustum.camX;
		double camY = frustum.camY;
		double camZ = frustum.camZ;
		float minX = (float) (x - camX);
		float minZ = (float) (z - camZ);
		return intersection.testAab(
			minX,
			(float) (bY - camY),
			minZ,
			minX + 1f,
			(float) (tY - camY),
			minZ + 1f);
	}

	public static boolean isVisible(Frustum frustum, Particle particle) {
		return frustum.intersection.testSphere(
			(float) (particle.x - frustum.camX),
			(float) (particle.y - frustum.camY),
			(float) (particle.z - frustum.camZ),
			max(particle.bbWidth, particle.bbHeight) * 0.866f);
	}
}

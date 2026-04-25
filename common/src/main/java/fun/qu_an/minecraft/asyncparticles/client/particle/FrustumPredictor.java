package fun.qu_an.minecraft.asyncparticles.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class FrustumPredictor {
	private static Vec3 prevCamPos = Vec3.ZERO;
	private static float prevYaw = 0f;
	private static float prevPitch = 0f;

	public static Frustum createExpandedFrustum(Matrix4f originalProj, Matrix4f viewMatrix, Camera camera) {
		Vec3 currCamPos = camera.getPosition();
		float currYaw = camera.getYRot();
		float currPitch = camera.getXRot();

		// 修复2：防止第一帧或传送导致的速度爆炸
		if (prevCamPos.equals(Vec3.ZERO) || currCamPos.distanceToSqr(prevCamPos) > 10000) {
			prevCamPos = currCamPos;
			prevYaw = currYaw;
			prevPitch = currPitch;
			// 修复1：参数顺序必须是 (View, Projection)
			Frustum f = new Frustum(viewMatrix, originalProj);
			f.prepare(currCamPos.x, currCamPos.y, currCamPos.z);
			return f;
		}

		float deltaYaw = Math.abs(currYaw - prevYaw);
		if (deltaYaw > 180) deltaYaw = 360 - deltaYaw;
		float deltaPitch = Math.abs(currPitch - prevPitch);
		float maxRot = Math.max(deltaYaw, deltaPitch);

		double moveSpeed = currCamPos.distanceTo(prevCamPos);

		// 膨胀系数算法
		float expandFactor = 1.0f + (maxRot * 0.015f) + ((float) moveSpeed * 0.05f);
		// 强制把系数限制在一个合理范围内 (1.0 = 原版大小, 1.3 = 扩大30%防闪烁)
		expandFactor = Math.clamp(expandFactor, 1.0f, 1.3f);

		// 修改投影矩阵
		Matrix4f expandedProj = new Matrix4f(originalProj);
		expandedProj.m00(expandedProj.m00() / expandFactor);
		expandedProj.m11(expandedProj.m11() / expandFactor);

		// 更新历史记录
		prevCamPos = currCamPos;
		prevYaw = currYaw;
		prevPitch = currPitch;

		// 修复1：参数顺序必须是 (View, Projection)
		Frustum expandedFrustum = new Frustum(viewMatrix, expandedProj);
		expandedFrustum.prepare(currCamPos.x, currCamPos.y, currCamPos.z);

		return expandedFrustum;
	}
}

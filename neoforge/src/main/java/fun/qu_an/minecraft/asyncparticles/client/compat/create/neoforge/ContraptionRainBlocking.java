package fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.foundation.collision.CollisionList;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CollideUtil;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CollisionType;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateUtil;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.RainEffect;
import fun.qu_an.minecraft.asyncparticles.client.mixin.compat.neoforge.create.AccessorMatrix3d;
import fun.qu_an.minecraft.asyncparticles.client.util.HeightMap;
import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;

/**
 * Thanks to ChatGPT
 * FIXME: thread safe and reletive pos storage
 */
public class ContraptionRainBlocking {
	public static ContraptionHeightMap getAttachedContractionHeightMap(ClientLevel level) {
		return ((ContraptionHeightMapProvider) level).asyncparticles$getHeightMap();
	}

	/**
	 * ------------------------------------------------------------------------
	 * 原始可读版 BOX_FACE_INDICES（保留作维护对照，不参与运行）
	 * ------------------------------------------------------------------------
	 * <p>
	 * 8 个角点固定编号：
	 * <p>
	 * 0 = (minX, minY, minZ)
	 * 1 = (minX, minY, maxZ)
	 * 2 = (minX, maxY, minZ)
	 * 3 = (minX, maxY, maxZ)
	 * 4 = (maxX, minY, minZ)
	 * 5 = (maxX, minY, maxZ)
	 * 6 = (maxX, maxY, minZ)
	 * 7 = (maxX, maxY, maxZ)
	 * <p>
	 * private static final int[][] BOX_FACE_INDICES = {
	 * {0, 1, 3, 2}, // -X
	 * {4, 6, 7, 5}, // +X
	 * {0, 4, 5, 1}, // -Y
	 * {2, 3, 7, 6}, // +Y
	 * {0, 2, 6, 4}, // -Z
	 * {1, 5, 7, 3}  // +Z
	 * };
	 * <p>
	 * 现在运行时改成扁平 int[]，少一层数组解引用。
	 * <p>
	 * 盒子 6 个面的扁平索引表。
	 * 面顺序仍然与原先一致，便于维护：
	 * 0: -X -> {0, 1, 3, 2}
	 * 1: +X -> {4, 6, 7, 5}
	 * 2: -Y -> {0, 4, 5, 1}
	 * 3: +Y -> {2, 3, 7, 6}
	 * 4: -Z -> {0, 2, 6, 4}
	 * 5: +Z -> {1, 5, 7, 3}
	 * <p>
	 * 每 4 个数字表示一个四边形面。
	 */
	private static final int[] BOX_FACE_INDICES_FLAT = {
		0, 1, 3, 2,
		4, 6, 7, 5,
		0, 4, 5, 1,
		2, 3, 7, 6,
		0, 2, 6, 4,
		1, 5, 7, 3
	};

	private static final double FACE_EPS = 1.0e-7;

	/**
	 * 批量检测矩形区域内空间立方体的挡雨情况。
	 */
	public static void tickRainBlocking(ClientLevel level,
	                                    int centerX,
	                                    int centerZ,
	                                    int range) {
		ContraptionHeightMapProvider mapProvider = (ContraptionHeightMapProvider) level;
		ContraptionHeightMap heightMap = mapProvider.asyncparticles$getHeightMap();
		heightMap.beginUpdate(centerX, centerZ, range);

		final double queryMinX = centerX - range + 0.5;
		final double queryMaxX = centerX + range + 0.5;
		final double queryMinZ = centerZ - range + 0.5;
		final double queryMaxZ = centerZ + range + 0.5;

		// 整个 batch 调用期间复用，不在 collider 循环里反复分配
		final double[] cornerX = new double[8];
		final double[] cornerY = new double[8];
		final double[] cornerZ = new double[8];

		Long2FloatMap tempHeightMap = ConfigHelper.getCreateRainEffect() == RainEffect.ALWAYS ? null : new Long2FloatOpenHashMap();
		for (Iterator<Entity> it = CreateUtil.forEachContraption(level); it.hasNext(); ) {
			AbstractContraptionEntity entity = (AbstractContraptionEntity) it.next();
			Contraption contraption = entity.getContraption();
			if (contraption == null) continue;

			CollisionList colliders = contraption.getSimplifiedEntityColliders();
			if (colliders == null || colliders.size <= 0) {
				continue;
			}

			AABB aabb = CreateUtilImpl.getBoundingBox(entity);
			if ((aabb.minX > queryMaxX
				|| aabb.maxX < queryMinX
				|| aabb.minZ > queryMaxZ
				|| aabb.maxZ < queryMinZ)) {
				continue;
			}

			checkRainBlocking(entity,
				colliders,
				cornerX,
				cornerY,
				cornerZ,
				heightMap,
				tempHeightMap);
		}

		heightMap.commitUpdate();
	}

	private static void checkRainBlocking(AbstractContraptionEntity entity,
	                                      CollisionList colliders,
	                                      double[] cornerX,
	                                      double[] cornerY,
	                                      double[] cornerZ,
	                                      ContraptionHeightMap heightMap,
	                                      Long2FloatMap tempHeightMap) {
		AbstractContraptionEntity.ContraptionRotationState rotationState = entity.getRotationState();
		AccessorMatrix3d mAcc = (AccessorMatrix3d) CreateUtilImpl.asMatrix(entity, rotationState);
		Vec3 anchor = CreateUtilImpl.getAnchorVec(entity);
		float yawOffset = rotationState.getYawOffset();

		/*
		  Vec3 worldPos = localPos.subtract(VecHelper.CENTER_OF_ORIGIN);
		  worldPos = rotationMatrix.transformTransposed(worldPos);
		  worldPos = VecHelper.rotate(worldPos, (double) yawOffset, Axis.Y);
		  worldPos = worldPos.add(VecHelper.CENTER_OF_ORIGIN);
		  worldPos = worldPos.add(anchorVec);
		  return worldPos;

		  上式可整理成：
		    world = Ry(+yaw) * M^T * local + [anchor + center - Ry(+yaw) * M^T * center]
		  写成仿射形式：
		    world = T * local + offset
		  其中：
		    T = Ry(+yaw) * M^T
		 */

		final double m00 = mAcc.m00();
		final double m01 = mAcc.m01();
		final double m02 = mAcc.m02();
		final double m10 = mAcc.m10();
		final double m11 = mAcc.m11();
		final double m12 = mAcc.m12();
		final double m20 = mAcc.m20();
		final double m21 = mAcc.m21();
		final double m22 = mAcc.m22();

		final double rad = Math.toRadians(yawOffset);
		final double cos = Math.cos(rad);
		final double sin = Math.sin(rad);

		/*
		 * T = Ry(yaw) * M^T
		 *
		 * 这里采用与前面 localToWorldPos 一致的 Y 轴旋转约定：
		 *   x' = x*cos + z*sin
		 *   y' = y
		 *   z' = -x*sin + z*cos
		 */
		final double t00 = cos * m00 + sin * m02;
		final double t01 = cos * m10 + sin * m12;
		final double t02 = cos * m20 + sin * m22;

		@SuppressWarnings("UnnecessaryLocalVariable") final double t10 = m01;
		@SuppressWarnings("UnnecessaryLocalVariable") final double t11 = m11;
		@SuppressWarnings("UnnecessaryLocalVariable") final double t12 = m21;

		final double t20 = -sin * m00 + cos * m02;
		final double t21 = -sin * m10 + cos * m12;
		final double t22 = -sin * m20 + cos * m22;

		/*
		 * offset = anchor + center - T * center
		 * 0.5 is centered offset
		 */
		final double offsetX = anchor.x + .5 - (t00 * .5 + t01 * .5 + t02 * .5);
		final double offsetY = anchor.y + .5 - (t10 * .5 + t11 * .5 + t12 * .5);
		final double offsetZ = anchor.z + .5 - (t20 * .5 + t21 * .5 + t22 * .5);

		for (int i = 0; i < colliders.size; i++) {
			final double cx = colliders.centerX[i];
			final double cy = colliders.centerY[i];
			final double cz = colliders.centerZ[i];
			final double ex = colliders.extentsX[i];
			final double ey = colliders.extentsY[i];
			final double ez = colliders.extentsZ[i];

			/*
			 * ----------------------------------------------------------------
			 * 上一版优化（保留作对照，不参与运行）
			 * ----------------------------------------------------------------
			 *
			 * final double minX = cx - ex;
			 * final double minY = cy - ey;
			 * final double minZ = cz - ez;
			 * final double maxX = cx + ex;
			 * final double maxY = cy + ey;
			 * final double maxZ = cz + ez;
			 *
			 * setTransformedCorner(0, minX, minY, minZ, ...);
			 * setTransformedCorner(1, minX, minY, maxZ, ...);
			 * ...
			 * setTransformedCorner(7, maxX, maxY, maxZ, ...);
			 *
			 * ----------------------------------------------------------------
			 * 当前进一步优化：
			 * ----------------------------------------------------------------
			 *
			 * 局部 AABB 的 8 个角点可以写成：
			 *   centerLocal ± (ex,0,0) ± (0,ey,0) ± (0,0,ez)
			 *
			 * 在线性部分 T 下：
			 *   T * (center ± axisX ± axisY ± axisZ)
			 * = T*center ± T*axisX ± T*axisY ± T*axisZ
			 *
			 * 因此只需要：
			 *   1) 变换局部中心
			 *   2) 求三个世界半轴向量 vx/vy/vz
			 *   3) 8 个角点用加减组合
			 */

			// centerWorld = T * centerLocal + offset
			double worldCenterX = t00 * cx + t01 * cy + t02 * cz + offsetX;
			double worldCenterY = t10 * cx + t11 * cy + t12 * cz + offsetY;
			double worldCenterZ = t20 * cx + t21 * cy + t22 * cz + offsetZ;

//			if (extraMatrix != null) {
//				Vector3d vector3d = extraMatrix.transformPosition(worldCenterX, worldCenterY, worldCenterZ, new Vector3d());
//				worldCenterX = vector3d.x;
//				worldCenterY = vector3d.y;
//				worldCenterZ = vector3d.z;
//			}

			// 局部 X 半轴 extent 变到世界后的向量 vx = T * (ex, 0, 0)
			double vxX = t00 * ex;
			double vxY = t10 * ex;
			double vxZ = t20 * ex;
//			if (extraMatrix != null) {
//				vxX = extraMatrix.m00() * vxX;
//				vxY = extraMatrix.m10() * vxY;
//				vxZ = extraMatrix.m20() * vxZ;
//			}

			// 局部 Y 半轴 extent 变到世界后的向量 vy = T * (0, ey, 0)
			double vyX = t01 * ey;
			double vyY = t11 * ey;
			double vyZ = t21 * ey;
//			if (extraMatrix != null) {
//				vyX = extraMatrix.m01() * vyX;
//				vyY = extraMatrix.m11() * vyY;
//				vyZ = extraMatrix.m21() * vyZ;
//			}

			// 局部 Z 半轴 extent 变到世界后的向量 vz = T * (0, 0, ez)
			double vzX = t02 * ez;
			double vzY = t12 * ez;
			double vzZ = t22 * ez;
//			if (extraMatrix != null) {
//				vzX = extraMatrix.m02() * vzX;
//				vzY = extraMatrix.m12() * vzY;
//				vzZ = extraMatrix.m22() * vzZ;
//			}

			/*
			 * 按固定编号填充 8 个角点：
			 *
			 * 0 = center - vx - vy - vz
			 * 1 = center - vx - vy + vz
			 * 2 = center - vx + vy - vz
			 * 3 = center - vx + vy + vz
			 * 4 = center + vx - vy - vz
			 * 5 = center + vx - vy + vz
			 * 6 = center + vx + vy - vz
			 * 7 = center + vx + vy + vz
			 */

			// 0 = (minX, minY, minZ)
			cornerX[0] = worldCenterX - vxX - vyX - vzX;
			cornerY[0] = worldCenterY - vxY - vyY - vzY;
			cornerZ[0] = worldCenterZ - vxZ - vyZ - vzZ;

			// 1 = (minX, minY, maxZ)
			cornerX[1] = worldCenterX - vxX - vyX + vzX;
			cornerY[1] = worldCenterY - vxY - vyY + vzY;
			cornerZ[1] = worldCenterZ - vxZ - vyZ + vzZ;

			// 2 = (minX, maxY, minZ)
			cornerX[2] = worldCenterX - vxX + vyX - vzX;
			cornerY[2] = worldCenterY - vxY + vyY - vzY;
			cornerZ[2] = worldCenterZ - vxZ + vyZ - vzZ;

			// 3 = (minX, maxY, maxZ)
			cornerX[3] = worldCenterX - vxX + vyX + vzX;
			cornerY[3] = worldCenterY - vxY + vyY + vzY;
			cornerZ[3] = worldCenterZ - vxZ + vyZ + vzZ;

			// 4 = (maxX, minY, minZ)
			cornerX[4] = worldCenterX + vxX - vyX - vzX;
			cornerY[4] = worldCenterY + vxY - vyY - vzY;
			cornerZ[4] = worldCenterZ + vxZ - vyZ - vzZ;

			// 5 = (maxX, minY, maxZ)
			cornerX[5] = worldCenterX + vxX - vyX + vzX;
			cornerY[5] = worldCenterY + vxY - vyY + vzY;
			cornerZ[5] = worldCenterZ + vxZ - vyZ + vzZ;

			// 6 = (maxX, maxY, minZ)
			cornerX[6] = worldCenterX + vxX + vyX - vzX;
			cornerY[6] = worldCenterY + vxY + vyY - vzY;
			cornerZ[6] = worldCenterZ + vxZ + vyZ - vzZ;

			// 7 = (maxX, maxY, maxZ)
			cornerX[7] = worldCenterX + vxX + vyX + vzX;
			cornerY[7] = worldCenterY + vxY + vyY + vzY;
			cornerZ[7] = worldCenterZ + vxZ + vyZ + vzZ;

			// 遍历 6 个面；扁平数组版少一层数组解引用
			for (int f = 0; f < BOX_FACE_INDICES_FLAT.length; f += 4) {
				rasterizeTopQuadFaceRaw(
					tempHeightMap,
					heightMap,
					cornerX, cornerY, cornerZ,
					BOX_FACE_INDICES_FLAT[f],
					BOX_FACE_INDICES_FLAT[f + 1],
					BOX_FACE_INDICES_FLAT[f + 2],
					BOX_FACE_INDICES_FLAT[f + 3]
				);
			}
		}

		if (tempHeightMap != null && !tempHeightMap.isEmpty()) {
			for (Long2FloatMap.Entry e : tempHeightMap.long2FloatEntrySet()) {
				long l = e.getLongKey();
				float f = e.getFloatValue();
				if (!heightMap.setHeight(l, f)) {
					continue;
				}

				Vec3 globalContactPoint = new Vec3(HeightMap.getX(l), f, HeightMap.getZ(l));
				Vec3 contactPointMotion = CreateUtilImpl.getContactPointMotion(entity, globalContactPoint);
				boolean b = Math.abs(contactPointMotion.x) + Math.abs(contactPointMotion.y) + Math.abs(contactPointMotion.z)
					> CreateUtil.LENGTH_SQR_EPSILON;
				heightMap.setMoving(l, b);
			}
			tempHeightMap.clear();
		}
	}

	/**
	 * 将一个世界空间中的四边形面直接 rasterize 到 XZ 网格上。
	 * <p>
	 * 输入是：
	 * - cornerX / cornerY / cornerZ 三个数组
	 * - 四个顶点索引 ia/ib/ic/id
	 * <p>
	 * 这样避免了 Vec3 对象和额外解引用。
	 * <p>
	 * ------------------------------------------------------------------------
	 * 上一版未继续优化的实现（保留作对照，不参与运行）
	 * ------------------------------------------------------------------------
	 * <p>
	 * final double invNy = 1.0 / ny;
	 * final double kx = -nx * invNy;
	 * final double kz = -nz * invNy;
	 * final double kb = ay + (nx * ax + nz * az) * invNy;
	 * <p>
	 * <code>for (int x = fromX; x <= toX; x++) {
	 * final double sampleX = x + 0.5;
	 * <p>
	 * for (int z = fromZ; z <= toZ; z++) {
	 * final double sampleZ = z + 0.5;
	 * <p>
	 * if (!isPointInConvexQuadXZRaw(sampleX, sampleZ, ax, az, bx, bz, cx, cz, dx, dz)) {
	 * continue;
	 * }
	 * <p>
	 * final double y = kx * sampleX + kz * sampleZ + kb;
	 * long key = packXZ(x, z);
	 * float old = heightMap.getOrDefault(key, Float.NEGATIVE_INFINITY);
	 * if (y > old) heightMap.put(key, (float) y);
	 * }
	 * }</code>
	 * <p>
	 * ------------------------------------------------------------------------
	 * 当前进一步优化：
	 * ------------------------------------------------------------------------
	 * <p>
	 * 1. 不再每格调用 isPointInConvexQuadXZRaw()
	 * 2. 把四条边都写成：
	 * E(x,z) = A*x + B*z + C
	 * 3. 在 x/z 双循环中对 E 做增量更新
	 * 4. 平面高度 y = kx*x + kz*z + kb 也做增量更新
	 * <p>
	 * 将一个世界空间中的四边形面直接 rasterize 到 XZ 网格上。
	 * <p>
	 * 输入是：
	 * - cornerX / cornerY / cornerZ 三个数组
	 * - 四个顶点索引 ia/ib/ic/id
	 * <p>
	 * 这样避免了 Vec3 对象和额外解引用。
	 * <p>
	 * ------------------------------------------------------------------------
	 * 旧版（保留思路对照，不参与运行）
	 * ------------------------------------------------------------------------
	 * <p>
	 * <code>final double area2 =
	 * ax * bz - az * bx +
	 * bx * cz - bz * cx +
	 * cx * dz - cz * dx +
	 * dx * az - dz * ax;</code>
	 * <p>
	 * <code>if (Math.abs(area2) <= FACE_EPS) {
	 * return;
	 * }</code>
	 * <p>
	 * final boolean ccw = area2 > 0.0;
	 * <p>
	 * <code>if (ccw) {
	 * for (...) {
	 * if (e1 >= -FACE_EPS && e2 >= -FACE_EPS && e3 >= -FACE_EPS && e4 >= -FACE_EPS) {
	 * ...
	 * }
	 * }
	 * } else {
	 * for (...) {
	 * if (e1 <= FACE_EPS && e2 <= FACE_EPS && e3 <= FACE_EPS && e4 <= FACE_EPS) {
	 * ...
	 * }
	 * }
	 * }</code>
	 * <p>
	 * ------------------------------------------------------------------------
	 * 当前“分支更少版”
	 * ------------------------------------------------------------------------
	 * <p>
	 * 做法：
	 * 1. 先求投影四边形有向面积 area2
	 * 2. 用 orientSign = +1 或 -1 表示绕序
	 * 3. 把四条边函数整体乘上 orientSign
	 * 4. 之后统一判断：
	 * e1 >= -FACE_EPS && e2 >= -FACE_EPS && e3 >= -FACE_EPS && e4 >= -FACE_EPS
	 * <p>
	 * 这样就去掉了整块 ccw/cw 双分支循环。
	 */
	private static void rasterizeTopQuadFaceRaw(
		Long2FloatMap tempHeightMap,
		ContraptionHeightMap heightMap,
		double[] cornerX, double[] cornerY, double[] cornerZ,
		int ia, int ib, int ic, int id
	) {
		final double ax = cornerX[ia];
		final double ay = cornerY[ia];
		final double az = cornerZ[ia];

		final double bx = cornerX[ib];
		final double by = cornerY[ib];
		final double bz = cornerZ[ib];

		final double cx = cornerX[ic];
		final double cy = cornerY[ic];
		final double cz = cornerZ[ic];

		final double dx = cornerX[id];
		final double dy = cornerY[id];
		final double dz = cornerZ[id];

		// normal = (b - a) x (c - a)
		final double abx = bx - ax;
		final double aby = by - ay;
		final double abz = bz - az;

		final double acx = cx - ax;
		final double acy = cy - ay;
		final double acz = cz - az;

		final double nx = aby * acz - abz * acy;
		final double ny = abz * acx - abx * acz;
		final double nz = abx * acy - aby * acx;

		// 只保留朝上的面；同时避免 ny 太小时反解 y 不稳定
		if (ny <= FACE_EPS) {
			return;
		}

		// XZ 投影包围盒裁剪
		final double minFaceX = Math.min(Math.min(ax, bx), Math.min(cx, dx));
		final double maxFaceX = Math.max(Math.max(ax, bx), Math.max(cx, dx));
//		final double minFaceY = Math.min(Math.min(ay, by), Math.min(cy, dy));
		final double maxFaceY = Math.max(Math.max(ay, by), Math.max(cy, dy));
		final double minFaceZ = Math.min(Math.min(az, bz), Math.min(cz, dz));
		final double maxFaceZ = Math.max(Math.max(az, bz), Math.max(cz, dz));

		final int fromX = Mth.floor(minFaceX);
		final int toX = Mth.ceil(maxFaceX) - 1;
		final int fromZ = Mth.floor(minFaceZ);
		final int toZ = Mth.ceil(maxFaceZ) - 1;

		if (fromX > toX || fromZ > toZ) {
			return;
		}

		/*
		 * 平面高度：
		 *   nx*(x-ax) + ny*(y-ay) + nz*(z-az) = 0
		 * => y = ay - (nx*(x-ax) + nz*(z-az)) / ny
		 * => y = kx*x + kz*z + kb
		 */
		final double invNy = 1.0 / ny;
		final double kx = -nx * invNy;
		final double kz = -nz * invNy;
		final double kb = ay + (nx * ax + nz * az) * invNy;

		/*
		 * 四条边函数：
		 *   E(x,z) = A*x + B*z + C
		 *
		 * 对有向边 p -> q：
		 *   E(x,z) = cross(q-p, (x,z)-p)
		 */
		double e1A = -(bz - az);
		double e1B = (bx - ax);
		double e1C = (bz - az) * ax - (bx - ax) * az;

		double e2A = -(cz - bz);
		double e2B = (cx - bx);
		double e2C = (cz - bz) * bx - (cx - bx) * bz;

		double e3A = -(dz - cz);
		double e3B = (dx - cx);
		double e3C = (dz - cz) * cx - (dx - cx) * cz;

		double e4A = -(az - dz);
		double e4B = (ax - dx);
		double e4C = (az - dz) * dx - (ax - dx) * dz;

		/*
		 * 投影多边形有向面积：
		 * - area2 > 0 表示一种绕序
		 * - area2 < 0 表示相反绕序
		 *
		 * 旧版会在这里分成 ccw/cw 两套循环。
		 * 现在直接把边函数整体乘以 orientSign，
		 * 把“内部判定”统一成同一个方向。
		 */
		final double area2 =
			ax * bz - az * bx +
				bx * cz - bz * cx +
				cx * dz - cz * dx +
				dx * az - dz * ax;

		// 投影退化太严重时跳过
		if (Math.abs(area2) <= FACE_EPS) {
			return;
		}

		final double orientSign = area2 > 0.0 ? 1.0 : -1.0;

		e1A *= orientSign;
		e1B *= orientSign;
		e1C *= orientSign;

		e2A *= orientSign;
		e2B *= orientSign;
		e2C *= orientSign;

		e3A *= orientSign;
		e3B *= orientSign;
		e3C *= orientSign;

		e4A *= orientSign;
		e4B *= orientSign;
		e4C *= orientSign;

		/*
		  利用平面梯度直接确定格子内最高点

		  平面方程：y = kx*x + kz*z + kb
		  梯度：(kx, kz)

		  对于格子 [x, x+1] x [z, z+1]：
		  - 如果 kx > 0，x 越大 y 越大 → 选右边 (x+1)
		  - 如果 kx < 0，x 越小 y 越大 → 选左边 (x)
		  - 如果 kz > 0，z 越大 y 越大 → 选上边 (z+1)
		  - 如果 kz < 0，z 越小 y 越大 → 选下边 (z)

		  因此每个格子只需采样1个角点（最高角）
		 */
		// xz取判断取中心点
		final double startSampleX = fromX + 0.5;
		final double startSampleZ = fromZ + 0.5;

		/*
		  把“中心点 inside”改成“格子与四边形投影有任意相交”
		  对边函数 E(x,z)=A*x+B*z+C，在格子中心的值为 Ecenter 时，
		  它在整个 1x1 格子中的最大可能增量是 0.5*(|A|+|B|)。

		  判断与水平面夹角是否小于 45 度：
		  cos(theta) > cos(45) => ny^2 > nx^2 + nz^2
		 */
		final boolean isFlat = (ny * ny) > (nx * nx + nz * nz);

		/*
		  如果平缓 (<45度)，给半格的膨胀容差，解决漏格子问题；
		  如果陡峭 (>=45度)，容差给 0，退化回严格的“中心点在内部”判定，防止高度暴走。
		 */
		final double e1Reach;
		final double e2Reach;
		final double e3Reach;
		final double e4Reach;
		if (!isFlat) {
			e1Reach = e2Reach = e3Reach = e4Reach = 0.0;
		} else {
			e1Reach = 0.5 * (Math.abs(e1A) + Math.abs(e1B));
			e2Reach = 0.5 * (Math.abs(e2A) + Math.abs(e2B));
			e3Reach = 0.5 * (Math.abs(e3A) + Math.abs(e3B));
			e4Reach = 0.5 * (Math.abs(e4A) + Math.abs(e4B));
		}

		double rowE1 = e1A * startSampleX + e1B * startSampleZ + e1C + e1Reach;
		double rowE2 = e2A * startSampleX + e2B * startSampleZ + e2C + e2Reach;
		double rowE3 = e3A * startSampleX + e3B * startSampleZ + e3C + e3Reach;
		double rowE4 = e4A * startSampleX + e4B * startSampleZ + e4C + e4Reach;
		// 高度取格子内最高角
		double rowYCorner = kx * (fromX + (kx > 0 ? 1.0 : 0.0)) + kz * (fromZ + (kz > 0 ? 1.0 : 0.0)) + kb;

		// ---------- 梯度确定的角点偏移 ----------
		final double cornerDx = kx > 0 ? 1.0 : 0.0;
		final double cornerDz = kz > 0 ? 1.0 : 0.0;
		// 角点相对于格子中心 (x+0.5, z+0.5) 的偏移量
		final double offsetX = cornerDx - 0.5;
		final double offsetZ = cornerDz - 0.5;
		// 角点高度与中心点高度的差值（常量）
		final double cornerToCenterDY = kx * offsetX + kz * offsetZ;

		// 角点边函数的偏移常量：ΔE = A*offsetX + B*offsetZ
		final double e1CornerOffset = e1A * offsetX + e1B * offsetZ;
		final double e2CornerOffset = e2A * offsetX + e2B * offsetZ;
		final double e3CornerOffset = e3A * offsetX + e3B * offsetZ;
		final double e4CornerOffset = e4A * offsetX + e4B * offsetZ;
		for (int x = fromX; x <= toX; x++) {
			double e1 = rowE1;
			double e2 = rowE2;
			double e3 = rowE3;
			double e4 = rowE4;
			double yCorner = rowYCorner;  // 当前格子的角点高度

			for (int z = fromZ; z <= toZ; z++) {
				if (e1 >= -FACE_EPS &&
					e2 >= -FACE_EPS &&
					e3 >= -FACE_EPS &&
					e4 >= -FACE_EPS) {
					double finalY;
					// 去掉膨胀，得到中心点的严格边函数，再加角点偏移得到角点的严格边函数
					double e1Corner = (e1 - e1Reach) + e1CornerOffset;
					double e2Corner = (e2 - e2Reach) + e2CornerOffset;
					double e3Corner = (e3 - e3Reach) + e3CornerOffset;
					double e4Corner = (e4 - e4Reach) + e4CornerOffset;

					if (e1Corner >= -FACE_EPS && e2Corner >= -FACE_EPS &&
						e3Corner >= -FACE_EPS && e4Corner >= -FACE_EPS) {
						// 角点在四边形内，安全使用角点高度
						finalY = yCorner;
					} else {
						// 角点不在内，退回到中心点高度（预防外推）
						finalY = yCorner - cornerToCenterDY;
					}

					float clampedY = (float) Math.min(finalY, maxFaceY) + 0.9f;
					if (tempHeightMap == null) {
						heightMap.setHeight(x, z, clampedY);
					} else {
						tempHeightMap.mergeFloat(HeightMap.asLong(x, z), clampedY, Float::max);
					}
				}

				e1 += e1B;
				e2 += e2B;
				e3 += e3B;
				e4 += e4B;
				yCorner += kz;
			}

			rowE1 += e1A;
			rowE2 += e2A;
			rowE3 += e3A;
			rowE4 += e4A;
			rowYCorner += kx;
		}
	}

	// TODO optimize the fallback
	public static float getHeight(ClientLevel level, int x, int z) {
		ContraptionHeightMap heightMap = getAttachedContractionHeightMap(level);
		ContraptionHeightMap.State state = heightMap.getState();
		float height = state.getHeight(x, z);
		int dx = Math.abs(x - state.centerX());
		int dz = Math.abs(z - state.centerZ());
		int distance = Math.max(dx, dz);
		if (distance < state.range() || distance > 8192) {
			return height;
		}
		Vec3 start = new Vec3(x + 0.5, level.getMaxBuildHeight() + 16, z + 0.5);
		BlockHitResult result = CollideUtil.rayCast(level, start, new Vec3(x + 0.5, level.getMinBuildHeight() - 16, z + 0.5));
		if (result == null) {
			return height;
		}
		if (result.getType() == BlockHitResult.Type.MISS) {
			return height;
		}
		return (float) result.getLocation().y;
	}

	public static float getHeight(ClientLevel level, BlockPos blockPos) {
		return getHeight(level, blockPos.getX(), blockPos.getZ());
	}

	// TODO optimize the fallback
	public static boolean isMoving(ClientLevel level, int x, int z) {
		ContraptionHeightMap heightMap = getAttachedContractionHeightMap(level);
		ContraptionHeightMap.State state = heightMap.getState();
		byte moving = state.isMoving(x, z);
		int dx = Math.abs(x - state.centerX());
		int dz = Math.abs(z - state.centerZ());
		int distance = Math.max(dx, dz);
		if (distance < state.range() || distance > 8192) {
			return moving > 0;
		}
		Vec3 motion = new Vec3(x + 0.5, level.getMinBuildHeight() - level.getMaxBuildHeight() - 32, z + 0.5);
		AABB bb = AABB.ofSize(new Vec3(x + 0.5, level.getMaxBuildHeight() + 16, z + 0.5), 1, 1, 1);
		CollisionType collideWithContraptions = CollideUtil.isCollideWithContraptions(level, motion, bb);
		return collideWithContraptions == CollisionType.MOVING;
	}

	public static boolean isMoving(ClientLevel level, BlockPos blockPos) {
		return isMoving(level, blockPos.getX(), blockPos.getZ());
	}
}

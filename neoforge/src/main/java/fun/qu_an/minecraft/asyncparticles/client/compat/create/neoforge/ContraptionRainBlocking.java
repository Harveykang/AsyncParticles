package fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.foundation.collision.CollisionList;
import com.simibubi.create.foundation.collision.Matrix3d;
import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;

import static fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge.CreateUtilImpl.forEachContraption;
import static fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge.TransformUtil.localToWorldPos;

public class ContraptionRainBlocking {
	/// 盒子 6 个面的顶点索引表。
	///
	/// 局部 AABB 的 8 个角点按如下顺序编号：
	/// ```
    /// 0 = (minX, minY, minZ)
    /// 1 = (minX, minY, maxZ)
    /// 2 = (minX, maxY, minZ)
    /// 3 = (minX, maxY, maxZ)
    /// 4 = (maxX, minY, minZ)
    /// 5 = (maxX, minY, maxZ)
    /// 6 = (maxX, maxY, minZ)
    /// 7 = (maxX, maxY, maxZ)
    /// ```
	/// 每个 face 里的 4 个索引，按面边界顺序排列，这样的绕序，使得：
	/// `normal = (b - a) x (c - a)`指向盒子外侧。
	/// 后续会用 normal.y > 0 来筛选“朝上的面”，
	/// 从而只光栅化可能成为最高挡雨面的那些面。
	///
	/// 面顺序本身不是算法必须的，只是为了阅读方便：
	/// ```
    /// 0: -X
    /// 1: +X
    /// 2: -Y (bottom)
    /// 3: +Y (top)
    /// 4: -Z
    /// 5: +Z
    /// ```
	///
	/// Local AABB corner layout:
	/// ```
    /// y = maxY layer:
    ///   3 ---- 7
    ///   |      |
    ///   2 ---- 6
    /// ```
	/// ```
    /// y = minY layer:
    ///   1 ---- 5
    ///   |      |
    ///   0 ---- 4
    /// ```
	/// with:
	///
	///   x: min -> max  (left -> right in the above sketch)
	///
	///   z: min -> max  (front/back depends on your mental view; use numeric definition as ground truth)
	private static final int[][] BOX_FACE_INDICES = {
		// x = minX, outward normal = (-1, 0, 0)
		{0, 1, 3, 2},
		// x = maxX, outward normal = (+1, 0, 0)
		{4, 6, 7, 5},
		// y = minY, outward normal = (0, -1, 0)
		{0, 4, 5, 1},
		// y = maxY, outward normal = (0, +1, 0)
		{2, 3, 7, 6},
		// z = minZ, outward normal = (0, 0, -1)
		{0, 2, 6, 4},
		// z = maxZ, outward normal = (0, 0, +1)
		{1, 5, 7, 3}
	};

	private static final double FACE_EPS = 1.0e-7;

	/// 批量检测矩形区域内空间立方体的挡雨情况。
	/// 1. contraption 的 colliders（局部 AABB）
	/// 3. 把每个局部 AABB 的 8 个角点变到世界空间
	/// 4. 枚举盒子 6 个世界面
	/// 5. 只处理法线 y 分量 > 0 的面（朝上的面）
	/// 6. 将这个四边形在 XZ 平面上做光栅化
	/// 7. 对于落在四边形投影内的每个方块列中心 (x+0.5, z+0.5)，用平面方程求该点对应的 worldY
	/// 8. 最大值加入 result
	public static void batchCheckRainBlocking(Long2FloatMap result, Level level, int x1, int z1, int x2, int z2) {
		int minX = Math.min(x1, x2);
		int maxX = Math.max(x1, x2);
		int minZ = Math.min(z1, z2);
		int maxZ = Math.max(z1, z2);

		for (Iterator<AbstractContraptionEntity> it = forEachContraption(level); it.hasNext(); ) {
			AbstractContraptionEntity entity = it.next();
			Contraption contraption = entity.getContraption();
			if (contraption == null) continue;

			AbstractContraptionEntity.ContraptionRotationState rotationState = entity.getRotationState();

			// rotationState.asMatrix() 是 world -> local
			// 对于 localToWorldPos 应使用 transformTransposed
			Matrix3d matrix = rotationState.asMatrix();
			// TODO JOML

			// Contraption 的世界坐标
			Vec3 anchor = entity.getAnchorVec();

			// 额外的 yaw 偏移
			float yawOffset = rotationState.getYawOffset();

			CollisionList colliders = contraption.getSimplifiedEntityColliders();
			if (colliders == null) {
				continue;
			}

			for (int i = 0; i < colliders.size; i++) {
				// 局部AABB
				double centerX = colliders.centerX[i];
				double centerY = colliders.centerY[i];
				double centerZ = colliders.centerZ[i];
				double extentsX = colliders.extentsX[i];
				double extentsY = colliders.extentsY[i];
				double extentsZ = colliders.extentsZ[i];

				double localMinX = centerX - extentsX;
				double localMinY = centerY - extentsY;
				double localMinZ = centerZ - extentsZ;
				double localMaxX = centerX + extentsX;
				double localMaxY = centerY + extentsY;
				double localMaxZ = centerZ + extentsZ;

				/*
				 * 构造局部 AABB 的 8 个角点，并转换到世界坐标。
				 *
				 * 角点编号必须与 BOX_FACE_INDICES 的约定保持一致，
				 * 否则面拓扑和法线方向都会错。
				 */
				Vec3[] corners = new Vec3[8];
				// TODO JOML
				corners[0] = localToWorldPos(new Vec3(localMinX, localMinY, localMinZ), anchor, matrix, yawOffset);
				corners[1] = localToWorldPos(new Vec3(localMinX, localMinY, localMaxZ), anchor, matrix, yawOffset);
				corners[2] = localToWorldPos(new Vec3(localMinX, localMaxY, localMinZ), anchor, matrix, yawOffset);
				corners[3] = localToWorldPos(new Vec3(localMinX, localMaxY, localMaxZ), anchor, matrix, yawOffset);
				corners[4] = localToWorldPos(new Vec3(localMaxX, localMinY, localMinZ), anchor, matrix, yawOffset);
				corners[5] = localToWorldPos(new Vec3(localMaxX, localMinY, localMaxZ), anchor, matrix, yawOffset);
				corners[6] = localToWorldPos(new Vec3(localMaxX, localMaxY, localMinZ), anchor, matrix, yawOffset);
				corners[7] = localToWorldPos(new Vec3(localMaxX, localMaxY, localMaxZ), anchor, matrix, yawOffset);

				/*
				 * 枚举盒子 6 个面。
				 *
				 * 并不是只处理“局部 +Y 面”。
				 * 因为 contraption 已经旋转到世界空间后，
				 * 原本的侧面也可能变成朝上面。
				 *
				 * 所以我们在世界空间里重新算法线，再用 normal.y > 0 来筛选。
				 */
				for (int[] face : BOX_FACE_INDICES) {
					rasterizeTopQuadFace(
						result,
						minX, maxX,
						minZ, maxZ,
						corners[face[0]],
						corners[face[1]],
						corners[face[2]],
						corners[face[3]]
					);
				}
			}
		}
	}

	/// 将一个世界空间中的四边形面光栅化到 XZ 网格上，并把最高y写入 result。
	///
	/// 这里要求：
	/// - a/b/c/d 是同一平面上的四边形顶点
	/// - 顶点顺序沿面边界排列
	/// - 顺序应与 BOX\_FACE\_INDICES 保持一致，使得 (b-a)x(c-a) 是外法线
	private static void rasterizeTopQuadFace(
		Long2FloatMap result,
		int queryMinX, int queryMaxX,
		int queryMinZ, int queryMaxZ,
		Vec3 a, Vec3 b, Vec3 c, Vec3 d
	) {
		/*
		 * 用前三个点计算面法线：
		 * normal = (b - a) x (c - a)
		 *
		 * 这里不需要单位化，因为后面只用到：
		 * 1. normal.y 的符号判断
		 * 2. 平面方程中的相对比例
		 */
		double abx = b.x - a.x;
		double aby = b.y - a.y;
		double abz = b.z - a.z;

		double acx = c.x - a.x;
		double acy = c.y - a.y;
		double acz = c.z - a.z;

		double nx = aby * acz - abz * acy;
		double ny = abz * acx - abx * acz;
		double nz = abx * acy - aby * acx;

		/*
		 * 只 rasterize 朝上的面。
		 *
		 * ny <= 0:
		 *   - 说明这个面不是朝上
		 *   - 或者接近竖直，无法稳定地由平面方程反解 y
		 *
		 * 因为我们求的是“最高挡雨面”，
		 * 所以只有 ny > 0 的面有意义。
		 */
		if (ny <= FACE_EPS) {
			return;
		}

		/*
		 * 先求四边形在 XZ 平面上的投影包围盒，
		 * 再与用户查询矩形做裁剪。
		 *
		 * 注意我们采样的是“方块列中心”：
		 *   sampleX = x + 0.5
		 *   sampleZ = z + 0.5
		 *
		 * 因此这里要把“投影包围盒”转换成可能命中的整数 x/z 范围：
		 *   x + 0.5 ∈ [minFaceX, maxFaceX]
		 * <=> x ∈ [ceil(minFaceX - 0.5), floor(maxFaceX - 0.5)]
		 */
		double minFaceX = Math.min(Math.min(a.x, b.x), Math.min(c.x, d.x));
		double maxFaceX = Math.max(Math.max(a.x, b.x), Math.max(c.x, d.x));
		double minFaceZ = Math.min(Math.min(a.z, b.z), Math.min(c.z, d.z));
		double maxFaceZ = Math.max(Math.max(a.z, b.z), Math.max(c.z, d.z));

		int fromX = Math.max(queryMinX, Mth.ceil(minFaceX - 0.5));
		int toX = Math.min(queryMaxX, Mth.floor(maxFaceX - 0.5));
		int fromZ = Math.max(queryMinZ, Mth.ceil(minFaceZ - 0.5));
		int toZ = Math.min(queryMaxZ, Mth.floor(maxFaceZ - 0.5));

		if (fromX > toX || fromZ > toZ) {
			return;
		}

		/*
		 * 遍历可能落入该面投影的所有世界列中心。
		 *
		 * 对每个 (x, z)：
		 * 1. 取列中心 (x + 0.5, z + 0.5)
		 * 2. 判断该点是否落在四边形的 XZ 投影内部
		 * 3. 若在，则用平面方程直接求对应 y
		 * 4. 更新 result 为更高的挡雨面
		 */
		double inverseNy = 1.0 / ny;

// 根据梯度确定每个格子的最高角
		double kx = -nx * inverseNy;
		double kz = -nz * inverseNy;

		double sampleOffsetX = kx > 0 ? 1.0 : 0.0;
		double sampleOffsetZ = kz > 0 ? 1.0 : 0.0;

		for (int x = fromX; x <= toX; x++) {
			for (int z = fromZ; z <= toZ; z++) {
				// 采样最高角
				double sampleX = x + sampleOffsetX;
				double sampleZ = z + sampleOffsetZ;

				// 检查采样点是否在四边形内
				if (isPointInConvexQuadXZ(sampleX, sampleZ, a, b, c, d)) {
					double y = a.y - (nx * (sampleX - a.x) + nz * (sampleZ - a.z)) * inverseNy;

					long key = BlockPos.asLong(x, 0, z);
					float old = result.getOrDefault(key, Float.NEGATIVE_INFINITY);
					y += 1.0;
					if (y > old) {
						result.put(key, (float) y);
					}
				}
			}
		}
	}

	/**
	 * 判断一个 XZ 平面中的点 (px, pz) 是否落在凸四边形 a-b-c-d 的投影内部。
	 * <p>
	 * 这里假设：
	 * - a/b/c/d 沿四边形边界顺序排列
	 * - 四边形是凸的
	 * <p>
	 * 算法：
	 * - 对四条边分别计算 2D 叉积符号
	 * - 若符号同时出现正负，说明点在外部
	 * - 若全同号或允许少量接近 0，则说明点在内部或边界上
	 * <p>
	 * 这是一个“对绕序不敏感”的写法：
	 * - 若顶点顺序是顺时针，内部点的叉积会整体同号
	 * - 若顶点顺序是逆时针，内部点的叉积也会整体同号
	 * 只要四边形顶点顺序沿边界一致即可
	 */
	private static boolean isPointInConvexQuadXZ(double px, double pz, Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
		double c1 = cross2D(b.x - a.x, b.z - a.z, px - a.x, pz - a.z);
		double c2 = cross2D(c.x - b.x, c.z - b.z, px - b.x, pz - b.z);
		double c3 = cross2D(d.x - c.x, d.z - c.z, px - c.x, pz - c.z);
		double c4 = cross2D(a.x - d.x, a.z - d.z, px - d.x, pz - d.z);

		boolean hasPos = c1 > FACE_EPS || c2 > FACE_EPS || c3 > FACE_EPS || c4 > FACE_EPS;
		boolean hasNeg = c1 < -FACE_EPS || c2 < -FACE_EPS || c3 < -FACE_EPS || c4 < -FACE_EPS;

		return !(hasPos && hasNeg);
	}

	/**
	 * 2D 叉积：
	 * (ax, az) x (bx, bz) = ax * bz - az * bx
	 * <p>
	 * 在 XZ 平面里，它的符号可以用来判断点位于有向边的哪一侧。
	 */
	private static double cross2D(double ax, double az, double bx, double bz) {
		return ax * bz - az * bx;
	}
}

package fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.foundation.collision.CollisionList;
import com.simibubi.create.foundation.collision.Matrix3d;
import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;

import static fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge.CreateUtilImpl.forEachContraption;

public class ContraptionHeightMap {
	/**
	 * 批量检测矩形区域内所有坐标的挡雨情况（性能优化版本）
	 * <p>
	 * 该方法会检测从 (x1, z1) 到 (x2, z2) 的矩形区域内，每个整数坐标点是否被机械结构遮挡雨水。
	 * 使用 Integer.MIN_VALUE 表示未被遮挡，其他值表示遮挡物的顶部Y坐标。
	 * <p>
	 * 世界方向向机械结构局部方向变换通过rotationMatrix完成，机械结构可以任意旋转包括底面朝上
	 * 机械结构中心坐标是anchorVec
	 *
	 * @param level 客户端世界
	 * @param x1    矩形区域起始X坐标（世界坐标）
	 * @param z1    矩形区域起始Z坐标（世界坐标）
	 * @param x2    矩形区域结束X坐标（世界坐标）
	 * @param z2    矩形区域结束Z坐标（世界坐标）
	 */
	public static void batchCheckRainBlocking(
		Long2FloatMap result,
		Level level, int x1, int z1, int x2, int z2) {

		// 确保 x1 <= x2, z1 <= z2
		int minX = Math.min(x1, x2);
		int maxX = Math.max(x1, x2);
		int minZ = Math.min(z1, z2);
		int maxZ = Math.max(z1, z2);

		// 遍历所有机械结构
		for (Iterator<AbstractContraptionEntity> it = forEachContraption(level); it.hasNext(); ) {
			AbstractContraptionEntity entity = it.next();
			Contraption contraption = entity.getContraption();

			if (contraption == null) continue;

			// 获取机械结构的边界盒（世界坐标）
			AABB entityBounds = entity.getBoundingBox();

			// 快速剔除：如果机械结构与检测区域没有重叠，跳过
			if (maxX < entityBounds.minX || minX > entityBounds.maxX ||
				maxZ < entityBounds.minZ || minZ > entityBounds.maxZ) {
				continue;
			}

			// 获取简化碰撞体列表（局部坐标）
			CollisionList colliders = contraption.getSimplifiedEntityColliders();
			if (colliders == null) continue;

			// 获取机械结构的变换信息
			Vec3 anchorVec = entity.getAnchorVec();
			AbstractContraptionEntity.ContraptionRotationState rotation = entity.getRotationState();
			Matrix3d rotationMatrix = rotation.asMatrix();
			float yawOffset = rotation.getYawOffset();

			// 遍历简化碰撞体列表，批量更新遮挡高度
			for (int i = 0; i < colliders.size; i++) {
				// 获取碰撞体的局部坐标中心点和范围
				double localCenterX = colliders.centerX[i];
				double localCenterY = colliders.centerY[i];
				double localCenterZ = colliders.centerZ[i];
				double extentX = colliders.extentsX[i];
				double extentY = colliders.extentsY[i];
				double extentZ = colliders.extentsZ[i];

				// 计算碰撞体在世界坐标系中的边界盒
				// 将局部坐标的角点转换到世界坐标
				// 注意：需要减去anchorVec和CENTER_OF_ORIGIN，应用反向旋转，然后使用转置矩阵
				Vec3 localMin = new Vec3(localCenterX - extentX, localCenterY - extentY, localCenterZ - extentZ);
				Vec3 localMax = new Vec3(localCenterX + extentX, localCenterY + extentY, localCenterZ + extentZ);

				// 将局部坐标转换到世界坐标：先减去偏移，再应用转置矩阵（逆旋转），最后加上anchorVec
				// 参考 ContraptionCollider.worldToLocalPos 的逆过程
				Vec3 worldMin = localMin.subtract(Vec3.ZERO); // CENTER_OF_ORIGIN是(0.5, 0.5, 0.5)，但这里我们处理的是相对坐标
				Vec3 worldMax = localMax.subtract(Vec3.ZERO);

				// 应用yawOffset的反向旋转
				worldMin = VecHelper.rotate(worldMin, yawOffset, Direction.Axis.Y);
				worldMax = VecHelper.rotate(worldMax, yawOffset, Direction.Axis.Y);

				// 应用旋转矩阵的转置（从局部到世界的逆变换）
				worldMin = rotationMatrix.transformTransposed(worldMin).add(anchorVec);
				worldMax = rotationMatrix.transformTransposed(worldMax).add(anchorVec);

				// 计算世界坐标下的整数范围
				int colliderMinX = (int) Math.round(Math.min(worldMin.x, worldMax.x));
				int colliderMaxX = (int) Math.round(Math.max(worldMin.x, worldMax.x));
				int colliderMinZ = (int) Math.round(Math.min(worldMin.z, worldMax.z));
				int colliderMaxZ = (int) Math.round(Math.max(worldMin.z, worldMax.z));

				// 与检测区域求交集
				int intersectMinX = Math.max(minX, colliderMinX);
				int intersectMaxX = Math.min(maxX, colliderMaxX);
				int intersectMinZ = Math.max(minZ, colliderMinZ);
				int intersectMaxZ = Math.min(maxZ, colliderMaxZ);

				// 如果没有交集，跳过这个碰撞体
				if (intersectMinX > intersectMaxX || intersectMinZ > intersectMaxZ) {
					continue;
				}

				// 计算碰撞体顶部Y坐标（世界坐标）
				float topY = (float) (Math.max(worldMin.y, worldMax.y) + 1);

				// 批量更新交集区域内的遮挡高度
				for (int x = intersectMinX; x <= intersectMaxX; x++) {
					for (int z = intersectMinZ; z <= intersectMaxZ; z++) {
						long packedXZ = BlockPos.asLong(x, 0, z);
						float currentTopY = result.get(packedXZ);
						// 只保留最高的遮挡物
						if (topY > currentTopY) {
							result.put(packedXZ, topY);
						}
					}
				}
			}

		}
	}
}

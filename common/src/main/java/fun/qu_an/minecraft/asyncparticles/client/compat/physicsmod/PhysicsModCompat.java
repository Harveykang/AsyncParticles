package fun.qu_an.minecraft.asyncparticles.client.compat.physicsmod;

import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
import net.diebuddies.physics.snow.math.AABB3D;
import net.diebuddies.physics.vines.VineHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.ClientShip;

public class PhysicsModCompat {
	public static boolean collideWithShip(ClientLevel level, double x, double y, double z, AABB3D aabb) {
		Vector3d min = aabb.getMin();
		Vector3d max = aabb.getMax();
		BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos(0, 0, 0);
		Iterable<ClientShip> iterable = VSClientUtils.getShipsInAABB(
			level,
			min.x - 0.09, min.y - 0.09, min.z - 0.09,
			max.x + 0.09, max.y + 0.09, max.z + 0.09);
		AABB3D toShipAABB = new AABB3D(0, 0, 0, 0, 0, 0);
		for (ClientShip ship : iterable) {
			Matrix4dc worldToShip = ship.getWorldToShip();
			Vector3d toShipPos = worldToShip.transformPosition(new Vector3d(x, y, z));
			blockPos.set(toShipPos.x, toShipPos.y, toShipPos.z);
			BlockState state = level.getBlockState(blockPos);
			if (state.isAir()) {
				continue;
			}
			VoxelShape voxelShape = state.getCollisionShape(level, blockPos);
			if (voxelShape.isEmpty() || VineHelper.getSetting(state) != null) {
				continue;
			}
			toShipAABB.getMin().set(toShipPos.x - 0.05, toShipPos.y - 0.05, toShipPos.z - 0.05);
			toShipAABB.getMax().set(toShipPos.x + 0.05, toShipPos.y + 0.05, toShipPos.z + 0.05);
			for (AABB aabb1 : voxelShape.toAabbs()) {
				if (toShipAABB.intersect(
					aabb1.minX + blockPos.getX(),
					aabb1.minY + blockPos.getY(),
					aabb1.minZ + blockPos.getZ(),
					aabb1.maxX + blockPos.getX(),
					aabb1.maxY + blockPos.getY(),
					aabb1.maxZ + blockPos.getZ())) {
					return false;
				}
			}
		}
		return false;
	}
}

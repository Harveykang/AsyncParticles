package fun.qu_an.minecraft.asyncparticles.client.compat.create;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

public class Create6Utils {
	static Vec3 rotate(Vec3 collisionLocation, float yawOffset, Direction.Axis axis) {
		return VecHelper.rotate(collisionLocation, yawOffset, axis);
	}

	static Vec3 getCenterOf(BlockPos blockPos) {
		if (blockPos.equals(Vec3i.ZERO))
			return VecHelper.CENTER_OF_ORIGIN;
		return Vec3.atLowerCornerWithOffset(blockPos, 0.5, 0.5, 0.5);
	}
}

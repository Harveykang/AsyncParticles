package fun.qu_an.minecraft.asyncparticles.client.compat.vs2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4dc;

public class ShipHitResult extends BlockHitResult {
	public final Matrix4dc worldToShip;
	public final Matrix4dc shipToWorld;
	public final Vec3 shipMotion;

	public ShipHitResult(Vec3 vec3, Direction direction, BlockPos blockPos, boolean bl, Matrix4dc worldToShip, Matrix4dc shipToWorld, Vec3 shipMotion) {
		super(vec3, direction, blockPos, bl);
		this.worldToShip = worldToShip;
		this.shipToWorld = shipToWorld;
		this.shipMotion = shipMotion;
	}

	public static ShipHitResult of(BlockHitResult blockHitResult, Matrix4dc worldToShip, Matrix4dc shipToWorld, Vec3 shipMotion) {
		return new ShipHitResult(blockHitResult.getLocation(), blockHitResult.getDirection(), blockHitResult.getBlockPos(), blockHitResult.isInside(), worldToShip, shipToWorld, shipMotion);
	}
}

package fun.qu_an.minecraft.asyncparticles.client.compat.create;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class ContraptionHitResult extends BlockHitResult {
	public final Vec3 contactPointMotion;

	public ContraptionHitResult(Vec3 contactPointMotion, Vec3 vec3, Direction direction, BlockPos blockPos, boolean bl) {
		super(vec3, direction, blockPos, bl);
		this.contactPointMotion = contactPointMotion;
	}
}

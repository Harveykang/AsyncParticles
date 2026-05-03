package fun.qu_an.minecraft.asyncparticles.client.compat.create;

import dev.architectury.injectables.annotations.ExpectPlatform;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class CollideUtil {
	public static CollisionType isCollideWithContraptions(ClientLevel level, Vec3 motion, AABB bb) {
		return isCollideWithContraptions(level, motion, bb, true);
	}

	@Nullable
	@ExpectPlatform
	public static CollisionType isCollideWithContraptions(ClientLevel level, Vec3 motion, AABB bb, boolean estimate) {
		ExceptionUtil.throwAssertionError();
		return null;
	}

	@Nullable
	@ExpectPlatform
	public static BlockHitResult rayCast(ClientLevel level, Vec3 start, Vec3 end) {
		ExceptionUtil.throwAssertionError();
		return null;
	}

	@Nullable
	@ExpectPlatform
	public static ContraptionHitResult rayCastWithContactPointMotion(ClientLevel level, Vec3 start, Vec3 end) {
		ExceptionUtil.throwAssertionError();
		return null;
	}
}
package fun.qu_an.minecraft.asyncparticles.client.compat.create;

import dev.architectury.injectables.annotations.ExpectPlatform;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class CreateUtil {
	@ExpectPlatform
	public static Vec3 collideMotionWithContraptions(ClientLevel level, Vec3 movement, AABB aabb) {
		throw new AssertionError();
	}

	public static boolean isCollideWithContraption(@NotNull ClientLevel level, Vec3 motion1, AABB aabb1) {
		return isCollideWithContraption(level, motion1, aabb1, true);
	}

	@ExpectPlatform
	public static boolean isCollideWithContraption(@NotNull ClientLevel level, Vec3 motion1, AABB aabb1, boolean b) {
		ExceptionUtil.throwAssertionError();
		return false;
	}

	public static boolean isUnderContraption(ClientLevel level, double x, double y, double z, double size) {
		AABB bounds = new AABB(x - size, y - size, z - size, x + size, y + size, z + size);
		return isCollideWithContraption(level, new Vec3(0, Math.max(16, level.getMaxBuildHeight() - y), 0), bounds);
	}

	public static boolean isUnderContraption(ClientLevel level, Vec3 pos, double size) {
		AABB bounds = new AABB(pos.x - size, pos.y - size, pos.z - size, pos.x + size, pos.y - size, pos.z + size);
		return isCollideWithContraption(level, new Vec3(0, Math.max(16, level.getMaxBuildHeight() - pos.y), 0), bounds);
	}

	@ExpectPlatform
	public static Vec3 getContraptionDeltaMovement(Entity entity) {
		throw new AssertionError();
	}
}

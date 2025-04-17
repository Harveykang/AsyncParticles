package fun.qu_an.minecraft.asyncparticles.client.compat.create;

import dev.architectury.injectables.annotations.ExpectPlatform;
import fun.qu_an.minecraft.asyncparticles.client.util.Utils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class CreateCompat {
	@ExpectPlatform
	public static Vec3 collideMotionWithContraptions(ClientLevel level, Vec3 movement, AABB aabb) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static boolean isUnderContraption(ClientLevel instance, double x, double y, double z) {
		Utils.throwAssertionError();
		return false;
	}

	public static boolean isCollideWithContraption(@NotNull ClientLevel level, Vec3 motion1, AABB aabb1) {
		return isCollideWithContraption(level, motion1, aabb1, true);
	}

	@ExpectPlatform
	public static boolean isCollideWithContraption(@NotNull ClientLevel level, Vec3 motion1, AABB aabb1, boolean b) {
		Utils.throwAssertionError();
		return false;
	}
}

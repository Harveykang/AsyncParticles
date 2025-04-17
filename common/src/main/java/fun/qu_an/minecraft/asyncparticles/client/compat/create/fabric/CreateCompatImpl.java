package fun.qu_an.minecraft.asyncparticles.client.compat.create.fabric;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class CreateCompatImpl {
	public static Vec3 collideMotionWithContraptions(ClientLevel level, Vec3 movement, AABB aabb) {
		throw new UnsupportedOperationException();
	}

	public static boolean isUnderContraption(ClientLevel instance, double x, double y, double z) {
		throw new UnsupportedOperationException();
	}

	public static boolean isCollideWithContraption(@NotNull ClientLevel level, Vec3 motion1, AABB aabb1, boolean b) {
		throw new UnsupportedOperationException();
	}
}

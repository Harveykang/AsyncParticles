package fun.qu_an.minecraft.asyncparticles.client.compat.create.fabric;

import fun.qu_an.minecraft.asyncparticles.client.util.CollisionResult;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class CreateCompatImpl {
	public static Vec3 collideMotionWithContraptions(ClientLevel level, Vec3 movement, AABB aabb) {
		throw new UnsupportedOperationException();
	}

	public static boolean isCollideWithContraption(@NotNull ClientLevel level, Vec3 motion1, AABB aabb1, boolean b) {
		throw new UnsupportedOperationException();
	}

	public static Vec3 getContraptionDeltaMovement(Entity entity) {
		throw new UnsupportedOperationException();
	}
}

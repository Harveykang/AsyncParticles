package fun.qu_an.minecraft.asyncparticles.client.compat.particlerain;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ParticleRainUtils {
	@ExpectPlatform
	public static void onShipCollision(ClientLevel level, Vec3 location, Vec3 movement, AABB aabb) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static void onCreateCollision(@NotNull ClientLevel level, Vec3 originalMotion, @NotNull Vec3 clipMotion, @NotNull AABB aabb) {
		throw new AssertionError();
	}
}

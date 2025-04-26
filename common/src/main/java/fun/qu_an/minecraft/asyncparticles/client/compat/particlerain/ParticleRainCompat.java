package fun.qu_an.minecraft.asyncparticles.client.compat.particlerain;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

public class ParticleRainCompat {
	public static final AtomicInteger asyncparticles$particleCount = new AtomicInteger(0);
	public static final AtomicInteger asyncparticles$fogCount = new AtomicInteger(0);
	public static void clearCounters() {
		asyncparticles$particleCount.set(0);
		asyncparticles$fogCount.set(0);
	}

//	@ExpectPlatform
//	public static void onShipCollision(ClientLevel level, Vec3 location, Vec3 movement, AABB aabb) {
//		throw new AssertionError();
//	}

	public static void onCreateCollision(@NotNull ClientLevel level, Vec3 originalMotion, @NotNull Vec3 clipMotion, @NotNull AABB aabb) {
		if (onCreateCollision0()) {
			onCreateCollision1(level, originalMotion, clipMotion, aabb);
		}
	}

	@ExpectPlatform
	private static boolean onCreateCollision0() {
		throw new AssertionError();
	}

	@ExpectPlatform
	private static void onCreateCollision1(@NotNull ClientLevel level, Vec3 originalMotion, @NotNull Vec3 clipMotion, @NotNull AABB aabb) {
		throw new AssertionError();
	}
}

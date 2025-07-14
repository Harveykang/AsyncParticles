package fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.v3;

import dev.architectury.injectables.annotations.ExpectPlatform;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class ParticleRainCompat {
	public static final ParticleRainCompat INSTANCE = init0();
	public final AtomicInteger particleCount = new AtomicInteger(0);
	public final AtomicInteger fogCount = new AtomicInteger(0);

	public static void init() {
		// init0() is called in the <clinit>
		if (!ModListHelper.IS_LEGACY_PARTICLERAIN) {
			throw new IllegalStateException("Mod ParticleRain is not legacy.");
		}
	}

	@ExpectPlatform
	private static ParticleRainCompat init0() {
		throw new AssertionError();
	}

	public void clearCounters() {
		particleCount.set(0);
		fogCount.set(0);
	}

	public abstract void onShipCollision(ClientLevel level, Vec3 location, Vec3 movement, AABB aabb);

	public abstract void onCreateCollision(@NotNull ClientLevel level, Vec3 originalMotion, @NotNull Vec3 clipMotion, @NotNull AABB aabb);
}

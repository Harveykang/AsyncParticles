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
}

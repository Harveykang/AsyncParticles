package fun.qu_an.minecraft.asyncparticles.client.compat.particlerain;

import dev.architectury.injectables.annotations.ExpectPlatform;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateUtil;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.RainEffect;
import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
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

	@ExpectPlatform
	public static void onShipCollision(ClientLevel level, Vec3 location, Vec3 movement, AABB aabb) {
		throw new AssertionError();
	}

	public static void onCreateCollision(@NotNull ClientLevel level, Vec3 originalMotion, @NotNull Vec3 clipMotion, @NotNull AABB aabb) {
		RainEffect createRainEffect = ConfigHelper.getCreateRainEffect();
		if (createRainEffect != RainEffect.NONE && onCreateCollision0()) {
			Vec3 center = aabb.getCenter();
			AABB aabb1 = new AABB(center.x, aabb.minY - 1, center.z, center.x, aabb.minY, center.z);
			Vec3 motion1 = originalMotion.scale(2);
			if (CreateUtil.isCollideWithContraption(level, motion1, aabb1, false).canSpawnRainEffect(createRainEffect)) {
				Vec3 startPos = new Vec3(center.x, aabb.minY, center.z);
				Vec3 spawnPos = startPos.add(clipMotion);
				Minecraft.getInstance().particleEngine
					.createParticle(ParticleTypes.RAIN, spawnPos.x, spawnPos.y, spawnPos.z, 0, 0, 0);
			}
		}
	}

	@ExpectPlatform
	private static boolean onCreateCollision0() {
		ExceptionUtil.throwAssertionError();
		return true;
	}

}

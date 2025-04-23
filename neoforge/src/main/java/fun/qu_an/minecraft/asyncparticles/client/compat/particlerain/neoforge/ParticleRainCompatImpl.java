package fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.neoforge;

import com.leclowndu93150.particlerain.ParticleRainConfig;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class ParticleRainCompatImpl {
	public static boolean onCreateCollision0() {
		if (ModListHelper.FABRIC_PARTICLERAIN_LOADED) {
			return fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.fabric.ParticleRainCompatImpl
				.onCreateCollision0();
		}
		return ParticleRainConfig.doSplashParticles;
	}

	public static void onCreateCollision1(@NotNull ClientLevel level, Vec3 originalMotion, @NotNull Vec3 clipMotion, @NotNull AABB aabb) {
		Vec3 center = aabb.getCenter();
		AABB aabb1 = new AABB(center.x, aabb.minY - 1, center.z, center.x, aabb.minY, center.z);
		Vec3 startPos = new Vec3(center.x, aabb.minY, center.z);
		Vec3 motion1 = originalMotion.scale(2);
		if (CreateUtil.isCollideWithContraption(level, motion1, aabb1, false)) {
			Vec3 spawnPos = startPos.add(clipMotion);
			Minecraft.getInstance().particleEngine
				.createParticle(ParticleTypes.RAIN, spawnPos.x, spawnPos.y, spawnPos.z, 0, 0, 0);
		}
	}
}

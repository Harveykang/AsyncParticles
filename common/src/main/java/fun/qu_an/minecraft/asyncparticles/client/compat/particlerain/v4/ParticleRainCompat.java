package fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.v4;

import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.RainEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

import static java.lang.Math.abs;

public class ParticleRainCompat {
	public static void onCreateCollision(ClientLevel level, Vec3 position) {
		if (ConfigHelper.getCreateRainEffect() != RainEffect.NONE &&
			level.random.nextFloat() < 0.3f) {
			Minecraft.getInstance().particleEngine
				.createParticle(ParticleTypes.RAIN, position.x, position.y, position.z, 0, 0, 0);
		}
	}

	public static void onShipCollision(ClientLevel level, Vec3 position, Vec3 shipMotion) {
		RainEffect vsRainEffect = ConfigHelper.getVSRainEffect();
		if (vsRainEffect == RainEffect.NONE ||
			!(level.random.nextFloat() < 0.3f)) {
			return;
		}
		if (vsRainEffect == RainEffect.STATIONARY && abs(shipMotion.lengthSqr()) > 0.01) {
			return;
		}
		Minecraft.getInstance().particleEngine
			.createParticle(ParticleTypes.RAIN, position.x, position.y, position.z, 0, 0, 0);
	}
}

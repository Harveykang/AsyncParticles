package fun.qu_an.minecraft.asyncparticles.client.compat.particlerain;

import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.RainEffect;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.util.GameUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import pigcart.particlerain.particle.render.BlendedParticleRenderType;

import java.util.concurrent.atomic.AtomicInteger;

public class ParticleRainCompat {
	public static final AtomicInteger particleCount = new AtomicInteger();

	public static void init() {
		AsyncTickBehavior.INSTANCE.addTickInParallel(BlendedParticleRenderType.INSTANCE);
	}

	public static void onCreateCollision(ClientLevel level, Vec3 position, Vec3 contactPointMotion) {
		RainEffect createRainEffect = ConfigHelper.getCreateRainEffect();
		if (createRainEffect == RainEffect.NONE || level.random.nextFloat() >= 0.3f ||
			(createRainEffect == RainEffect.STATIONARY && GameUtil.manhattanLength(contactPointMotion) > 0.05)) {
			return;
		}
		Minecraft.getInstance().particleEngine
			.createParticle(ParticleTypes.RAIN, position.x, position.y, position.z, 0, 0, 0);
	}

	public static void onShipCollision(ClientLevel level, Vec3 position, Vec3 shipMotion) {
		RainEffect vsRainEffect = ConfigHelper.getVSRainEffect();
		if (vsRainEffect == RainEffect.NONE || level.random.nextFloat() >= 0.3f ||
			(vsRainEffect == RainEffect.STATIONARY && GameUtil.manhattanLength(shipMotion) > 0.05)) {
			return;
		}
		Minecraft.getInstance().particleEngine
			.createParticle(ParticleTypes.RAIN, position.x, position.y, position.z, 0, 0, 0);
	}
}

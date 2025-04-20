package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.simple_weather;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BaseAshSmokeParticle;
import net.minecraft.client.particle.SpriteSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tv.soaryn.simpleweather.particles.RainParticle;

@Mixin(RainParticle.class)
public class MixinRainParticle extends BaseAshSmokeParticle {
	protected MixinRainParticle(ClientLevel level, double x, double y, double z, float xSeedMultiplier, float ySpeedMultiplier, float zSpeedMultiplier, double xSpeed, double ySpeed, double zSpeed, float quadSizeMultiplier, SpriteSet sprites, float rColMultiplier, int lifetime, float gravity, boolean hasPhysics) {
		super(level, x, y, z, xSeedMultiplier, ySpeedMultiplier, zSpeedMultiplier, xSpeed, ySpeed, zSpeed, quadSizeMultiplier, sprites, rColMultiplier, lifetime, gravity, hasPhysics);
	}

	@ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", ordinal = 1, target = "Ltv/soaryn/simpleweather/particles/RainParticle;yo:D"))
	private double modifyYo(double original) {
		if (onGround || stoppedByCollision) {
			return y;
		} else {
			return original;
		}
	}
}

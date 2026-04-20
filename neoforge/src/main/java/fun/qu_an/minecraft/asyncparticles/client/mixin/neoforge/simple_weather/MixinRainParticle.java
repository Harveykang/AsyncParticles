package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.simple_weather;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge.CreateUtilImpl;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BaseAshSmokeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tv.soaryn.simpleweather.particles.RainParticle;

import java.lang.ref.WeakReference;
import java.util.Collection;

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

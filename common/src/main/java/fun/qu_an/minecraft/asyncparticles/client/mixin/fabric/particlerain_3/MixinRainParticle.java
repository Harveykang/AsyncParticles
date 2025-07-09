package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.particlerain_3;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.ParticleRainClient;
import pigcart.particlerain.particle.RainParticle;

import java.util.Collections;

@Mixin(RainParticle.class)
public abstract class MixinRainParticle extends MixinWeatherParticle {
	protected MixinRainParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Inject(method = "tick", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lpigcart/particlerain/particle/WeatherParticle;tick()V"))
	private void onTick(CallbackInfo ci) {
		if (!level.getFluidState(pos).isEmpty()) {
			asyncparticles$setInvisible(true);
		}
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		setSize(3.8F, 3.8F);
	}

	@ModifyExpressionValue(
		method = "tick",
		slice = @Slice(
			from = @At(
				value = "INVOKE",
				target = "Lpigcart/particlerain/particle/RainParticle;removeIfObstructed()Z"
			)
		),
		at = {
			@At(value = "FIELD", ordinal = 0,
				target = "Lpigcart/particlerain/particle/RainParticle;y:D"),
			@At(value = "FIELD", ordinal = 1,
				target = "Lpigcart/particlerain/particle/RainParticle;y:D")
		})
	private double modifyY(double original) {
		return original - 1.9d;
	}

	@ModifyExpressionValue(
		method = "tick",
		slice = @Slice(
			from = @At(
				value = "INVOKE",
				target = "Lpigcart/particlerain/particle/RainParticle;removeIfObstructed()Z"
			)
		),
		at = @At(
			value = "FIELD",
			remap = false,
			target = "Lpigcart/particlerain/ModConfig$RainOptions;windStrength:F"
		)
	)
	private float modifyWindStrength(float original) {
		return original >= 0 ? original + 1.895f : original - 1.895f;
	}

	@Redirect(
		method = "tick",
		slice = @Slice(
			from = @At(
				value = "INVOKE",
				target = "Lpigcart/particlerain/particle/RainParticle;removeIfObstructed()Z"
			)
		),
		at = @At(
			value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/client/particle/ParticleEngine;createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;"
		)
	)
	private Particle redirectCreateStreaks(ParticleEngine particleEngine,
										   ParticleOptions particleOptions,
										   double d,
										   double e,
										   double f,
										   double g,
										   double h,
										   double i,
										   @Local(ordinal = 0) BlockHitResult hit) {
		Vec3 v = hit.location;
		double j = ParticleRainClient.config.rain.windStrength >= 0 ? -0.005 : 0.005;
		return particleEngine.createParticle(particleOptions, v.x + j, v.y, v.z + j, g, h, i);
	}

	@Redirect(
		method = "tick",
		slice = @Slice(
			from = @At(
				value = "INVOKE",
				target = "Lpigcart/particlerain/particle/RainParticle;removeIfObstructed()Z"
			)
		),
		at = @At(
			value = "INVOKE", ordinal = 1,
			target = "Lnet/minecraft/client/particle/ParticleEngine;createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;"
		)
	)
	private Particle redirectCreateStreaksRain(ParticleEngine particleEngine,
											   ParticleOptions particleOptions,
											   double d,
											   double e,
											   double f,
											   double g,
											   double h,
											   double i,
											   @Local(ordinal = 0) BlockHitResult hit) {
		Vec3 v = hit.location;
		double j = ParticleRainClient.config.rain.windStrength >= 0 ? -0.005 : 0.005;
		return particleEngine.createParticle(particleOptions, v.x + j, v.y, v.z + j, g, h, i);
	}

	@Override
	public void move(double d, double e, double f) {
		if (this.stoppedByCollision) {
			return;
		}
		double g = d;
		double h = e;
		double i = f;
		if (this.hasPhysics && (d != (double) 0.0F || e != (double) 0.0F || f != (double) 0.0F) && d * d + e * e + f * f < MAXIMUM_COLLISION_VELOCITY_SQUARED) {
			Vec3 originalMotion = new Vec3(d, e, f);
			Vec3 apply = Type.RAIN.collide(level, new Vec3(x, y, z), originalMotion, asyncparticles$getWeatherAABB());
			if (apply == null) {
				asyncparticles$setInvisible(true);
				remove();
				return;
			}
			Vec3 motion = Entity.collideBoundingBox(
				null,
				apply,
				asyncparticles$getWeatherAABB(), // It looks good that way
				this.level,
				Collections.emptyList());
			if (!apply.equals(originalMotion) && motion.equals(apply)) {
				d = apply.x;
				e = apply.y;
				f = apply.z;
			} else {
				double d1 = Math.abs(motion.y / h);
				double d2 = g * d1;
				d = Math.abs(d2) > Math.abs(motion.x) ? motion.x : d2;
				e = motion.y;
				double d3 = i * d1;
				f = Math.abs(d3) > Math.abs(motion.z) ? motion.z : d3;
			}
		}

		if (d != (double) 0.0F || e != (double) 0.0F || f != (double) 0.0F) {
			asyncparticles$setWeatherAABB(asyncparticles$getWeatherAABB().move(d, e, f));
			this.setBoundingBox(this.getBoundingBox().move(d, e, f));
			this.setLocationFromBoundingbox();
		}

		if (Math.abs(h) >= (double) 1.0E-5F && Math.abs(e) < (double) 1.0E-5F) {
			this.stoppedByCollision = true;
		}

		this.onGround = h != e && h < (double) 0.0F;

		if (g != d) {
			this.xd = 0.0;
		}

		if (i != f) {
			this.zd = 0.0;
		}

	}
}

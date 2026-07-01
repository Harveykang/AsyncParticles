package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.forge.prettyrain;

import com.leclowndu93150.particlerain.ParticleRainClient;
import com.leclowndu93150.particlerain.particle.RainParticle;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RainParticle.class)
public abstract class MixinRainParticle extends MixinWeatherParticle {
	protected MixinRainParticle(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/core/BlockPos$MutableBlockPos;below(I)Lnet/minecraft/core/BlockPos;"))
	private BlockPos modifyMaxCount(BlockPos.MutableBlockPos instance, int i) {
		return instance;
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		setSize(3.8F, 3.8F);
	}

	@ModifyExpressionValue(
		method = "tick",
		slice = @Slice(
			from = @At(
				value = "INVOKE", remap = false,
				target = "Lcom/leclowndu93150/particlerain/particle/RainParticle;removeIfObstructed()Z"
			)
		),
		at = {
		@At(value = "FIELD", ordinal = 0,
			target = "Lcom/leclowndu93150/particlerain/particle/RainParticle;y:D"),
		@At(value = "FIELD", ordinal = 1,
			target = "Lcom/leclowndu93150/particlerain/particle/RainParticle;y:D")
	})
	private double modifyY(double original) {
		return original - 1.9d;
	}

	@ModifyExpressionValue(
		method = "tick",
		slice = @Slice(
			from = @At(
				value = "INVOKE", remap = false,
				target = "Lcom/leclowndu93150/particlerain/particle/RainParticle;removeIfObstructed()Z"
			)
		),
		at = @At(
			value = "FIELD",
			remap = false,
			target = "Lcom/leclowndu93150/particlerain/ModConfig$RainOptions;windStrength:F"
		)
	)
	private float modifyWindStrength(float original) {
		return original >= 0 ? original + 1.895f : original - 1.895f;
	}

	@Redirect(
		method = "tick",
		slice = @Slice(
			from = @At(
				value = "INVOKE", remap = false,
				target = "Lcom/leclowndu93150/particlerain/particle/RainParticle;removeIfObstructed()Z"
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
				value = "INVOKE", remap = false,
				target = "Lcom/leclowndu93150/particlerain/particle/RainParticle;removeIfObstructed()Z"
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
}

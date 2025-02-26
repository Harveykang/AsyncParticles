package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.particlerain;

import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.CountManagements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.particle.WeatherParticle;

@Mixin(value = WeatherParticle.class)
public abstract class MixinWeatherPatricle {
	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		CountManagements.asyncParticles$particleCount.getAndIncrement();
	}

	@Inject(method = "remove", at = @At(value = "FIELD", remap = false, ordinal = 0, target = "Lpigcart/particlerain/ParticleRainClient;particleCount:I"))
	private void onRemove(CallbackInfo ci) {
		CountManagements.asyncParticles$particleCount.getAndDecrement();
	}
}

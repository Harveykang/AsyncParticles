package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.particlerain;

import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.particle.WeatherParticle;

@Mixin(value = ParticleEngine.class, priority = 1500)
public class MixinParticleEngine {
	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;getParticleLimit()Ljava/util/Optional;"))
	public void tick(CallbackInfo ci, @Local(ordinal = 0) Particle particle) {
		if (particle instanceof WeatherParticle) {
			ParticleRainCompat.particleCount.getAndDecrement();
		}
	}
}

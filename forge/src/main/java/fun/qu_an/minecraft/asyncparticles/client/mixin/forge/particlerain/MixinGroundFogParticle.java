package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.particlerain;

import com.leclowndu93150.particlerain.particle.GroundFogParticle;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GroundFogParticle.class)
public class MixinGroundFogParticle {
	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		ParticleRainCompat.asyncParticles$fogCount.getAndIncrement();
	}

	@Inject(method = "remove", at = @At(value = "FIELD", remap = false, ordinal = 0, target = "Lcom/leclowndu93150/particlerain/ParticleRainClient;fogCount:I"))
	private void onRemove(CallbackInfo ci) {
		ParticleRainCompat.asyncParticles$fogCount.getAndDecrement();
	}
}

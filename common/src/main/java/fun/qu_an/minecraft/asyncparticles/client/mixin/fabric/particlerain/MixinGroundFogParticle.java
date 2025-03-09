package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.particlerain;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.CountManagements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.particle.GroundFogParticle;

@Mixin(GroundFogParticle.class)
public abstract class MixinGroundFogParticle implements ParticleAddon {
	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		CountManagements.asyncParticles$fogCount.getAndIncrement();
	}

	@Inject(method = "remove", at = @At(value = "FIELD", ordinal = 0, remap = false, target = "Lpigcart/particlerain/ParticleRainClient;fogCount:I"))
	private void onRemove(CallbackInfo ci) {
		CountManagements.asyncParticles$fogCount.getAndDecrement();
	}

	@Override
	public boolean shouldCull() {
		return false;
	}
}

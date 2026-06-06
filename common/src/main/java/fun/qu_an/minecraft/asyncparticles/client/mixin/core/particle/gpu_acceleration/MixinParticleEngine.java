package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class MixinParticleEngine {
	@Inject(method = "clearParticles", at = @At("HEAD"))
	public void clearParticles(CallbackInfo ci) {
		GpuParticleBehavior.INSTANCE.onClearParticles();
	}
}

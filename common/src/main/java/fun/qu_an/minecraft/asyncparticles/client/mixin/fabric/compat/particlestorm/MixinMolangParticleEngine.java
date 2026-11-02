package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.compat.particlestorm;

import fun.qu_an.minecraft.asyncparticles.client.compat.particlestorm.ParticleStormCompat;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MolangParticleEngine.class, remap = false)
public abstract class MixinMolangParticleEngine {
	@Inject(method = "removeAll", at = @At("HEAD"))
	private void releaseRenderer(CallbackInfo ci) {
		ParticleStormCompat.release();
	}
}

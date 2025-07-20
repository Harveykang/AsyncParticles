package fun.qu_an.minecraft.asyncparticles.client.mixin.create;

import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.foundation.ponder.PonderWorldParticles")
public class MixinPonderWorldParticles {
	@Inject(method = "tickParticleList", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;tick()V"))
	private void onTickParticle(CallbackInfo ci, @Local(ordinal = 0) Particle p) {
		if (ConfigHelper.particleLightCache()) {
			((LightCachedParticleAddon) p).asyncparticles$refresh();
		}
	}
}

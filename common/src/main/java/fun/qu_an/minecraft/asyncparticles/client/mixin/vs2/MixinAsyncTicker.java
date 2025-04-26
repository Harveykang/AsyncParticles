package fun.qu_an.minecraft.asyncparticles.client.mixin.vs2;

import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSCompat;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AsyncTicker.class, remap = false)
public class MixinAsyncTicker {
	@Inject(method = "tickSyncParticles", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
		target = "Lnet/minecraft/client/particle/ParticleEngine;tickParticle(Lnet/minecraft/client/particle/Particle;)V"))
	private static void onTickSyncParticles(CallbackInfo ci, @Local(ordinal = 0) Particle particle) {
		VSCompat.removeIfOutOfSight(particle);
	}
}

package fun.qu_an.minecraft.asyncparticles.client.mixin.vs2;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSCompat;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TrackingEmitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ParticleEngine.class, priority = 1500)
public class MixinMixinParticleEngine {
	@TargetHandler(
		mixin = "fun.qu_an.minecraft.asyncparticles.client.mixin.tick.MixinParticleEngine",
		name = "asyncparticles$tickEmitters"
	)
	@Inject(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
		target = "Lnet/minecraft/client/particle/TrackingEmitter;tick()V"))
	private void onTickEmitter(CallbackInfo ci, @Local(ordinal = 0) TrackingEmitter emitter) {
		VSCompat.removeIfOutOfSight(emitter);
	}

	@Inject(method = "tickParticle", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
		target = "Lnet/minecraft/client/particle/Particle;tick()V"))
	private void onTickParticleList(Particle particle, CallbackInfo ci) {
		VSCompat.removeIfOutOfSight(particle);
	}
}

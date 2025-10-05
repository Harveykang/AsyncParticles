package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.tick;

import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
	@Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;tick()V"))
	private void runTick(boolean bl, CallbackInfo ci, @Local(ordinal = 1) int index) {
		AsyncTickBehavior.preTick(index == 0);
	}

	@Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;tick()V", shift = At.Shift.AFTER))
	private void runTickAfter(boolean bl, CallbackInfo ci, @Local(ordinal = 0) int max, @Local(ordinal = 1) int index) {
		AsyncTickBehavior.postTick(index == max - 1);
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;tick()V"))
	private void redirectParticleEngineTick(ParticleEngine instance) {
		if (!ConfigHelper.isTickAsync()) {
			instance.tick();
		}
	}
}

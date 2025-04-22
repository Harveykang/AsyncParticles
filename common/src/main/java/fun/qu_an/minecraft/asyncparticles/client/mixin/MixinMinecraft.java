package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
	@Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;runAllTasks()V"))
	private void onRunAllTasks(boolean bl, CallbackInfo ci) {
		AsyncTicker.onRunAllTasks();
	}

	@Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;tick()V"))
	private void onRunTick(boolean bl, CallbackInfo ci, @Local(ordinal = 0) int i, @Local(ordinal = 1) int j) {
		AsyncTicker.onTickBefore(j, Math.min(10, i));
	}

	@Inject(method = "runTick", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/Minecraft;tick()V"))
	private void onRunTickAfter(boolean bl, CallbackInfo ci, @Local(ordinal = 0) int i, @Local(ordinal = 1) int j) {
		AsyncTicker.onTickAfter(j, Math.min(10, i));
	}

	@Inject(method = "setLevel", at = @At(value = "FIELD", ordinal = 0,
		target = "Lnet/minecraft/client/Minecraft;level:Lnet/minecraft/client/multiplayer/ClientLevel;"))
	private void onSetLevel(CallbackInfo ci) {
		AsyncTicker.reset();
		AsyncRenderer.reset();
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;tick()V"))
	private void redirectParticleEngineTick(ParticleEngine instance) {
		AsyncTicker.tickSyncParticles();
	}
}

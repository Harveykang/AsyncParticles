package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.tick;

import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
	@Unique
	private boolean asyncparticles$alreadyReset = false;

	@Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;tick()V"))
	private void onPreTick(boolean bl, CallbackInfo ci, @Local(ordinal = 0) int ticksToDo, @Local(ordinal = 1) int i) {
		AsyncTickBehavior.getInstance().preTick(i == 0, i == ticksToDo - 1);
	}

	@Inject(method = "runTick", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/Minecraft;tick()V"))
	private void onPostTick(boolean bl, CallbackInfo ci) {
		AsyncTickBehavior.getInstance().postTick();
	}

	@Inject(method = {"setLevel", "clearLevel()V"}, at = @At("HEAD"))
	private void onSetLevel(CallbackInfo ci) {
		AsyncTickBehavior.getInstance().reset();
		asyncparticles$alreadyReset = true;
	}

	@Inject(method = "updateScreenAndTick", at = @At("HEAD"))
	private void onUpdateScreenAndTick(CallbackInfo ci) {
		if (!asyncparticles$alreadyReset) {
			AsyncTickBehavior.getInstance().reset();
			asyncparticles$alreadyReset = true;
		}
	}

	@Inject(method = {"setLevel", "clearLevel()V"}, at = @At("RETURN"))
	private void onSetLevelReturn(CallbackInfo ci) {
		asyncparticles$alreadyReset = false;
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;tick()V"))
	private void redirectParticleEngineTick(ParticleEngine instance) {
		if (!ConfigHelper.isAsyncParticleTick()) {
			instance.tick();
		}
	}
}

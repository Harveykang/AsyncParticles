package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.tick;

import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.render.AsyncRenderBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
	@Shadow
	@Final
	public ParticleEngine particleEngine;
	@Unique
	private boolean asyncparticles$sorted = false;
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

	@Inject(method = {"setLevel", "clearClientLevel", "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V"}, at = @At("HEAD"))
	private void onSetLevel(CallbackInfo ci) {
		AsyncTickBehavior.getInstance().reset();
		AsyncRenderBehavior.getInstance().reset();
		asyncparticles$alreadyReset = true;
	}

	@Inject(method = "updateScreenAndTick", at = @At("HEAD"))
	private void onUpdateScreenAndTick(CallbackInfo ci) {
		if (!asyncparticles$alreadyReset) {
			AsyncTickBehavior.getInstance().reset();
			AsyncRenderBehavior.getInstance().reset();
			asyncparticles$alreadyReset = true;
		}
	}

	@Inject(method = {"setLevel", "clearClientLevel", "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V"}, at = @At("RETURN"))
	private void onSetLevelReturn(CallbackInfo ci) {
		asyncparticles$alreadyReset = false;
	}

	@Inject(method = "setLevel", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
		target = "Lnet/minecraft/client/Minecraft;updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;)V"))
	private void afterSetLevel(CallbackInfo ci) {
		if (!asyncparticles$sorted) {
			asyncparticles$sorted = true;
			((ParticleEngineAddon) particleEngine).asyncparticle$sortRenderOrder();
		}
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;tick()V"))
	private void redirectParticleEngineTick(ParticleEngine instance) {
		if (!ConfigHelper.isAsyncParticleTick()) {
			instance.tick();
		}
	}
}

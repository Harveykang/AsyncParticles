package fun.qu_an.minecraft.asyncparticles.client.mixin.core.tick;

import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.particle.GpuParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import org.objectweb.asm.Opcodes;
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

	@Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;tick()V"))
	private void onTick(boolean bl, CallbackInfo ci, @Local(ordinal = 0) int i, @Local(ordinal = 1) int j) {
		AsyncTicker.onTickBefore(j, Math.min(10, i));
	}

	@Inject(method = "runTick", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/Minecraft;tick()V"))
	private void onTickAfter(boolean bl, CallbackInfo ci, @Local(ordinal = 0) int i, @Local(ordinal = 1) int j) {
		AsyncTicker.onTickAfter(j, Math.min(10, i));
	}

	@Inject(method = "setLevel", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, ordinal = 0,
		target = "Lnet/minecraft/client/Minecraft;level:Lnet/minecraft/client/multiplayer/ClientLevel;"))
	private void onSetLevel(CallbackInfo ci) {
		AsyncTicker.reset();
		AsyncRenderer.reset();
	}

	@Inject(method = "setLevel", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
		target = "Lnet/minecraft/client/Minecraft;updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;)V"))
	private void afterSetLevel(CallbackInfo ci) {
		if (!asyncparticles$sorted) {
			asyncparticles$sorted = true;
			((ParticleEngineAddon) particleEngine).asyncparticle$sortRenderOrder();
			GpuParticles.setInternalParticleLimit(ConfigHelper.getParticleLimit());
		}
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;tick()V"))
	private void redirectParticleEngineTick(ParticleEngine instance) {
		if (ConfigHelper.isTickAsync()) {
			AsyncTicker.tickSyncParticles();
		} else {
			instance.tick();
		}
	}
}

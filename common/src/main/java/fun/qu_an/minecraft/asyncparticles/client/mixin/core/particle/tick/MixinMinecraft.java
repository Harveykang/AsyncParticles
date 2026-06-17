package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.tick;

import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
	@Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;tick()V"))
	private void onPreTick(boolean advanceGameTime, CallbackInfo ci, @Local(ordinal = 0) int ticksToDo, @Local(ordinal = 1) int i) {
		AsyncTickBehavior.getInstance().preTick(i == 0, i == ticksToDo - 1);
	}

	@Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;tick()V", shift = At.Shift.AFTER))
	private void onPostTick(boolean advanceGameTime, CallbackInfo ci, @Local(ordinal = 0) int ticksToDo, @Local(ordinal = 1) int i) {
		AsyncTickBehavior.getInstance().postTick();
	}

	@Inject(method = "setLevel", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, ordinal = 0,
		target = "Lnet/minecraft/client/Minecraft;level:Lnet/minecraft/client/multiplayer/ClientLevel;"))
	private void onSetLevel(CallbackInfo ci) {
		AsyncTickBehavior.getInstance().reset();
	}

	@Inject(method = "setLevel", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
		target = "Lnet/minecraft/client/Minecraft;updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;)V"))
	private void afterSetLevel(CallbackInfo ci) {
		GpuParticleBehavior.getInstance().setUpNextTickRendering(ConfigHelper.getParticleLimit());
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;tick()V"))
	private void redirectParticleEngineTick(ParticleEngine instance) {
		if (ConfigHelper.isAsyncTickParticle()) {
			((ParticleEngineAddon) instance).asyncparticle$tickSyncParticles();
		} else {
			instance.tick();
		}
	}
}

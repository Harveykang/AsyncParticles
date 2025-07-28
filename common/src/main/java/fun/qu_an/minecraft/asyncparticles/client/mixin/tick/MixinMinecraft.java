package fun.qu_an.minecraft.asyncparticles.client.mixin.tick;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;

@Mixin(Minecraft.class)
public class MixinMinecraft {
	@Shadow
	@Final
	public ParticleEngine particleEngine;
	@Unique
	private boolean asyncparticles$isPrepared = false;

	@Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;tick()V"))
	private void onRunTick(boolean bl, CallbackInfo ci, @Local(ordinal = 0) int i, @Local(ordinal = 1) int j) {
		AsyncTicker.onTickBefore(j, Math.min(10, i));
	}

	@Inject(method = "runTick", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/Minecraft;tick()V"))
	private void onRunTickAfter(boolean bl, CallbackInfo ci, @Local(ordinal = 0) int i, @Local(ordinal = 1) int j) {
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
		if (!asyncparticles$isPrepared) {
			asyncparticles$isPrepared = true;
			// make custom types render after non-customs
			// Remove duplicated render types, (e.g. Hex Casting mod's bug)
			Set<ParticleRenderType> renderTypes = new LinkedHashSet<>((int) (ParticleEngine.RENDER_ORDER.size() * 1.34) + 1);
			for (ParticleRenderType type : ParticleEngine.RENDER_ORDER) {
				if (AsyncRenderer.getVertexFormatPair(type, particleEngine.textureManager) != AsyncRenderer.EMPTY_FORMAT) {
					renderTypes.add(type);
				}
			}
			for (ParticleRenderType type : ParticleEngine.RENDER_ORDER) {
				if (AsyncRenderer.getVertexFormatPair(type, particleEngine.textureManager) == AsyncRenderer.EMPTY_FORMAT) {
					renderTypes.add(type);
				}
			}
			ParticleEngine.RENDER_ORDER = ImmutableList.copyOf(renderTypes);
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

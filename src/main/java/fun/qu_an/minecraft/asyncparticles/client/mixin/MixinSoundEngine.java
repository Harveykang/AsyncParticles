package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import net.minecraft.client.sounds.SoundEngine;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public class MixinSoundEngine {
	@Shadow
	@Final
	private static Logger LOGGER;

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void onTick(boolean bl, CallbackInfo ci) {
		// TODO: 是否可以异步？（一些 mod 频繁播放音效会导致卡顿）
		if (!AsyncTicker.shouldTickParticles) {
			ci.cancel();
		}
	}

	@WrapMethod(method = "tick")
	private void wrapTick(boolean bl, Operation<Void> original) {
		try {
			// FIXME: 查明原因
			original.call(bl);
		} catch (NullPointerException e) {
			LOGGER.error("Error while ticking particles", e);
		}
	}
}

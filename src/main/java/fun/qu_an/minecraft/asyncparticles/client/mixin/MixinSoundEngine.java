package fun.qu_an.minecraft.asyncparticles.client.mixin;

import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public class MixinSoundEngine {
//	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
//	private void onTick(boolean bl, CallbackInfo ci) {
//		// TODO: 是否可以异步？（一些 mod 频繁播放音效会导致卡顿）
//		if (!AsyncTicker.shouldTickParticles) {
//			ci.cancel();
//		}
//	}
}

package fun.qu_an.minecraft.asyncparticles.client.mixin.core.off_thread_access;

import com.mojang.blaze3d.platform.GlStateManager;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
public class MixinGlStateManager {
	@Inject(method = "_deleteTexture", remap = false, at = @At("HEAD"), cancellable = true)
	private static void deleteTexture(int i, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> GlStateManager._deleteTexture(i));
		}
	}

	@Inject(method = "_deleteTextures", remap = false, at = @At("HEAD"), cancellable = true)
	private static void deleteTextures(int[] is, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> GlStateManager._deleteTextures(is));
		}
	}
}

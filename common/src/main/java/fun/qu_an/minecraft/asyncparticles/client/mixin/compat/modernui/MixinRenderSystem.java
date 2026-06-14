package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.modernui;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSystem.class, priority = 1500, remap = false)
public class MixinRenderSystem {
	@Inject(method = "assertOnRenderThread", at = @At("HEAD"))
	private static void onRenderThread(CallbackInfo ci) {
		ThreadUtil.assertNotParticleThread();
	}
}

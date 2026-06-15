package fun.qu_an.minecraft.asyncparticles.client.mixin.core;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.compat.GLCaps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class MixinRenderSystem {
	@Inject(method = "initRenderer", at = @At("RETURN"))
	private static void onInitRenderer(GpuDevice device, CallbackInfo ci) {
		GLCaps.init();
	}
}

package fun.qu_an.minecraft.asyncparticles.client.mixin.core.render;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.compat.GLCaps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class MixinRenderSystem {
	@Inject(method = "initRenderer", remap = false, at = @At("RETURN"))
	private static void onInitRenderer(int i, boolean bl, CallbackInfo ci) {
		GLCaps.init();
	}
}

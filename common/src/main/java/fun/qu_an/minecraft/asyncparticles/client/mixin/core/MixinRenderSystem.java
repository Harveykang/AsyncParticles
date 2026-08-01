package fun.qu_an.minecraft.asyncparticles.client.mixin.core;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.compat.GLCaps;
import fun.qu_an.minecraft.asyncparticles.client.coremod.AsyncParticlesMixinPlugin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class MixinRenderSystem {
	@Inject(method = "initRenderer", remap = false, at = @At("RETURN"))
	private static void onInitRenderer(CallbackInfo ci) {
		GLCaps.init();
	}

	@Inject(method = "initRenderer", remap = false, order = 9000, at = @At("RETURN"))
	private static void onInitRendererLate(CallbackInfo ci) {
		AsyncParticlesMixinPlugin.LOGGER
			.info("AsyncParticles will cancel some of its mixins to achieve specific functions, so you can ignore the related Mixinsquared-canceller warnings below.");
	}
}

package fun.qu_an.minecraft.asyncparticles.client.mixin.core.render;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.coremod.AsyncParticlesMixinPlugin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSystem.class, priority = 9000)
public class MixinRenderSystem_Late {
	@Inject(method = "initRenderer", remap = false, at = @At("RETURN"))
	private static void onInitRendererLate(CallbackInfo ci) {
		AsyncParticlesMixinPlugin.LOGGER
			.info("AsyncParticles will cancel some of its mixins to achieve specific functions, so you can ignore the related Mixinsquared-canceller warnings below.");
	}
}

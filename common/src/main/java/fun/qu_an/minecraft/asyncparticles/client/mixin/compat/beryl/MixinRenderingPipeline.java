package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.beryl;

import fun.qu_an.minecraft.asyncparticles.client.compat.beryl.BerylCompat;
import net.beryl.render.RenderingPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderingPipeline.class)
public class MixinRenderingPipeline {
	@Inject(method = "setupParticleShader", at = @At("HEAD"))
	private static void onSetupParticleShader(CallbackInfo ci) {
		BerylCompat.onShaderBegin();
	}

	@Inject(method = "resetShaders", at = @At("HEAD"))
	private static void onResetShaders(CallbackInfo ci) {
		BerylCompat.onShaderEnd();
	}
}

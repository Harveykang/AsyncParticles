package fun.qu_an.minecraft.asyncparticles.client.mixin.acceleratedrendering;

import com.github.argon4w.acceleratedrendering.features.entities.AcceleratedEntityRenderingFeature;
import com.github.argon4w.acceleratedrendering.features.items.AcceleratedItemRenderingFeature;
import com.github.argon4w.acceleratedrendering.features.text.AcceleratedTextRenderingFeature;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AsyncRenderer.class)
public class MixinAsyncRenderer {
	@Inject(method = "irisCustom", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LightTexture;turnOnLightLayer()V"))
	private static void onIrisCustomTurnOnLightLayer(CallbackInfo ci) {
		AcceleratedEntityRenderingFeature.useVanillaPipeline();
		AcceleratedItemRenderingFeature.useVanillaPipeline();
		AcceleratedTextRenderingFeature.useVanillaPipeline();
	}

	@Inject(method = "irisCustom", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LightTexture;turnOffLightLayer()V"))
	private static void onIrisCustomTurnOffLightLayer(CallbackInfo ci) {
		AcceleratedEntityRenderingFeature.resetPipeline();
		AcceleratedItemRenderingFeature.resetPipeline();
		AcceleratedTextRenderingFeature.resetPipeline();
	}
}

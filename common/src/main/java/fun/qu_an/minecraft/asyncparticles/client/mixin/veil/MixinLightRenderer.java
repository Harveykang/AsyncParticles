package fun.qu_an.minecraft.asyncparticles.client.mixin.veil;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Pseudo
@Mixin(targets = "foundry.veil.api.client.render.light.renderer.LightRenderer", remap = false)
public class MixinLightRenderer {
	@Mutable
	@Shadow
	@Final
	private Map<?, ?> renderers;

	@Dynamic
	@Inject(method = "<init>", require = 0, at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		renderers = new ConcurrentHashMap<>(renderers);
	}
}

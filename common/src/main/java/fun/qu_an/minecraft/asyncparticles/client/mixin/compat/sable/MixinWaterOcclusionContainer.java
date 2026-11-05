package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.sable;

import dev.ryanhcode.sable.sublevel.water_occlusion.WaterOcclusionContainer;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WaterOcclusionContainer.class, remap = false)
public class MixinWaterOcclusionContainer {
	@Inject(method = "getContainer", at = @At("HEAD"), cancellable = true)
	private static void skipWorkerLookup(CallbackInfoReturnable<WaterOcclusionContainer<?>> cir) {
		if (ThreadUtil.isOnParticleThread()) {
			cir.setReturnValue(null);
		}
	}
}

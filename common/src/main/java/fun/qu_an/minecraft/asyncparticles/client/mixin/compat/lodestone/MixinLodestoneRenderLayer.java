package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.lodestone;

import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.lodestar.lodestone.handlers.RenderHandler;

@Mixin(RenderHandler.LodestoneRenderLayer.class)
public class MixinLodestoneRenderLayer {
	@Inject(method = {"getBuffers", "getParticleBuffers", "getTarget", "getParticleTarget"},
		remap = false,
		at = @At("HEAD"))
	private void onGetBuffers(CallbackInfoReturnable<Boolean> cir) {
		ThreadUtil.assertNotParticleRendererThread();
	}
}

package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.immersive_portals;

import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;

@Mixin(value = ParticleEngine.class)
public class MixinParticleEngine {
	@Dynamic
	@Inject(method = {
		"Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V",
		"Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V"},
		at = @At("HEAD"), cancellable = true)
	private void onRender(CallbackInfo ci) {
		if (PortalRendering.isRendering()) {
			ci.cancel();
		}
	}

//	@Dynamic
//	@Inject(method = {
//		"tick"},
//		at = @At("HEAD"), cancellable = true)
//	private void onTick(CallbackInfo ci) {
//		if (PortalRendering.isRendering()) {
//			ci.cancel();
//		}
//	}
}

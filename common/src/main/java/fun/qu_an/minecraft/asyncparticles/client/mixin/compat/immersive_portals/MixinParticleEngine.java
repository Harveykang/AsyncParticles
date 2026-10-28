package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.immersive_portals;

import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;

@Mixin(value = ParticleEngine.class)
public class MixinParticleEngine {
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void onRemoteTick(CallbackInfo ci) {
		// The regular tick already advances particles in every level.
		if (ClientWorldLoader.isClientRemoteTicking) {
			ci.cancel();
		}
	}

	@Dynamic
	@Inject(method = {
		"Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V",
		"Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"},
		at = @At("HEAD"), cancellable = true)
	private void onRender(CallbackInfo ci) {
		if (PortalRendering.isRendering() && RenderStates.getRenderedPortalNum() > 4) {
			ci.cancel();
		}
	}
}

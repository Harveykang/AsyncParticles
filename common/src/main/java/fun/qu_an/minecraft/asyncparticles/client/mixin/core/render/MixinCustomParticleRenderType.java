package fun.qu_an.minecraft.asyncparticles.client.mixin.core.render;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.particle.ParticleRenderType$5")
public class MixinCustomParticleRenderType {
	@Inject(method = "end", at = @At("HEAD"))
	private void onEnd(CallbackInfo ci) {
		// this fixes some particles rendered after the CUSTOM render type could be seen through blocks.
		RenderSystem.enableDepthTest();
	}
}

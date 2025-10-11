package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.async_render;

import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_render.AsyncRenderBehavior;
import net.minecraft.client.renderer.state.ParticlesRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticlesRenderState.class)
public class MixinParticlesRenderState {
	/**
	 * @see MixinParticleEngine#onExtractRenderState
	 */
	@Inject(method = "submit", at = @At(value = "HEAD"))
	public void onSubmit(CallbackInfo ci) {
		AsyncRenderBehavior.waitRenderingFuture();
	}
}

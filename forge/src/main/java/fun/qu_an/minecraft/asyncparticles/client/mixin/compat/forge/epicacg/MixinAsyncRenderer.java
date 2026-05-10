package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.forge.epicacg;

import com.dfdyz.epicacg.client.render.pipeline.PostEffectPipelines;
import com.dfdyz.epicacg.client.render.pipeline.PostParticleRenderType;
import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncRenderBehavior;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AsyncRenderBehavior.class)
public class MixinAsyncRenderer {
	@Inject(method = "start", remap = false, at = @At(value = "INVOKE",
		target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;add(Ljava/lang/Object;)Z"))
	private void activatePostEffectPipeline(CallbackInfo ci, @Local ParticleRenderType particleRenderType) {
		if (particleRenderType instanceof PostParticleRenderType){
			PostEffectPipelines.active();
		}
	}
}

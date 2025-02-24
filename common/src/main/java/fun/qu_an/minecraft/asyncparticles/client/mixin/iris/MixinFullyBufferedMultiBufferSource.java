package fun.qu_an.minecraft.asyncparticles.client.mixin.iris;

import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.util.AssertionUtil;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FullyBufferedMultiBufferSource.class)
public class MixinFullyBufferedMultiBufferSource {
	@Inject(method = "getBuffer", at = @At("HEAD"))
	private void getBuffer(RenderType renderType, CallbackInfoReturnable<VertexConsumer> cir) {
		AssertionUtil.assertNotParticleThread();
	}

	@Inject(method = "endBatch()V", at = @At("HEAD"))
	private void endBatch(CallbackInfo ci) {
		AssertionUtil.assertNotParticleThread();
	}

	@Inject(method = "endBatch(Lnet/minecraft/client/renderer/RenderType;)V", at = @At("HEAD"))
	private void endBatch1(CallbackInfo ci) {
		AssertionUtil.assertNotParticleThread();
	}

	@Inject(method = "endBatchWithType", remap = false, at = @At("HEAD"))
	private void endLastBatch(CallbackInfo ci) {
		AssertionUtil.assertNotParticleThread();
	}
}

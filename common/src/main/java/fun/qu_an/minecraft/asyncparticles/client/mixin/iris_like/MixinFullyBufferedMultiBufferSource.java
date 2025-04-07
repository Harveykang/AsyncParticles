package fun.qu_an.minecraft.asyncparticles.client.mixin.iris_like;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
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
		ThreadUtil.assertNotParticleRendererThread();
	}

	@Inject(method = "endBatch()V", at = @At("HEAD"))
	private void endBatch(CallbackInfo ci) {
		ThreadUtil.assertNotParticleRendererThread();
	}

	@Inject(method = "endBatch(Lnet/minecraft/client/renderer/RenderType;)V", at = @At("HEAD"))
	private void endBatch1(CallbackInfo ci) {
		ThreadUtil.assertNotParticleRendererThread();
	}

	@Inject(method = "endBatchWithType", remap = false, at = @At("HEAD"))
	private void endLastBatch(CallbackInfo ci) {
		ThreadUtil.assertNotParticleRendererThread();
	}

	@WrapWithCondition(method = "getBuffer", at = @At(value = "INVOKE", target = "Lnet/irisshaders/batchedentityrendering/impl/FullyBufferedMultiBufferSource;removeReady()V"))
	private boolean removeReady(FullyBufferedMultiBufferSource instance) {
		return Iris.getPipelineManager().getPipeline().map(p -> p.getPhase() != WorldRenderingPhase.PARTICLES).orElse(true);
	}
}

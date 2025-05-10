package fun.qu_an.minecraft.asyncparticles.client.mixin.iris_like;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FullyBufferedMultiBufferSource.class)
public class MixinFullyBufferedMultiBufferSource {
	@Inject(method = "getBuffer", at = @At("HEAD"))
	private void getBuffer(CallbackInfoReturnable<VertexConsumer> cir) {
		ThreadUtil.assertNotParticleRendererThread();
	}

	@Inject(method = {
		"endBatch()V",
		"endBatch(Lnet/minecraft/client/renderer/RenderType;)V"},
		at = @At("HEAD"))
	private void endBatches(CallbackInfo ci) {
		ThreadUtil.assertNotParticleRendererThread();
	}

	@Inject(method = "endBatchWithType",
		remap = false, at = @At("HEAD"))
	private void endBatches2(CallbackInfo ci) {
		ThreadUtil.assertNotParticleRendererThread();
	}
}

package fun.qu_an.minecraft.asyncparticles.client.mixin.iris_like;

import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.irisshaders.batchedentityrendering.impl.OldFullyBufferedMultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = OldFullyBufferedMultiBufferSource.class, priority = 500)
public class MixinOldFullyBufferedMultiBufferSource {
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
}

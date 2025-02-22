package fun.qu_an.minecraft.asyncparticles.client.mixin.iris;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FullyBufferedMultiBufferSource.class, remap = false)
public class MixinFullyBufferedMultiBufferSource {
	@Inject(method = "getBuffer", at = @At("HEAD"))
	private void getBuffer(RenderType renderType, CallbackInfoReturnable<VertexConsumer> cir) {
		RenderSystem.assertOnRenderThread();
	}

	@Inject(method = "endBatch()V", at = @At("HEAD"))
	private void endBatch(CallbackInfo ci) {
		RenderSystem.assertOnRenderThread();
	}

	@Inject(method = "endBatch(Lnet/minecraft/client/renderer/RenderType;)V", at = @At("HEAD"))
	private void endBatch1(CallbackInfo ci) {
		RenderSystem.assertOnRenderThread();
	}

	@Inject(method = "endBatchWithType", at = @At("HEAD"))
	private void endLastBatch(CallbackInfo ci) {
		RenderSystem.assertOnRenderThread();
	}
}

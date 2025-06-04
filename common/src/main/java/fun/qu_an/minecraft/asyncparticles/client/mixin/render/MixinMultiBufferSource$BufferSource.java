package fun.qu_an.minecraft.asyncparticles.client.mixin.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MultiBufferSource.BufferSource.class, priority = 500)
public abstract class MixinMultiBufferSource$BufferSource {
	@Inject(method = "getBuffer", at = @At("HEAD"))
	private void getBuffer(RenderType renderType, CallbackInfoReturnable<VertexConsumer> cir) {
		ThreadUtil.assertNotParticleRendererThread();
	}

	@Inject(method = {
		"endBatch(Lnet/minecraft/client/renderer/RenderType;)V",
		"endBatch()V",
		"endLastBatch"},
		at = @At("HEAD"))
	private void endBatches(CallbackInfo ci) {
		ThreadUtil.assertNotParticleRendererThread();
	}
}

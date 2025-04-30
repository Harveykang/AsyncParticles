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

@Mixin(value = MultiBufferSource.BufferSource.class, priority = 900)
public abstract class MixinMultiBufferSource$BufferSource
//	implements RenderCall
{
	@Inject(method = "getBuffer", at = @At("HEAD"))
	private void getBuffer(RenderType renderType, CallbackInfoReturnable<VertexConsumer> cir) {
		ThreadUtil.assertNotParticleRendererThread();
	}

	@Inject(method = "endBatch()V", at = @At("HEAD"))
	private void endBatch(CallbackInfo ci) {
		ThreadUtil.assertNotParticleRendererThread();
	}

	@Inject(method = "endLastBatch", at = @At("HEAD"))
	private void endLastBatch(CallbackInfo ci) {
		ThreadUtil.assertNotParticleRendererThread();
	}
}

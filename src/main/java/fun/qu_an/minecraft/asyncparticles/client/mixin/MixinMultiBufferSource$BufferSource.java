package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// FIXME: 可能导致兼容性地狱
@Mixin(MultiBufferSource.BufferSource.class)
public abstract class MixinMultiBufferSource$BufferSource
//	implements RenderCall
{
	@Inject(method = "getBuffer", at = @At("HEAD"))
	private void getBuffer(RenderType renderType, CallbackInfoReturnable<VertexConsumer> cir) {
		RenderSystem.assertOnRenderThread();
	}

	@Inject(method = "endBatch()V", at = @At("HEAD"))
	private void endBatch(CallbackInfo ci) {
		RenderSystem.assertOnRenderThread();
	}

	@Inject(method = "endLastBatch", at = @At("HEAD"))
	private void endLastBatch(CallbackInfo ci) {
		RenderSystem.assertOnRenderThread();
	}
}

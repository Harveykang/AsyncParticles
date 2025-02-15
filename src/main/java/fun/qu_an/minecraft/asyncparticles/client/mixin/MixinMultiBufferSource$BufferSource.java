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
//		if (RenderSystem.isOnRenderThread()) {
//			return;
//		}
//		ThreadLocalBufferBuilder builder = offThreads.computeIfAbsent(renderType,
//			(rt) -> {
//				ThreadLocalBufferBuilder builder1 = new ThreadLocalBufferBuilder(rt.bufferSize(), AsyncRenderer.executor.getParallelism());
//				builder1.begin(renderType.mode(), renderType.format());
//				return builder1;
//			});
//		cir.setReturnValue(builder);
//		last.set(Pair.of(renderType, builder));
//		if (!shouldEndOffThreads) {
//			AsyncRenderer.recordSync(this);
//			shouldEndOffThreads = true;
//		}
	}

	@Inject(method = "endBatch()V", at = @At("HEAD"))
	private void endBatch(CallbackInfo ci) {
		RenderSystem.assertOnRenderThread();
//		if (RenderSystem.isOnRenderThread()) {
//			return;
//		}
//		offThreads.forEach((renderType, bufferBuilder) -> {
//			BufferBuilder builder = bufferBuilder.pollBuffer();
//			if (builder == null) {
//				return;
//			}
//			AsyncRenderer.recordSync(() -> {
//				BufferBuilder.RenderedBuffer renderedBuffer = builder.end();
//				renderType.setupRenderState();
//				BufferUploader.drawWithShader(renderedBuffer);
//				renderType.clearRenderState();
//			});
//		});
//		ci.cancel();
	}

	@Inject(method = "endLastBatch", at = @At("HEAD"))
	private void endLastBatch(CallbackInfo ci) {
		RenderSystem.assertOnRenderThread();
//		if (RenderSystem.isOnRenderThread()) {
//			return;
//		}
//		Pair<RenderType, ThreadLocalBufferBuilder> builder = last.get();
//		if (builder == null) {
//			return;
//		}
//		BufferBuilder builder1 = builder.right().pollBuffer();
//		if (builder1 == null) {
//			return;
//		}
//		RenderType left = builder.left();
//		AsyncRenderer.recordSync(() -> {
//			BufferBuilder.RenderedBuffer renderedBuffer = builder1.end();
//			left.setupRenderState();
//			BufferUploader.drawWithShader(renderedBuffer);
//			left.clearRenderState();
//		});
//		last.remove();
//		ci.cancel();
	}

//	@Override
//	public void execute() {
//		if (!shouldEndOffThreads) {
//			return;
//		}
//		offThreads.forEach((renderType, bufferBuilder) -> {
//			if (renderType.sortOnUpload) {
//				bufferBuilder.setQuadSorting(RenderSystem.getVertexSorting());
//			}
//			renderType.setupRenderState();
//			bufferBuilder.endAll(BufferUploader::drawWithShader);
//			renderType.clearRenderState();
//		});
//		offThreads.clear();
//		shouldEndOffThreads = false;
//	}
}

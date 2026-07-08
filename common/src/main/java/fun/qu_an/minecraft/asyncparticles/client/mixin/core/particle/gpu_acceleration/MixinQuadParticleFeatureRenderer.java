package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.ComputeResult;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticlePipelines;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.QuadParticleFeatureRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(QuadParticleFeatureRenderer.class)
public class MixinQuadParticleFeatureRenderer {
	@Inject(method = "executeGroup", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/GpuDevice;createCommandEncoder()Lcom/mojang/blaze3d/systems/CommandEncoder;"))
	public void executeGroupPreDraw(FeatureFrameContext context,
	                                int groupIndex,
	                                List<QuadParticleFeatureRenderer.Submit> submits,
	                                boolean strictlyOrdered,
	                                CallbackInfo ci,
	                                @Share("result") LocalRef<ComputeResult> resultRef,
	                                @Share("indexBuffer") LocalRef<GpuBuffer> indexBufferRef,
	                                @Share("indexType") LocalRef<IndexType> indexTypeRef) {
		if (!ConfigHelper.isGpuParticles()) {
			return;
		}
		ComputeResult result = GpuParticleBehavior.getInstance().ensureComputeReady();
		if (result == null) {
			return;
		}
		resultRef.set(result);
		int indexCount = 0;
		for (ComputeResult.ParticleSlice slice : result.slices()) {
			if (slice.count() > indexCount) {
				indexCount = slice.count();
			}
		}
		indexCount *= 6;
		RenderSystem.AutoStorageIndexBuffer indexBuffer = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
		indexBufferRef.set(indexBuffer.getBuffer(indexCount));
		indexTypeRef.set(indexBuffer.type());
	}

	@Inject(method = "executeGroup", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/renderer/feature/QuadParticleFeatureRenderer;drawLayers(Lnet/minecraft/client/renderer/StagedVertexBuffer;Ljava/util/Map;Lcom/mojang/blaze3d/systems/RenderPass;Lnet/minecraft/client/renderer/texture/TextureManager;)V"))
	public void executeGroupPostDraw(FeatureFrameContext context,
	                                 int groupIndex,
	                                 List<QuadParticleFeatureRenderer.Submit> submits,
	                                 boolean strictlyOrdered,
	                                 CallbackInfo ci,
	                                 @Local(ordinal = 0) QuadParticleFeatureRenderer.PreparedGroup group,
	                                 @Local(ordinal = 0) RenderPass renderPass,
	                                 @Share("result") LocalRef<ComputeResult> resultRef,
	                                 @Share("indexBuffer") LocalRef<GpuBuffer> indexBufferRef,
	                                 @Share("indexType") LocalRef<IndexType> indexTypeRef) {
		ComputeResult result = resultRef.get();
		if (result == null) {
			return;
		}
		renderPass.setVertexBuffer(0, result.buffer().slice());
		renderPass.setIndexBuffer(indexBufferRef.get(), indexTypeRef.get());
		TextureManager textureManager = context.textureManager();
		ComputeResult.ParticleSlice[] slices = result.slices();
		if (result.isIndirect()) {
			for (int i = 0; i < slices.length; i++) {
				ComputeResult.ParticleSlice slice = slices[i];
				SingleQuadParticle.Layer layer = slice.layer();
				boolean translucent = layer.translucent();
				if (translucent != group.translucent()) {
					continue;
				}
				renderPass.setPipeline(GpuParticlePipelines.of(layer.pipeline(), translucent));
				AbstractTexture texture = textureManager.getTexture(layer.textureAtlasLocation());
				renderPass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
				renderPass.drawIndexedIndirect(result.indirectBuffer().slice((long) i * result.indirectCommandStride(), result.indirectCommandStride()), 1);
			}
			return;
		}
		for (ComputeResult.ParticleSlice slice : slices) {
			SingleQuadParticle.Layer layer = slice.layer();
			boolean translucent = layer.translucent();
			if (translucent != group.translucent()) {
				continue;
			}
			renderPass.setPipeline(GpuParticlePipelines.of(layer.pipeline(), translucent));
			AbstractTexture texture = textureManager.getTexture(layer.textureAtlasLocation());
			renderPass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
			renderPass.drawIndexed(slice.indexCount(), 1, 0, slice.vertexOffset(), 0);
		}
	}
}

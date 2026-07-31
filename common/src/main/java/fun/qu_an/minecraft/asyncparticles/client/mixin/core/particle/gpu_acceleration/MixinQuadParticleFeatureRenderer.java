package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import fun.qu_an.minecraft.asyncparticles.client.config.ComputeExecutionStage;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.ComputeResult;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticlePipelines;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.QuadParticleFeatureRenderer;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(QuadParticleFeatureRenderer.class)
public class MixinQuadParticleFeatureRenderer {
	@Shadow
	private static RenderPipeline getOitPipeline(OitStage stage, SingleQuadParticle.Layer layer) {
		throw new UnsupportedOperationException("Implemented via mixin");
	}

	@Inject(method = "executeGroup", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/RenderPass;pushDebugGroup(Ljava/util/function/Supplier;)V"))
	public void executeGroupPreDraw(FeatureFrameContext context,
	                                @Nullable OitStage stage,
	                                RenderPass renderPass,
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
		if (ConfigHelper.getComputeExecutionStage() == ComputeExecutionStage.PARTICLE_RENDERING) {
			GpuParticleBehavior.getInstance().compute();
		}
		ComputeResult result = GpuParticleBehavior.getInstance().ensureComputeReady();
		if (result == null) {
			return;
		}
		resultRef.set(result);
		RenderSystem.AutoStorageIndexBuffer indexBuffer = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
		indexBufferRef.set(indexBuffer.getBuffer(result.maxIndexCount()));
		indexTypeRef.set(indexBuffer.type());
	}

	@Inject(method = "executeGroup", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/renderer/feature/QuadParticleFeatureRenderer;drawLayers(Lnet/minecraft/client/renderer/StagedVertexBuffer;Lnet/minecraft/client/renderer/feature/QuadParticleFeatureRenderer$PreparedGroup;Lcom/mojang/renderpearl/api/commands/RenderPass;Lnet/minecraft/client/renderer/oit/OitStage;)V"))
	public void executeGroupPostDraw(FeatureFrameContext context,
	                                 @Nullable OitStage stage,
	                                 RenderPass renderPass,
	                                 int groupIndex,
	                                 List<QuadParticleFeatureRenderer.Submit> submits,
	                                 boolean strictlyOrdered,
	                                 CallbackInfo ci,
	                                 @Local(ordinal = 0) QuadParticleFeatureRenderer.PreparedGroup group,
	                                 @Share("result") LocalRef<ComputeResult> resultRef,
	                                 @Share("indexBuffer") LocalRef<GpuBuffer> indexBufferRef,
	                                 @Share("indexType") LocalRef<IndexType> indexTypeRef) {
		ComputeResult result = resultRef.get();
		if (result == null) {
			return;
		}
		TextureManager textureManager = context.textureManager();
		renderPass.setVertexBuffer(0, result.buffer().slice());
		renderPass.setIndexBuffer(indexBufferRef.get(), indexTypeRef.get());
		ComputeResult.ParticleSlice[] slices = result.slices();
		if (result.isIndirect()) {
			for (int i = 0; i < slices.length; i++) {
				ComputeResult.ParticleSlice slice = slices[i];
				SingleQuadParticle.Layer layer = slice.layer();
				boolean translucent = layer.translucent();
				if (translucent != group.translucent()) {
					continue;
				}
				RenderPipeline pipeline = GpuParticlePipelines.of(stage != null ? getOitPipeline(stage, layer) : layer.pipeline(), translucent);
				renderPass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
				AbstractTexture texture = textureManager.getTexture(layer.textureAtlasLocation());
				renderPass.setUniform("Sampler0", texture.getTextureView(), texture.getSampler());
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
			RenderPipeline pipeline = GpuParticlePipelines.of(stage != null ? getOitPipeline(stage, layer) : layer.pipeline(), translucent);
			renderPass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
			AbstractTexture texture = textureManager.getTexture(layer.textureAtlasLocation());
			renderPass.setUniform("Sampler0", texture.getTextureView(), texture.getSampler());
			renderPass.drawIndexed(slice.indexCount(), 1, 0, slice.vertexOffset(), 0);
		}
	}
}

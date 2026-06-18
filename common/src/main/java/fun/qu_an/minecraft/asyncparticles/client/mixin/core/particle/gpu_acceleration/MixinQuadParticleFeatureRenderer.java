package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.PrimitiveTopology;
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
	@Inject(method = "executeGroup", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lcom/mojang/blaze3d/systems/GpuDevice;createCommandEncoder()Lcom/mojang/blaze3d/systems/CommandEncoder;"))
	public void executeGroupPreDraw(FeatureFrameContext context,
	                                int groupIndex,
	                                List<QuadParticleFeatureRenderer.Submit> submits,
	                                boolean strictlyOrdered,
	                                CallbackInfo ci,
	                                @Share("result") LocalRef<ComputeResult> resultRef) {
		if (ConfigHelper.isGpuParticles()) {
			ComputeResult result = GpuParticleBehavior.getInstance().ensureComputeReady();
			resultRef.set(result);
		}
	}

	@Inject(method = "executeGroup", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/renderer/feature/QuadParticleFeatureRenderer;drawLayers(Lnet/minecraft/client/renderer/StagedVertexBuffer;Ljava/util/Map;Lcom/mojang/blaze3d/systems/RenderPass;Lnet/minecraft/client/renderer/texture/TextureManager;)V"))
	public void executeGroupPostDraw(FeatureFrameContext context,
	                                 int groupIndex,
	                                 List<QuadParticleFeatureRenderer.Submit> submits,
	                                 boolean strictlyOrdered,
	                                 CallbackInfo ci,
	                                 @Local(ordinal = 0) QuadParticleFeatureRenderer.PreparedGroup group,
	                                 @Local(ordinal = 0) RenderPass renderPass,
	                                 @Share("result") LocalRef<ComputeResult> resultRef) {
		ComputeResult result = resultRef.get();
		if (result == null) {
			return;
		}
		renderPass.setVertexBuffer(0, result.buffer().slice());
		RenderSystem.AutoStorageIndexBuffer indexBuffer = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
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
				renderPass.setIndexBuffer(indexBuffer.getBuffer(slice.indexCount()), indexBuffer.type());
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
			renderPass.setIndexBuffer(indexBuffer.getBuffer(slice.indexCount()), indexBuffer.type());
			AbstractTexture texture = textureManager.getTexture(layer.textureAtlasLocation());
			renderPass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
			renderPass.drawIndexed(slice.indexCount(), 1, 0, slice.vertexOffset(), 0);
		}
	}
}

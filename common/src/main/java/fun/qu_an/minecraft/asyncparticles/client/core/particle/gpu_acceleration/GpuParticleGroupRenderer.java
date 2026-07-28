package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import fun.qu_an.minecraft.asyncparticles.client.config.ComputeExecutionStage;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class GpuParticleGroupRenderer implements SubmitNodeCollector.ParticleGroupRenderer {
	private static final GpuParticleGroupRenderer instance = new GpuParticleGroupRenderer();
	private final QuadParticleRenderState.PreparedBuffers emptyBuffers =
		new QuadParticleRenderState.PreparedBuffers(0, null, null);
	private final RenderSystem.AutoStorageIndexBuffer quadIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
	private ComputeResult result;
	private GpuBufferSlice dynamicTransforms;
	private GpuBuffer indexBuffer;
	private VertexFormat.IndexType indexType;

	public static GpuParticleGroupRenderer getInstance() {
		return instance;
	}

	@Override
	public QuadParticleRenderState.@Nullable PreparedBuffers prepare(ParticleFeatureRenderer.ParticleBufferCache cache) {
		if (!ConfigHelper.isGpuParticles()) {
			return null;
		}
		if (ConfigHelper.getComputeExecutionStage() == ComputeExecutionStage.PARTICLE_RENDERING) {
			GpuParticleBehavior.getInstance().compute();
		}
		ComputeResult result = GpuParticleBehavior.getInstance().ensureComputeReady();
		if (result == null) {
			return null;
		}

		this.result = result;

		this.dynamicTransforms = RenderSystem.getDynamicUniforms()
			.writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());

		this.indexBuffer = quadIndexBuffer.getBuffer(result.maxIndexCount());
		this.indexType = quadIndexBuffer.type();

		return emptyBuffers;
	}

	@Override
	public void render(QuadParticleRenderState.PreparedBuffers preparedBuffers,
	                   ParticleFeatureRenderer.ParticleBufferCache cache,
	                   RenderPass renderPass,
	                   TextureManager textureManager,
	                   boolean translucent) {
		renderPass.setVertexBuffer(0, result.buffer());
		renderPass.setUniform("DynamicTransforms", dynamicTransforms);
		renderPass.setIndexBuffer(indexBuffer, indexType);
		for (ComputeResult.ParticleSlice slice : result.slices()) {
			SingleQuadParticle.Layer layer = slice.layer();
			if (translucent != layer.translucent()) {
				continue;
			}
			renderPass.setPipeline(GpuParticlePipelines.of(layer.pipeline(), translucent));
			AbstractTexture texture = textureManager.getTexture(layer.textureAtlasLocation());
			renderPass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());

			renderPass.drawIndexed(slice.vertexOffset(), 0, slice.indexCount(), 1);
		}
	}

	public void clear() {
		result = null;
		dynamicTransforms = null;
		indexBuffer = null;
		indexType = null;
	}
}

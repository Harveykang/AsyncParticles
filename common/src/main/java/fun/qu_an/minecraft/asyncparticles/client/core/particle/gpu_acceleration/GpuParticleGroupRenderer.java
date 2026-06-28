package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;

public class GpuParticleGroupRenderer implements SubmitNodeCollector.ParticleGroupRenderer {
	private static final GpuParticleGroupRenderer instance = new GpuParticleGroupRenderer();
	private final QuadParticleRenderState.PreparedBuffers emptyBuffers =
		new QuadParticleRenderState.PreparedBuffers(0, null, null);
	private boolean translucent;
	private ComputeResult result;
	private GpuBufferSlice dynamicTransforms;

	public static GpuParticleGroupRenderer getInstance() {
		return instance;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public QuadParticleRenderState.PreparedBuffers prepare(
		ParticleFeatureRenderer.@NonNull ParticleBufferCache buffer, boolean translucent) {
		if (!ConfigHelper.isGpuParticles()) {
			return null;
		}

		ComputeResult result = GpuParticleBehavior.getInstance().ensureComputeReady();
		if (result == null) {
			return null;
		}

		this.result = result;
		this.translucent = translucent;

		this.dynamicTransforms = RenderSystem.getDynamicUniforms()
			.writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());

		return emptyBuffers;
	}

	@Override
	public void render(QuadParticleRenderState.@NonNull PreparedBuffers buffers,
	                   ParticleFeatureRenderer.@NonNull ParticleBufferCache bufferCache,
	                   @NonNull RenderPass renderPass,
	                   @NonNull TextureManager textureManager) {
		renderPass.setVertexBuffer(0, result.buffer());
		renderPass.setUniform("DynamicTransforms", dynamicTransforms);
		RenderSystem.AutoStorageIndexBuffer indexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		for (ComputeResult.ParticleSlice slice : result.slices()) {
			SingleQuadParticle.Layer layer = slice.layer();
			if (translucent != layer.translucent()) {
				continue;
			}
			renderPass.setIndexBuffer(indexBuffer.getBuffer(slice.indexCount()), indexBuffer.type());
			renderPass.setPipeline(GpuParticlePipelines.of(layer.pipeline(), () -> translucent));
			AbstractTexture texture = textureManager.getTexture(layer.textureAtlasLocation());
			renderPass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());

			renderPass.drawIndexed(slice.vertexOffset(), 0, slice.indexCount(), 1);
		}
	}

	public void clear() {
		result = null;
		dynamicTransforms = null;
	}
}

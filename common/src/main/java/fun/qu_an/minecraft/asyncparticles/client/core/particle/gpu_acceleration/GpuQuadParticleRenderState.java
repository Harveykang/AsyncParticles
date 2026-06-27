package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.ParticleHelper;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;

// add() -> submit() -> prepare() -> render()
public class GpuQuadParticleRenderState extends QuadParticleRenderState {
	// reuse buffers
	private final IParticleRenderer renderer;
	private float partialTick;

	public GpuQuadParticleRenderState() {
		this.renderer = GpuParticleBehavior.getInstance().createRenderer();
	}

	public PreparedBuffers prepare(final ParticleFeatureRenderer.@NonNull ParticleBufferCache cachedBuffer,
	                               final boolean translucent) {
		if (isEmpty()) {
			return null;
		}
		Collection<SingleQuadParticle.Layer> layers = renderer.getComputeLayers();
		Map<SingleQuadParticle.Layer, PreparedLayer> preparedLayers = new Reference2ReferenceOpenHashMap<>(layers.size());
		for (SingleQuadParticle.Layer layer : layers) {
			if (layer.translucent() == translucent){
				preparedLayers.put(layer, null);
			}
		}
		if (preparedLayers.isEmpty()) {
			return null;
		}
		GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
			.writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
		return new PreparedBuffers(translucent ? 1 : 10, dynamicTransforms, preparedLayers);
	}

	@Override
	public void render(
		final @NonNull PreparedBuffers preparedBuffers,
		final ParticleFeatureRenderer.@NonNull ParticleBufferCache bufferCache,
		final @NonNull RenderPass renderPass,
		final @NonNull TextureManager textureManager
	) {
		if (isEmpty()) {
			return;
		}
		renderer.compute(Minecraft.getInstance().gameRenderer.getMainCamera(), partialTick);
		ComputeResult results = renderer.awaitCompute();
		renderPass.setVertexBuffer(0, results.buffer());
		RenderSystem.AutoStorageIndexBuffer indexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		renderPass.setIndexBuffer(indexBuffer.getBuffer(results.totalIndexCount()), indexBuffer.type());
		renderPass.setUniform("DynamicTransforms", preparedBuffers.dynamicTransforms());

		Map<SingleQuadParticle.Layer, PreparedLayer> layers = preparedBuffers.layers();
		for (ComputeResult.ParticleSlice slice : results.slices()) {
			if (slice.count() == 0) {
				continue;
			}
			SingleQuadParticle.Layer layer = slice.layer();
			if (!layers.containsKey(layer)) {
				continue;
			}
			renderPass.setPipeline(GpuParticlePipelines.of(layer.pipeline(), () -> preparedBuffers.indexCount() == 1));
			AbstractTexture texture = textureManager.getTexture(layer.textureAtlasLocation());
			renderPass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());

			renderPass.drawIndexed(slice.vertexOffset(), 0, slice.indexCount(), 1);
			// ((QuadParticleRenderState.PreparedLayer)entry.getValue()).vertexOffset,
			// 0,
			// ((QuadParticleRenderState.PreparedLayer)entry.getValue()).indexCount,
			// 1
		}
	}

	public void tickRenderers(Vec3 camPos, Queue<SingleQuadParticle> particles) {
		Map<SingleQuadParticle.Layer, Queue<SingleQuadParticle>> particleMap = new Reference2ReferenceOpenHashMap<>();
		int size = (int) (particles.size() * 0.5);
		for (SingleQuadParticle sqp : particles) {
			particleMap.computeIfAbsent(sqp.getLayer(), _ -> ParticleHelper.newParticleQueue(size)).add(sqp);
		}
		renderer.tick(camPos, particleMap);
	}

	@Override
	public boolean isEmpty() {
		return renderer.isShouldSkip();
	}

	public void prepareBuffer() {
		renderer.prepareBuffer();
	}

	public void flushBufferAndSwap(Vec3 prevGpuCamPos) {
		renderer.flushBufferAndSwap(prevGpuCamPos);
	}

	public void beginFrame() {
		renderer.beginFrame(partialTick);
	}

	public void append(Vec3 camPos, SingleQuadParticle particle) {
		renderer.append(camPos, particle);
	}

	public void setPartialTick(float partialTick) {
		this.partialTick = partialTick;
	}

	public void resize(int particleLimit) {
		renderer.resize(particleLimit);
	}

	@Override
	public void add(
		final SingleQuadParticle.@NonNull Layer layer,
		final float x,
		final float y,
		final float z,
		final float xRot,
		final float yRot,
		final float zRot,
		final float wRot,
		final float scale,
		final float u0,
		final float u1,
		final float v0,
		final float v1,
		final int color,
		final int lightCoords
	) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void submit(final @NonNull SubmitNodeCollector submitNodeCollector, final @NonNull CameraRenderState camera) {
		if (!isEmpty()) {
			submitNodeCollector.submitParticleGroup(this);
		}
	}

	public void reload() {
		renderer.reload();
	}
}

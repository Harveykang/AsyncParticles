package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.ParticleHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.render.ComputeResult;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.render.GpuParticlePipelines;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.render.IParticleRenderer;
import it.unimi.dsi.fastutil.objects.Reference2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.QuadParticleGroup;
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
import java.util.Set;
import java.util.function.Supplier;

// add() -> submit() -> prepare() -> render()
public class GpuQuadParticleRenderState extends QuadParticleRenderState {
	// reuse buffers
	private final IParticleRenderer renderer;
	private final QuadParticleGroup particleGroup;
	private float partialTick;
	private final Reference2BooleanMap<PreparedBuffers> translucentMap = new Reference2BooleanArrayMap<>(2);

	public GpuQuadParticleRenderState(QuadParticleGroup particleGroup) {
		this.particleGroup = particleGroup;
		this.renderer = GpuParticleBehavior.getInstance().createRenderer();
	}

	@Override
	public boolean isEmpty() {
		return particleGroup.isEmpty();
	}

	@Override
	public void render(
		final @NonNull PreparedBuffers preparedBuffers,
		final ParticleFeatureRenderer.@NonNull ParticleBufferCache bufferCache,
		final @NonNull RenderPass renderPass,
		final @NonNull TextureManager textureManager
	) {
		if (renderer.isShouldSkip()) {
			return;
		}
 		ComputeResult results = renderer.compute(Minecraft.getInstance().gameRenderer.getMainCamera(), partialTick);
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
			renderPass.setPipeline(GpuParticlePipelines.of(layer.pipeline(), () -> translucentMap.getBoolean(preparedBuffers)));
			AbstractTexture texture = textureManager.getTexture(layer.textureAtlasLocation());
			renderPass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());

			renderPass.drawIndexed(slice.vertexOffset(), 0, slice.indexCount(), 1);
			// ((QuadParticleRenderState.PreparedLayer)entry.getValue()).vertexOffset,
			// 0,
			// ((QuadParticleRenderState.PreparedLayer)entry.getValue()).indexCount,
			// 1
		}
	}

	public void mapBuffers(Supplier<Set<SingleQuadParticle.Layer>> potentialLayerSupplier) {
		renderer.mapBuffer(potentialLayerSupplier);
	}

	public void tickRenderers(Vec3 camPos, Queue<SingleQuadParticle> particles) {
		Map<SingleQuadParticle.Layer, Queue<SingleQuadParticle>> particleMap = new Reference2ReferenceOpenHashMap<>();
		int size = (int) (particles.size() * 0.5);
		for (SingleQuadParticle sqp : particles) {
			particleMap.computeIfAbsent(sqp.getLayer(), _ -> ParticleHelper.newParticleQueue(size)).add(sqp);
		}
		renderer.tick(camPos, particleMap);
	}

	public void unmapBuffersAndSwap(Vec3 prevGpuCamPos) {
		renderer.unmapBufferAndSwap(prevGpuCamPos);
	}

	public void beginFrame() {
		renderer.beginFrame();
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

	public PreparedBuffers prepare(final ParticleFeatureRenderer.@NonNull ParticleBufferCache cachedBuffer,
	                               final boolean translucent) {
		if (isEmpty()) {
			return null;
		}
		GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
			.writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
		Collection<SingleQuadParticle.Layer> layers = renderer.getComputeLayers();
		Map<SingleQuadParticle.Layer, PreparedLayer> preparedLayers = new Reference2ReferenceOpenHashMap<>(layers.size());
		for (SingleQuadParticle.Layer layer : layers) {
			if (layer.translucent() == translucent){
				preparedLayers.put(layer, null);
			}
		}
		PreparedBuffers preparedBuffers = new PreparedBuffers(0, dynamicTransforms, preparedLayers);
		this.translucentMap.put(preparedBuffers, translucent);
		return preparedBuffers;
	}

	@Override
	public void submit(final @NonNull SubmitNodeCollector submitNodeCollector, final @NonNull CameraRenderState camera) {
		if (!isEmpty()) {
			submitNodeCollector.submitParticleGroup(this);
		}
		translucentMap.clear();
	}

	public void reload() {
		renderer.reload();
	}
}

package fun.qu_an.minecraft.asyncparticles.client.core;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import fun.qu_an.minecraft.asyncparticles.client.addon.SingleQuadParticleLayerAddition;
import it.unimi.dsi.fastutil.objects.Reference2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.io.Closeable;
import java.util.Collection;
import java.util.Map;

public class AsyncQuadParticleRenderState extends QuadParticleRenderState implements Closeable {
	private MappableRingBuffer buffers;
	private GpuBuffer.MappedView mappedView;
	private long mappedAddress;
	private final Reference2IntMap<SingleQuadParticle.Layer> layerOffsets = new Reference2IntArrayMap<>();

	public int getParticleCount() {
		return particleCount;
	}

	public void beforeExtract(int size, Reference2IntMap<SingleQuadParticle.Layer> countMap) {
		if (countMap.isEmpty()) {
			return;
		}
		if (buffers == null) {
			buffers = new MappableRingBuffer(() -> "AsyncQuadParticleRenderState",
				GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_VERTEX, size);
		} else if (buffers.size() < size) {
			buffers.close();
			buffers = new MappableRingBuffer(() -> "AsyncQuadParticleRenderState",
				GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_VERTEX, size);
		} else {
			buffers.rotate();
		}

		layerOffsets.clear();
		int offset = 0;
		for (Reference2IntMap.Entry<SingleQuadParticle.Layer> layerEntry : countMap.reference2IntEntrySet()) {
			((SingleQuadParticleLayerAddition) (Object) layerEntry.getKey()).setOffsetBytes(offset);
			layerOffsets.put(layerEntry.getKey(), offset);
			offset += layerEntry.getIntValue() * 28 * 4;
		}

		GpuBuffer gpuBuffer = buffers.currentBuffer();
		mappedView = RenderSystem.getDevice().createCommandEncoder()
			.mapBuffer(gpuBuffer, false, true);
		mappedAddress = MemoryUtil.memAddress(mappedView.data());
	}

	@Override
	public void add(@NotNull SingleQuadParticle.Layer layer,
					float f, float g, float h, float i, float j, float k, float l, float m, float n, float o, float p, float q,
					int r, int s) {
		SingleQuadParticleLayerAddition addition = (SingleQuadParticleLayerAddition) (Object) layer;
		int offset = addition.offsetBytes();
		renderRotatedQuad(mappedAddress + offset, f, g, h, i, j, k, l, m, n, o, p, q, r, s);
		addition.setOffsetBytes(offset + 28 * 4);
	}

	protected void renderRotatedQuad(long address, float f, float g, float h, float i, float j, float k, float l, float m, float n, float o, float p, float q, int r, int s) {
		Quaternionf quaternionf = new Quaternionf(i, j, k, l);
		this.renderVertex(address, quaternionf, f, g, h, 1.0F, -1.0F, m, o, q, r, s);
		address += 28L;
		this.renderVertex(address, quaternionf, f, g, h, 1.0F, 1.0F, m, o, p, r, s);
		address += 28L;
		this.renderVertex(address, quaternionf, f, g, h, -1.0F, 1.0F, m, n, p, r, s);
		address += 28L;
		this.renderVertex(address, quaternionf, f, g, h, -1.0F, -1.0F, m, n, q, r, s);
	}

	private void renderVertex(long address, Quaternionf quaternionf, float f, float g, float h, float i, float j, float k, float l, float m, int n, int o) {
		Vector3f vector3f = new Vector3f(i, j, 0.0F).rotate(quaternionf).mul(k).add(f, g, h);
		MemoryUtil.memPutFloat(address, vector3f.x());
		address += 4L;
		MemoryUtil.memPutFloat(address, vector3f.y());
		address += 4L;
		MemoryUtil.memPutFloat(address, vector3f.z());
		address += 4L;
		MemoryUtil.memPutFloat(address, l);
		address += 4L;
		MemoryUtil.memPutFloat(address, m);
		address += 4L;
		MemoryUtil.memPutInt(address, n);
		address += 4L;
		MemoryUtil.memPutInt(address, o);
	}

	@Nullable
	@Override
	public QuadParticleRenderState.PreparedBuffers prepare(ParticleFeatureRenderer.ParticleBufferCache ignored) {
		if (mappedView != null) {
			mappedView.close();
		}
		if (getParticleCount() == 0) {
			return null;
		}

		Map<SingleQuadParticle.Layer, PreparedLayer> map = new Reference2ObjectArrayMap<>();
		for (Reference2IntMap.Entry<SingleQuadParticle.Layer> layerEntry : layerOffsets.reference2IntEntrySet()) {
			int offsetBytes = layerOffsets.getInt(layerEntry.getKey());
			int vertexOffset = offsetBytes / 28;
			int vertexCount = (((SingleQuadParticleLayerAddition) (Object) layerEntry.getKey())
				.offsetBytes() - offsetBytes) / 28;
			int indexCount = vertexCount / 4 * 6;
			map.put(layerEntry.getKey(), new PreparedLayer(vertexOffset, indexCount));
		}

		GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
			.writeTransform(
				RenderSystem.getModelViewMatrix(),
				new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
				new Vector3f(),
				RenderSystem.getTextureMatrix(),
				RenderSystem.getShaderLineWidth()
			);
		return new PreparedBuffers(particleCount * 6, gpuBufferSlice, map);

	}

	@Override
	public void render(
		PreparedBuffers preparedBuffers,
		ParticleFeatureRenderer.ParticleBufferCache ignored,
		RenderPass renderPass,
		TextureManager textureManager,
		boolean bl
	) {
		RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		renderPass.setVertexBuffer(0, buffers.currentBuffer());
		renderPass.setIndexBuffer(autoStorageIndexBuffer.getBuffer(preparedBuffers.indexCount()), autoStorageIndexBuffer.type());
		renderPass.setUniform("DynamicTransforms", preparedBuffers.dynamicTransforms());

		for (Map.Entry<SingleQuadParticle.Layer, PreparedLayer> entry : preparedBuffers.layers().entrySet()) {
			if (bl == entry.getKey().translucent()) {
				renderPass.setPipeline(entry.getKey().pipeline());
				renderPass.bindSampler("Sampler0", textureManager.getTexture(entry.getKey().textureAtlasLocation()).getTextureView());
				renderPass.drawIndexed(
					entry.getValue().vertexOffset(), 0, entry.getValue().indexCount(), 1
				);
			}
		}
	}

	@Override
	public void close() {
		if (buffers != null) {
			buffers.close();
		}
	}
}

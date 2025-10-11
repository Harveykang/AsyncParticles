package fun.qu_an.minecraft.asyncparticles.client.core.particle.async_render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleGroupAddition;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleVertexStorageAddition;
import fun.qu_an.minecraft.asyncparticles.client.addon.SingleQuadParticleLayerAddition;
import fun.qu_an.minecraft.asyncparticles.client.util.MemStackUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.ParticleThreadLocal;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;

// add() -> afterAdd() -> waitFuture() -> submit() -> prepare() -> render()
public class AsyncQuadParticleRenderState extends QuadParticleRenderState implements AsyncParticleRenderState {
	private final ParticleGroup<SingleQuadParticle> group;
	private MeshData meshData;
	private ByteBufferBuilder byteBufferBuilder;
	private final Map<SingleQuadParticle.Layer, PreparedLayer> preparedLayers = new Reference2ObjectArrayMap<>();

	public AsyncQuadParticleRenderState(ParticleGroup<SingleQuadParticle> group) {
		this.group = group;
	}

	@Override
	public void clear() {
		if (((ParticleGroupAddition) group).asyncparticles$getFuture() != null) {
			throw new IllegalStateException("submit() has not been called!");
		}
		freeBuffers();
		super.clear();
	}

	@Override
	public void add(@NotNull SingleQuadParticle.Layer layer,
					float f, float g, float h, float i, float j, float k, float l, float m, float n, float o, float p, float q,
					int r, int s) {
		ParticleLayerAttached attached = ((SingleQuadParticleLayerAddition) (Object) layer).asyncparticles$getAttached();
		attached.add(f, g, h, i, j, k, l, m, n, o, p, q, r, s);
	}

	@Override
	public void afterAdd() {
		particleCount = ParticleLayerAttached.getParticleCount();
		if (particleCount == 0) {
			return;
		}
		particles.putAll(ParticleLayerAttached.getParticles());
		int vertexCount = particleCount * 4;
		byteBufferBuilder = ByteBufferBuilder.exactlySized(vertexCount * DefaultVertexFormat.PARTICLE.getVertexSize());
		long target = byteBufferBuilder.reserve(vertexCount * DefaultVertexFormat.PARTICLE.getVertexSize());
		preparedLayers.clear();

		renderParticles(target);
//		renderParticles2(target);

		ByteBufferBuilder.Result result = byteBufferBuilder.build();
		if (result != null) {
			VertexFormat.IndexType indexType = VertexFormat.IndexType.least(vertexCount);
			meshData = new MeshData(result, new MeshData.DrawState(DefaultVertexFormat.PARTICLE, vertexCount, VertexFormat.Mode.QUADS.indexCount(vertexCount), VertexFormat.Mode.QUADS, indexType));
		}
//		meshData = bufferBuilder.build();
	}

	private void renderParticles(long targetAddress) {
		int j = 0;
		try (MemoryStack memoryStack = MemStackUtil.stackPush()) {
			long offsetPtr = memoryStack.nmalloc(4);
			MemoryUtil.memPutInt(offsetPtr, 0);

//			int[] offset = {0};

//			long ptr = memoryStack.nmalloc(4 + 4 * DefaultVertexFormat.PARTICLE.getVertexSize());
//			long offsetPtr = ptr + 4L * DefaultVertexFormat.PARTICLE.getVertexSize();
//			MemoryUtil.memPutInt(offsetPtr, 0);

			for (Map.Entry<SingleQuadParticle.Layer, Storage> entry : particles.entrySet()) {
				Storage storage = entry.getValue();
				int count = storage.count();
				if (count > 0) {
					storage.forEachParticle((f, g, h, ix, jx, k, l, m, n, o, p, q, r, s) -> {
						int targetOffset = MemoryUtil.memGetInt(offsetPtr);
						this.renderRotatedQuad(targetAddress + targetOffset, f, g, h, ix, jx, k, l, m, n, o, p, q, r, s);
						MemoryUtil.memPutInt(offsetPtr, targetOffset + 112);

//						int i = offset[0];
//						this.renderRotatedQuad(i + targetAddress, f, g, h, ix, jx, k, l, m, n, o, p, q, r, s);
//						offset[0] = i + 112;

//						this.renderRotatedQuad(bufferBuilder, f, g, h, ix, jx, k, l, m, n, o, p, q, r, s);
					});
					preparedLayers.put(entry.getKey(), new PreparedLayer(j, count * 6));
					j += count * 4;
				}
			}
		}
	}

	public void renderParticles2(long targetAddress) {
		Set<Map.Entry<SingleQuadParticle.Layer, Storage>> entries = this.particles.entrySet();
		List<ForkJoinTask<?>> tasks = new ArrayList<>(AsyncRenderBehavior.THREADS);
		int j = 0;
		for (Map.Entry<SingleQuadParticle.Layer, Storage> entry : entries) {
			Storage storage = entry.getValue();
			int count = storage.count();
			if (count > 0) {
				int slice = (count + 1023) / 1024;
				for (int i = 0; i < slice; i++) {
					int start = i * 1024;
					int end = Math.min(start + 1024, count);
					final long finalTarget = targetAddress;
					tasks.add(AsyncRenderBehavior.EXECUTOR.submit(() -> {
						try (MemoryStack memoryStack = MemStackUtil.stackPush()) {
							long offsetPtr = memoryStack.nmalloc(4);
							MemoryUtil.memPutInt(offsetPtr, 0);
							((ParticleVertexStorageAddition) storage)
								.asyncparticles$slice(start, end)
								.forEachParticle((f, g, h, ix, jx, k, l, m, n, o, p, q, r, s) -> {
									int targetOffset = MemoryUtil.memGetInt(offsetPtr);
									this.renderRotatedQuad(finalTarget + targetOffset, f, g, h, ix, jx, k, l, m, n, o, p, q, r, s);
									MemoryUtil.memPutInt(offsetPtr, targetOffset + 112);
								});
						}
					}));
					targetAddress += (end - start) * 112L;
				}
				preparedLayers.put(entry.getKey(), new PreparedLayer(j, count * 6));
				j += count * 4;
			}
//			if (count > 0) {
//				final long finalTarget = targetAddress;
//				tasks[idx++] = AsyncRenderBehavior.EXECUTOR.submit(() -> {
//					try (MemoryStack memoryStack = MemStackUtil.stackPush()) {
//						long offsetPtr = memoryStack.nmalloc(4);
//						MemoryUtil.memPutInt(offsetPtr, 0);
//
//						storage.forEachParticle((f, g, h, ix, jx, k, l, m, n, o, p, q, r, s) -> {
//							int targetOffset = MemoryUtil.memGetInt(offsetPtr);
//							this.renderRotatedQuad(finalTarget + targetOffset, f, g, h, ix, jx, k, l, m, n, o, p, q, r, s);
//							MemoryUtil.memPutInt(offsetPtr, targetOffset + 112);
//						});
//					}
//				});
//
//				preparedLayers.put(entry.getKey(), new PreparedLayer(j, count * 6));
//				j += count * 4;
//				targetAddress += count * 112L;
//			}
		}
		for (ForkJoinTask<?> task : tasks) {
			if (task == null) {
				break;
			}
			task.join();
		}
	}

	private static final ParticleThreadLocal<Quaternionf> QUATERNION_LOCAL = ParticleThreadLocal.withInitial(Quaternionf::new);
	private static final ParticleThreadLocal<Vector3f> VECTOR3F_LOCAL = ParticleThreadLocal.withInitial(Vector3f::new);

	protected void renderRotatedQuad(
		long ptr, float f, float g, float h, float i, float j, float k, float l, float m, float n, float o, float p, float q, int r, int s
	) {
		Quaternionf quaternionf = QUATERNION_LOCAL.get();
		quaternionf.set(i, j, k, l);
		Vector3f vector3f = VECTOR3F_LOCAL.get();
		vector3f.set(1.0F, -1.0F, 0.0F).rotate(quaternionf).mul(m).add(f, g, h);
		this.renderVertex(ptr, vector3f, o, q, r, s);
		ptr += 28L;
		vector3f.set(1.0F, 1.0F, 0.0F).rotate(quaternionf).mul(m).add(f, g, h);
		this.renderVertex(ptr, vector3f, o, p, r, s);
		ptr += 28L;
		vector3f.set(-1.0F, 1.0F, 0.0F).rotate(quaternionf).mul(m).add(f, g, h);
		this.renderVertex(ptr, vector3f, n, p, r, s);
		ptr += 28L;
		vector3f.set(-1.0F, -1.0F, 0.0F).rotate(quaternionf).mul(m).add(f, g, h);
		this.renderVertex(ptr, vector3f, n, q, r, s);
	}

	protected void renderVertex(
		long ptr, Vector3f vector3f, float l, float m, int n, int o
	) {
		// Position
		MemoryUtil.memPutFloat(ptr, vector3f.x);
		ptr += 4L;
		MemoryUtil.memPutFloat(ptr, vector3f.y);
		ptr += 4L;
		MemoryUtil.memPutFloat(ptr, vector3f.z);
		ptr += 4L;
		// UV
		MemoryUtil.memPutFloat(ptr, l);
		ptr += 4L;
		MemoryUtil.memPutFloat(ptr, m);
		ptr += 4L;
		// Color ARGB
		MemoryUtil.memPutByte(ptr, (byte) ARGB.red(n));
		ptr += 1L;
		MemoryUtil.memPutByte(ptr, (byte) ARGB.green(n));
		ptr += 1L;
		MemoryUtil.memPutByte(ptr, (byte) ARGB.blue(n));
		ptr += 1L;
		MemoryUtil.memPutByte(ptr, (byte) ARGB.alpha(n));
		ptr += 1L;
		// Light
		MemoryUtil.memPutShort(ptr, (short) (o & 65535));
		ptr += 2L;
		MemoryUtil.memPutShort(ptr, (short) (o >> 16 & 65535));
	}

	@Nullable
	@Override
	public QuadParticleRenderState.PreparedBuffers prepare(ParticleFeatureRenderer.ParticleBufferCache particleBufferCache) {
		if (((ParticleGroupAddition) group).asyncparticles$getFuture() != null) {
			throw new IllegalStateException("submit() has not been called!");
		}
		if (meshData == null) {
			freeBuffers();
			return null;
		}
		try {
			particleBufferCache.write(meshData.vertexBuffer());
			RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS).getBuffer(meshData.drawState().indexCount());
			GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
				.writeTransform(
					RenderSystem.getModelViewMatrix(),
					new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
					new Vector3f(),
					RenderSystem.getTextureMatrix(),
					RenderSystem.getShaderLineWidth()
				);
			return new PreparedBuffers(meshData.drawState().indexCount(), gpuBufferSlice, preparedLayers);

		} finally {
			freeBuffers();
		}
	}

	private void freeBuffers() {
		if (byteBufferBuilder != null) {
			this.byteBufferBuilder.close();
			this.byteBufferBuilder = null;
			meshData = null; // byteBufferBuilder has released the buffer, so we can set it to null
		}
	}

}

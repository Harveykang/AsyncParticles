package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.GLCaps;
import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.buffer.ParticleVertexBuffer;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.shader.ParticleTransformFeedbackShader;
import fun.qu_an.minecraft.asyncparticles.client.util.MemStackUtil;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Supplier;

import static fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.render.GpuParticlePipelines.multiDrawCount;
import static fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.render.GpuParticlePipelines.multiDrawFirst;

public class NewParticleMultiRenderer implements IParticleRenderer {
	protected static final class LayerBatch {
		final SingleQuadParticle.Layer layer;
		int tickOffset;    // particle index in source buffer
		int tickCount;
		int appendOffset;  // particle index in append region (relative to particleLimit)
		int appendCount;

		LayerBatch(SingleQuadParticle.Layer layer) {
			this.layer = layer;
		}

		int totalCount() {
			return tickCount + appendCount;
		}
	}

	protected final ParticleVertexBuffer[] sources = new ParticleVertexBuffer[2];
	protected ByteBuffer mappedBuffer;
	protected int processingIndex = 0;
	protected int particleLimit;

	// tick: ordered layer batches
	protected final List<LayerBatch>[] layerBatches = new List[]{new ArrayList<>(), new ArrayList<>()};
	protected final int[] tickCount = new int[2];

	// append: temporarily stored particles
	protected final List<SingleQuadParticle>[] pendingAppends = new List[]{new ArrayList<>(), new ArrayList<>()};

	protected final Vec3[] camPositions = {Vec3.ZERO, Vec3.ZERO};

	protected final ParticleVertexBuffer target;
	protected final GpuBuffer targetMoj;
	protected final int tf;

	protected boolean shouldSkip = true;
	protected boolean computed = false;

	private ComputeResult computeResult;

	public NewParticleMultiRenderer() {
		sources[0] = new ParticleVertexBuffer(true);
		GpuParticlePipelines.bindAttr(GpuParticlePipelines.RAW_PARTICLE, sources[0]);
		sources[1] = new ParticleVertexBuffer(true);
		GpuParticlePipelines.bindAttr(GpuParticlePipelines.RAW_PARTICLE, sources[1]);

		target = new ParticleVertexBuffer(-1, true);
		targetMoj = RenderSystem.getDevice().createBuffer(
			() -> "GPU_PARTICLE_BUFFER",
			GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_HINT_CLIENT_STORAGE,
			1L // minimal; resized in resize()
		);
		int handle = ((GlBuffer) targetMoj).handle;
		GL15C.glDeleteBuffers(handle);
		GlBuffer.MEMORY_POOl.free(handle);
		((GlBuffer) targetMoj).handle = target.vbo;
		GlBuffer.MEMORY_POOl.malloc(target.vbo, target.getSize());

		tf = GLCaps.tfSupport.genTransformFeedback();
		if (tf > 0) {
			GLCaps.tfSupport.glBindTransformFeedback(tf);
			GLCaps.tfSupport.glBindTransformFeedbackBuffer(tf, 0, target.vbo);
			GLCaps.tfSupport.glBindTransformFeedback(0);
		}
		resize(AsyncParticlesConfig.DEFAULT_PARTICLE_LIMIT);
	}

	@Override
	public void beginFrame() {
		computed = false;
	}

	@Override
	public void unmapBufferAndSwap(Vec3 prevGpuCamPos) {
		int pi = processingIndex;

		int vertexSize = GpuParticlePipelines.RAW_PARTICLE.getVertexSize();
		int tickParticle = tickCount[pi];

		List<LayerBatch> batches = layerBatches[pi];
		List<SingleQuadParticle> pending = pendingAppends[pi];
		extractAppendParticles(prevGpuCamPos, pending, batches);

		int appendTotal = pending.size();
		if (tickParticle > 0) {
			sources[pi].flush(0, tickParticle * vertexSize);
		}
		if (appendTotal > 0){
			sources[pi].flush(particleLimit * vertexSize, appendTotal * vertexSize);
		}
		if (tickParticle > 0 || appendTotal > 0) {
			shouldSkip = false;
			processingIndex ^= 1;
		}
		if (mappedBuffer != null) {
			mappedBuffer = null;
			sources[pi].unmap();
		}

		// Reset for next frame
		int next = processingIndex;
		this.tickCount[next] = 0;
		this.pendingAppends[next].clear();
		this.layerBatches[next].clear();
	}

	private void extractAppendParticles(Vec3 prevGpuCamPos,
	                                    List<SingleQuadParticle> pending,
	                                    List<LayerBatch> batches) {
		// 2. Write append particles (if any) to the append region
		int appendTotal = pending.size();
		if (appendTotal == 0) {
			return;
		}
		int vertexSize = GpuParticlePipelines.RAW_PARTICLE.getVertexSize();
		final double cx = prevGpuCamPos.x;
		final double cy = prevGpuCamPos.y;
		final double cz = prevGpuCamPos.z;

		int offset;
		if (mappedBuffer != null) {
			offset = particleLimit * vertexSize;
		} else {
			mappedBuffer = sources[processingIndex].mapRange(particleLimit * vertexSize, appendTotal * vertexSize, true);
			offset = 0;
		}
		final long bufferAddress = MemoryUtil.memAddress(mappedBuffer) + offset;

		// Group pending particles by layer, preserving tick order + new layers in append order
		Map<SingleQuadParticle.Layer, List<SingleQuadParticle>> byLayer = new Reference2ReferenceOpenHashMap<>();
		for (LayerBatch batch : batches) {
			byLayer.put(batch.layer, new ArrayList<>()); // ensure tick order
		}
		for (SingleQuadParticle sqp : pending) {
			byLayer.computeIfAbsent(sqp.getLayer(), _ -> new ArrayList<>()).add(sqp);
		}

		int writePos = 0;
		try (MemoryStack stack = MemStackUtil.stackPush()) {
			final long address = stack.nmalloc(vertexSize);
			for (Map.Entry<SingleQuadParticle.Layer, List<SingleQuadParticle>> entry : byLayer.entrySet()) {
				List<SingleQuadParticle> appends = entry.getValue();
				if (appends.isEmpty()) continue;
				SingleQuadParticle.Layer layer = entry.getKey();
				int firstPos = writePos;

				for (SingleQuadParticle sqp : appends) {
//						if (!((GpuParticleAddon) sqp).asyncparticles$shouldRender()) {
//							continue;
//						}
					writeParticle(address, sqp, cx, cy, cz);
					MemoryUtil.memCopy(address, bufferAddress + (long) writePos * vertexSize, vertexSize);
					writePos++;
				}
				int count = writePos - firstPos;
				if (count <= 0) {
					continue;
				}

				// Find or create LayerBatch
				LayerBatch batch = null;
				for (LayerBatch b : batches) {
					if (b.layer == layer) {
						batch = b;
						break;
					}
				}
				if (batch == null) {
					batch = new LayerBatch(layer);
					batches.add(batch);
				}
				batch.appendOffset = firstPos;
				batch.appendCount = count;
			}
		}
	}

	@Override
	public void mapBuffer(Supplier<Set<SingleQuadParticle.Layer>> potentialLayer) {
		if (mappedBuffer != null) {
			throw new IllegalStateException("Mapped buffer is not null");
		}
		int vertexSize = GpuParticlePipelines.RAW_PARTICLE.getVertexSize();
		// Map the full source buffer : tick uses [0, particleLimit), append uses [particleLimit, 2*particleLimit)
		mappedBuffer = sources[processingIndex].map(2 * this.particleLimit * vertexSize);
	}

	@Override
	public boolean isMapped() {
		return mappedBuffer != null;
	}

	@Override
	public boolean isShouldSkip() {
		return shouldSkip;
	}

	@Override
	public void tick(Vec3 cameraPos, Map<SingleQuadParticle.Layer, Queue<SingleQuadParticle>> particles) {
		if (mappedBuffer == null) {
			throw new IllegalStateException("Mapped buffer is null!");
		}
		int pi = processingIndex;
		camPositions[pi] = cameraPos;

		final double cx = cameraPos.x, cy = cameraPos.y, cz = cameraPos.z;
		final long bufferAddress = MemoryUtil.memAddress(mappedBuffer);
		final int vertexSize = GpuParticlePipelines.RAW_PARTICLE.getVertexSize();
		int position = 0;

		List<LayerBatch> batches = layerBatches[pi];
		batches.clear();

		try (MemoryStack stack = MemStackUtil.stackPush()) {
			final long address = stack.nmalloc(vertexSize);
			for (Map.Entry<SingleQuadParticle.Layer, Queue<SingleQuadParticle>> entry : particles.entrySet()) {
				Queue<SingleQuadParticle> queue = entry.getValue();
				if (queue.size() > particleLimit) {
					throw new IllegalStateException("Particle limit exceeded!");
				}
				SingleQuadParticle.Layer layer = entry.getKey();
				int layerStart = position / vertexSize;
				int layerCount = 0;
				for (SingleQuadParticle sqp : queue) {
					if (!((GpuParticleAddon) sqp).asyncparticles$shouldRender()) continue;
					writeParticle(address, sqp, cx, cy, cz);
					MemoryUtil.memCopy(address, bufferAddress + (long) position, vertexSize);
					position += vertexSize;
					layerCount++;
				}
				if (layerCount > 0) {
					LayerBatch batch = new LayerBatch(layer);
					batch.tickOffset = layerStart;
					batch.tickCount = layerCount;
					batches.add(batch);
					tickCount[pi] += layerCount;
				}
			}
		}
	}

	@Override
	public ComputeResult compute(Camera camera, float partialTicks) {
		if (computed) {
			return computeResult;
		}
		int usingIdx = processingIndex ^ 1;
		if (shouldSkip) {
			throw new IllegalStateException("Should skip rendering during this tick!");
		}
		RenderSystem.assertOnRenderThread();

		if (tf > 0) GLCaps.tfSupport.glBindTransformFeedback(tf);
		ParticleTransformFeedbackShader.INSTANCE.use();
		ParticleTransformFeedbackShader.INSTANCE.setup(
			partialTicks,
			camera.leftVector().x(), camera.leftVector().y(), camera.leftVector().z(),
			camera.upVector().x(), camera.upVector().y(), camera.upVector().z(),
			(float) (camPositions[usingIdx].x - camera.position().x),
			(float) (camPositions[usingIdx].y - camera.position().y),
			(float) (camPositions[usingIdx].z - camera.position().z));

		int processedVertexSize = GpuParticlePipelines.PLAIN_PARTICLE.getVertexSize();
		List<LayerBatch> batches = layerBatches[usingIdx];
		int totalParticles = 0;
		for (LayerBatch b : batches) {
			totalParticles += b.totalCount();
		}

		// Resize target to fit actual particle count
		int needSize = totalParticles * 4 * processedVertexSize;
		if (needSize > target.getSize()) {
			resizeTarget(needSize);
		}

		sources[usingIdx].bind();
		if (tf <= 0) {
			GLCaps.tfSupport.glBindTransformFeedbackBuffer(0, 0, target.vbo);
		}
		GL11C.glEnable(GL30C.GL_RASTERIZER_DISCARD);

		int baseCount = 0;
		List<ComputeResult.ParticleSlice> slices = new ArrayList<>();
		for (LayerBatch batch : batches) {
			int tc = batch.tickCount;
			int ac = batch.appendCount;
			int total = tc + ac;
			if (total <= 0) {
				continue;
			}

			GLCaps.tfSupport.glBindTransformFeedbackBufferRange(
				Math.max(tf, 0), 0, target.vbo,
				(long) baseCount * 4 * processedVertexSize,
				(long) total * 4 * processedVertexSize);

			GLCaps.tfSupport.glBeginTransformFeedback(GL11C.GL_POINTS);

			if (ac <= 0) {
				GL11C.glDrawArrays(GL11C.GL_POINTS, batch.tickOffset, tc);
			} else if (tc <= 0) {
				GL11C.glDrawArrays(GL11C.GL_POINTS, particleLimit + batch.appendOffset, ac);
			} else {
				multiDrawFirst[0] = batch.tickOffset;
				multiDrawCount[0] = tc;
				multiDrawFirst[1] = particleLimit + batch.appendOffset;
				multiDrawCount[1] = ac;
				GL14C.glMultiDrawArrays(GL11C.GL_POINTS,
					multiDrawFirst, multiDrawCount);
			}

			GLCaps.tfSupport.glEndTransformFeedback();

			slices.add(new ComputeResult.ParticleSlice(batch.layer, baseCount, total));
			baseCount += total;
		}

		GL11C.glDisable(GL30C.GL_RASTERIZER_DISCARD);
		ParticleVertexBuffer.unbind();

		if (tf > 0) GLCaps.tfSupport.glBindTransformFeedback(0);

		computed = true;
		return computeResult = new ComputeResult(targetMoj, baseCount, slices.toArray(ComputeResult.ParticleSlice[]::new));
	}

	@Override
	public void append(Vec3 cameraPos, SingleQuadParticle sqp) {
		if (!((GpuParticleAddon) sqp).asyncparticles$shouldRender()) {
			return;
		}
		pendingAppends[processingIndex].add(sqp);
		shouldSkip = false;
	}

	@Override
	public void resize(int particleLimit) {
		int rawSize = particleLimit * 2 * GpuParticlePipelines.RAW_PARTICLE.getVertexSize();
		if (rawSize != sources[0].getSize()) sources[0].resize0(rawSize);
		if (rawSize != sources[1].getSize()) sources[1].resize0(rawSize);
		this.particleLimit = particleLimit;

		int proceedSize = 4 * particleLimit * GpuParticlePipelines.PLAIN_PARTICLE.getVertexSize();
		int size = target.getSize();
		if (proceedSize >= size || proceedSize < size * 0.33f) {
			resizeTarget(proceedSize);
		}
	}

	private void resizeTarget(int neededBytes) {
		GlBuffer.MEMORY_POOl.free(target.vbo);
		target.resize0(neededBytes);
		int newSize = target.getSize();
		targetMoj.size = newSize;
		GlBuffer.MEMORY_POOl.malloc(target.vbo, newSize);
	}

	@Override
	public Collection<SingleQuadParticle.Layer> getComputeLayers() {
		List<LayerBatch> batches = layerBatches[processingIndex ^ 1];
		List<SingleQuadParticle.Layer> result = new ArrayList<>(batches.size());
		for (LayerBatch batch : batches) {
			result.add(batch.layer);
		}
		return result;
	}

	public void close() {
		sources[0].delete();
		sources[1].delete();
		targetMoj.close();
		target.delete(true, false);
	}

	private void writeParticle(long address, SingleQuadParticle sqp, double cx, double cy, double cz) {
		float oSize = sqp.getQuadSize(0f), size = sqp.getQuadSize(1f);
		float minU = sqp.getU0(), minV = sqp.getV0(), maxU = sqp.getU1(), maxV = sqp.getV1();
		int light = sqp.getLightCoords(0f);
		long ptr = address;

		MemoryUtil.memPutFloat(ptr, (float) (sqp.xo - cx));
		ptr += 4L;
		MemoryUtil.memPutFloat(ptr, (float) (sqp.yo - cy));
		ptr += 4L;
		MemoryUtil.memPutFloat(ptr, (float) (sqp.zo - cz));
		ptr += 4L;
		MemoryUtil.memPutFloat(ptr, (float) (sqp.x - cx));
		ptr += 4L;
		MemoryUtil.memPutFloat(ptr, (float) (sqp.y - cy));
		ptr += 4L;
		MemoryUtil.memPutFloat(ptr, (float) (sqp.z - cz));
		ptr += 4L;
		MemoryUtil.memPutFloat(ptr, oSize);
		ptr += 4L;
		MemoryUtil.memPutFloat(ptr, size);
		ptr += 4L;
		MemoryUtil.memPutFloat(ptr, minU);
		ptr += 4L;
		MemoryUtil.memPutFloat(ptr, minV);
		ptr += 4L;
		MemoryUtil.memPutFloat(ptr, maxU);
		ptr += 4L;
		MemoryUtil.memPutFloat(ptr, maxV);
		ptr += 4L;
		int color = ARGB.color(
			(int) (sqp.alpha * 255.0f),
			(int) (sqp.bCol * 255.0f),
			(int) (sqp.gCol * 255.0f),
			(int) (sqp.rCol * 255.0f));
		MemoryUtil.memPutInt(ptr, color);
		ptr += 4L;
		MemoryUtil.memPutInt(ptr, color);
		ptr += 4L;
		MemoryUtil.memPutInt(ptr, light);
		ptr += 4L;
		MemoryUtil.memPutFloat(ptr, sqp.oRoll);
		ptr += 4L;
		MemoryUtil.memPutFloat(ptr, sqp.roll);

		((GpuParticleAddon) sqp).asyncparticles$postTick(address);
	}
}


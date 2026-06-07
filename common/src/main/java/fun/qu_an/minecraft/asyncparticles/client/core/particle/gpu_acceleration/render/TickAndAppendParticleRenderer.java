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
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
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

import static fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.render.GpuParticlePipelines.*;

public class TickAndAppendParticleRenderer implements IParticleRenderer {
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
	protected final int[] appendCount = new int[2];

	// append: temporarily stored particles
	protected final List<SingleQuadParticle>[] pendingAppends = new List[]{new ArrayList<>(), new ArrayList<>()};

	protected final Vec3[] camPositions = {Vec3.ZERO, Vec3.ZERO};

	protected final ParticleVertexBuffer target;
	protected final GpuBuffer targetMoj;
	protected final int tf;

	protected boolean shouldSkip = true;
	protected boolean computed = false;

	private ComputeResult computeResult;

	public TickAndAppendParticleRenderer() {
		sources[0] = new ParticleVertexBuffer(true);
		GpuParticlePipelines.bindAttr(RAW_PARTICLE, sources[0]);
		sources[1] = new ParticleVertexBuffer(true);
		GpuParticlePipelines.bindAttr(RAW_PARTICLE, sources[1]);

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
		int vertexSize = RAW_PARTICLE.getVertexSize();

		List<SingleQuadParticle> pending = pendingAppends[pi];
		int tickCount = this.tickCount[pi];
		int appendCount = pending.size();
		if (appendCount == 0) {
			if (tickCount > 0) {
				sources[pi].flush(tickCount * vertexSize);
			}
		} else {
			extractAppendParticles(prevGpuCamPos, pending, layerBatches[pi]);
			if (tickCount > 0) {
				sources[pi].flush((particleLimit + tickCount) * vertexSize);
			} else {
				sources[pi].flush(appendCount * vertexSize);
			}
			this.appendCount[pi] = appendCount;
		}

		if (mappedBuffer != null) {
			shouldSkip = false;
			processingIndex ^= 1;
			sources[pi].unmap();
			mappedBuffer = null;
		} else {
			shouldSkip = true;
		}

		// Reset for next frame
		int next = processingIndex;
		this.tickCount[next] = 0;
		this.appendCount[next] = 0;
//		this.pendingAppends[next].clear(); // Clear in tick() method
//		this.layerBatches[next].clear(); // Clear in tick() method
	}

	private void extractAppendParticles(Vec3 prevGpuCamPos,
	                                    List<SingleQuadParticle> pending,
	                                    List<LayerBatch> batches) {
		int appendCount = pending.size();
		int vertexSize = RAW_PARTICLE.getVertexSize();
		final double cx = prevGpuCamPos.x;
		final double cy = prevGpuCamPos.y;
		final double cz = prevGpuCamPos.z;

		int pi = processingIndex;
		int addressOffset;
		if (mappedBuffer != null) {
			addressOffset = particleLimit * vertexSize;
		} else {
			mappedBuffer = sources[pi].mapRange(particleLimit * vertexSize, appendCount * vertexSize, true);
			addressOffset = 0;
		}
		final long bufferAddress = MemoryUtil.memAddress(mappedBuffer) + addressOffset;

		Map<SingleQuadParticle.Layer, List<SingleQuadParticle>> layerMap = new Reference2ReferenceOpenHashMap<>();
		for (SingleQuadParticle particle : pending) {
			List<SingleQuadParticle> list = layerMap.computeIfAbsent(particle.getLayer(),
				_ -> new ReferenceArrayList<>(pending.size() / 2)); // To reduce re-allocation
			list.add(particle);
		}

		int baseCount = 0;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			long address = stack.nmalloc(vertexSize);
			for (Map.Entry<SingleQuadParticle.Layer, List<SingleQuadParticle>> entry : layerMap.entrySet()) {
				List<SingleQuadParticle> list = entry.getValue();
				LayerBatch batch = null;
				for (LayerBatch b : batches) {
					if (b.layer == entry.getKey()) {
						batch = b;
						break;
					}
				}
				int layerAppend = list.size();
				if (batch == null) {
					batch = new LayerBatch(entry.getKey());
					batch.tickOffset = this.tickCount[pi];
//					batch.tickCount = 0;
					batches.add(batch);
				}
				batch.appendOffset = baseCount;
				batch.appendCount = layerAppend;
				int baseWrite = baseCount * vertexSize;
				for (SingleQuadParticle sqp : list) {
					float oSize = sqp.getQuadSize(0f), size = sqp.getQuadSize(1f);
					float minU = sqp.getU0();
					float minV = sqp.getV0();
					float maxU = sqp.getU1();
					float maxV = sqp.getV1();
					int light = sqp.getLightCoords(0f);

					long ptr = address;
					// oPosition (0-11)
					MemoryUtil.memPutFloat(ptr, (float) (sqp.xo - cx));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (sqp.yo - cy));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (sqp.zo - cz));
					ptr += 4L;

					// Position (12-23)
					MemoryUtil.memPutFloat(ptr, (float) (sqp.x - cx));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (sqp.y - cy));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (sqp.z - cz));
					ptr += 4L;

					// oSize, size (24-31)
					MemoryUtil.memPutFloat(ptr, oSize);
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, size);
					ptr += 4L;

					// UVMinMax (32-47)
					MemoryUtil.memPutFloat(ptr, minU);
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, minV);
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, maxU);
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, maxV);
					ptr += 4L;

					int color = ARGB.color( // ABGR
						(int) (sqp.alpha * 255.0f),
						(int) (sqp.bCol * 255.0f),
						(int) (sqp.gCol * 255.0f),
						(int) (sqp.rCol * 255.0f));
					// oColor (48-51)
					MemoryUtil.memPutInt(ptr, color);
					ptr += 4L;

					// Color (52-55)
					MemoryUtil.memPutInt(ptr, color);
					ptr += 4L;

					// Light (56-59): 2 shorts
					MemoryUtil.memPutInt(ptr, light);
					ptr += 4L;

					// Rolls (60-67)
					MemoryUtil.memPutFloat(ptr, sqp.oRoll);
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, sqp.roll);

					((GpuParticleAddon) sqp).asyncparticles$postTick(address);

					MemoryUtil.memCopy(address, bufferAddress + baseWrite, vertexSize);
					baseWrite += vertexSize;
				}
				baseCount += layerAppend;
			}
		}
	}

	@Override
	public void mapBuffer(Supplier<Set<SingleQuadParticle.Layer>> potentialLayer) {
		if (mappedBuffer != null) {
			throw new IllegalStateException("Mapped buffer is not null");
		}
		int vertexSize = RAW_PARTICLE.getVertexSize();
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
	public void tick(Vec3 camPos, Map<SingleQuadParticle.Layer, Queue<SingleQuadParticle>> particles) {
		if (mappedBuffer == null) {
			throw new IllegalStateException("Mapped buffer is null!");
		}
		int pi = processingIndex;
		camPositions[pi] = camPos;

		final double cx = camPos.x;
		final double cy = camPos.y;
		final double cz = camPos.z;
		final long bufferAddress = MemoryUtil.memAddress(mappedBuffer);
		final int vertexSize = RAW_PARTICLE.getVertexSize();

		List<LayerBatch> batches = layerBatches[pi];
		batches.clear();

		int position = 0;
		try (MemoryStack stack = MemStackUtil.stackPush()) {
			final long address = stack.nmalloc(vertexSize);
			for (Map.Entry<SingleQuadParticle.Layer, Queue<SingleQuadParticle>> entry : particles.entrySet()) {
				Queue<SingleQuadParticle> queue = entry.getValue();
				if (queue.size() > particleLimit) {
					throw new IllegalStateException("Particle limit exceeded!");
				}

				int layerTick = 0;
				for (SingleQuadParticle sqp : queue) {
					if (!((GpuParticleAddon) sqp).asyncparticles$shouldRender()) {
						continue;
					}
					float oSize = sqp.getQuadSize(0f), size = sqp.getQuadSize(1f);
					float minU = sqp.getU0(), minV = sqp.getV0(), maxU = sqp.getU1(), maxV = sqp.getV1();
					int light = sqp.getLightCoords(0f);

					long ptr = address;
					// oPosition (0-11)
					MemoryUtil.memPutFloat(ptr, (float) (sqp.xo - cx));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (sqp.yo - cy));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (sqp.zo - cz));
					ptr += 4L;

					// Position (12-23)
					MemoryUtil.memPutFloat(ptr, (float) (sqp.x - cx));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (sqp.y - cy));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (sqp.z - cz));
					ptr += 4L;

					// oSize, size (24-31)
					MemoryUtil.memPutFloat(ptr, oSize);
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, size);
					ptr += 4L;

					// UVMinMax (32-47)
					MemoryUtil.memPutFloat(ptr, minU);
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, minV);
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, maxU);
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, maxV);
					ptr += 4L;

					int color = ARGB.color( // ABGR
						(int) (sqp.alpha * 255.0f),
						(int) (sqp.bCol * 255.0f),
						(int) (sqp.gCol * 255.0f),
						(int) (sqp.rCol * 255.0f));
					// oColor (48-51)
					MemoryUtil.memPutInt(ptr, color);
					ptr += 4L;

					// Color (52-55)
					MemoryUtil.memPutInt(ptr, color);
					ptr += 4L;

					// Light (56-59): 2 shorts
					MemoryUtil.memPutInt(ptr, light);
					ptr += 4L;

					// Rolls (60-67)
					MemoryUtil.memPutFloat(ptr, sqp.oRoll);
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, sqp.roll);

					((GpuParticleAddon) sqp).asyncparticles$postTick(address);

					MemoryUtil.memCopy(address, bufferAddress + (long) position, vertexSize);
					position += vertexSize;
					layerTick++;
				}

				if (layerTick > 0) {
					LayerBatch batch = new LayerBatch(entry.getKey());
					batch.tickOffset = position / vertexSize - layerTick;
					batch.tickCount = layerTick;
					batches.add(batch);
				}
			}
		}
		this.tickCount[pi] = position / vertexSize;

		// Clear to prepare pending list
		pendingAppends[pi].clear();
	}

	@Override
	public ComputeResult compute(Camera camera, float partialTicks) {
		if (computed) {
			return computeResult;
		}
		if (shouldSkip) {
			throw new IllegalStateException("Should skip rendering during this tick!");
		}
		RenderSystem.assertOnRenderThread();

		if (tf > 0) {
			GLCaps.tfSupport.glBindTransformFeedback(tf);
		}
		int usingIdx = processingIndex ^ 1;
		ParticleTransformFeedbackShader.INSTANCE.use();
		ParticleTransformFeedbackShader.INSTANCE.setup(
			partialTicks,
			camera.leftVector().x(), camera.leftVector().y(), camera.leftVector().z(),
			camera.upVector().x(), camera.upVector().y(), camera.upVector().z(),
			(float) (camPositions[usingIdx].x - camera.position().x),
			(float) (camPositions[usingIdx].y - camera.position().y),
			(float) (camPositions[usingIdx].z - camera.position().z));

		int processedVertexSize = GpuParticlePipelines.PLAIN_PARTICLE.getVertexSize();

		// Resize target to fit actual particle count
		int needSize = (particleLimit + appendCount[usingIdx]) * 4 * processedVertexSize;
		if (needSize > target.getSize()) {
			resizeTarget(needSize);
		}

		sources[usingIdx].bind();
		if (tf <= 0) {
			GLCaps.tfSupport.glBindTransformFeedbackBuffer(0, 0, target.vbo);
		}
		GL11C.glEnable(GL30C.GL_RASTERIZER_DISCARD);

		List<LayerBatch> batches = layerBatches[usingIdx];
		ComputeResult.ParticleSlice[] slices = new ComputeResult.ParticleSlice[batches.size()];
		int baseCount = 0;
		for (int i = 0, batchesSize = batches.size(); i < batchesSize; i++) {
			LayerBatch batch = batches.get(i);
			int tc = batch.tickCount;
			int ac = batch.appendCount;
			int layerCount = tc + ac;
			if (layerCount <= 0) {
				continue;
			}

			GLCaps.tfSupport.glBindTransformFeedbackBufferRange(
				Math.max(tf, 0), 0, target.vbo,
				(long) baseCount * 4 * processedVertexSize,
				(long) layerCount * 4 * processedVertexSize);

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

			slices[i] = new ComputeResult.ParticleSlice(batch.layer, baseCount, layerCount);
			baseCount += layerCount;
		}

		GL11C.glDisable(GL30C.GL_RASTERIZER_DISCARD);
		ParticleVertexBuffer.unbind();

		if (tf > 0) GLCaps.tfSupport.glBindTransformFeedback(0);

		computed = true;
		return computeResult = new ComputeResult(targetMoj, baseCount, slices);
	}

	@Override
	public void append(Vec3 camPos, SingleQuadParticle sqp) {
		if (((GpuParticleAddon) sqp).asyncparticles$shouldRender()) {
			pendingAppends[processingIndex].add(sqp);
		}
	}

	@Override
	public void resize(int particleLimit) {
		int rawSize = 2 * particleLimit * RAW_PARTICLE.getVertexSize();
		if (rawSize != sources[0].getSize()) {
			sources[0].resize0(rawSize);
		}
		if (rawSize != sources[1].getSize()) {
			sources[1].resize0(rawSize);
		}
		this.particleLimit = particleLimit;

		int proceedSize = 4 * particleLimit * GpuParticlePipelines.PLAIN_PARTICLE.getVertexSize();
		int size = target.getSize();
		if (proceedSize >= size * 0.75f || proceedSize < size * 0.33f) {
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
		for (int i = 0; i < 2; i++) {
			sources[i].delete();
			pendingAppends[i].clear();
			layerBatches[i].clear();
			tickCount[i] = 0;
			appendCount[i] = 0;
			camPositions[i] = Vec3.ZERO;
		}
		targetMoj.close();
		target.delete(true, false);
		computed = false;
		shouldSkip = true;
		processingIndex = 0;
		GLCaps.tfSupport.deleteTransformFeedback(tf);
	}
}

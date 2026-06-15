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
import it.unimi.dsi.fastutil.ints.IntArrayList;
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
	}

	protected final ParticleVertexBuffer[] sources = new ParticleVertexBuffer[2];
	protected ByteBuffer mappedBuffer;
	protected int processingIndex = 0;
	protected int particleLimit;

	// tick: ordered layer batches
	@SuppressWarnings("unchecked")
	protected final List<LayerBatch>[] layerBatches = new List[]{new ArrayList<>(), new ArrayList<>()};
	protected final int[] tickCount = new int[2];

	// append: temporarily stored particles
	protected final List<SingleQuadParticle> pendingAppends = new ArrayList<>();
	protected int appendCount;

	protected final Vec3[] camPositions = {Vec3.ZERO, Vec3.ZERO};

	protected final ParticleVertexBuffer target;
	protected final GpuBuffer targetMoj;
	protected final int tf;
	protected int[] multiDrawFirst;
	protected int[] multiDrawCount;

	protected boolean shouldSkip = true;
	protected boolean computed = false;
	protected ComputeResult computeResult;

	public TickAndAppendParticleRenderer(int particleLimit) {
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
			GLCaps.tfSupport.glBindTransformFeedbackBuffer(target.vbo);
			GLCaps.tfSupport.glBindTransformFeedback(0);
		}

		resize(Math.max(particleLimit, AsyncParticlesConfig.MIN_PARTICLE_LIMIT));
	}

	@Override
	public void beginFrame() {
		computed = false;
		computeResult = null;
	}

	@Override
	public void unmapBufferAndSwap(Vec3 prevGpuCamPos) {
		int pi = processingIndex;
		int vertexSize = RAW_PARTICLE.getVertexSize();

		int tickCount = this.tickCount[pi];
		if (tickCount > 0) {
			sources[pi].flush(tickCount * vertexSize); // offsetRelativeToMap = 0
		}
		int appendCount = pendingAppends.size();
		if (appendCount > 0) {
			int offsetRelativeToMap = extractAppendParticles(prevGpuCamPos, pendingAppends, layerBatches[pi]);
			sources[pi].flush(offsetRelativeToMap, appendCount * vertexSize);
			if (offsetRelativeToMap == 0 && camPositions[pi] == Vec3.ZERO) { // first append
				camPositions[pi] = prevGpuCamPos;
			}
			this.appendCount = appendCount;
		}

		if (mappedBuffer != null) {
			shouldSkip = tickCount + appendCount == 0;
			sources[pi].unmap();
			processingIndex ^= 1;
			mappedBuffer = null;
		} else {
			shouldSkip = true;
		}

		// Reset for next frame
		int next = processingIndex;
		this.tickCount[next] = 0;
//		this.pendingAppends.clear(); // Clear in tick() method
//		this.layerBatches[next].clear(); // Clear in tick() method
	}

	protected int extractAppendParticles(Vec3 prevGpuCamPos,
	                                   List<SingleQuadParticle> pending,
	                                   List<LayerBatch> batches) {
		int appendCount = pending.size();
		int vertexSize = RAW_PARTICLE.getVertexSize();
		final double cx = prevGpuCamPos.x;
		final double cy = prevGpuCamPos.y;
		final double cz = prevGpuCamPos.z;

		int pi = processingIndex;
		int offsetRelativeToMap;
		if (mappedBuffer != null) {
			offsetRelativeToMap = particleLimit * vertexSize;
		} else {
			mappedBuffer = sources[pi].mapRange(particleLimit * vertexSize, appendCount * vertexSize, true);
			offsetRelativeToMap = 0;
		}
		final long bufferAddress = MemoryUtil.memAddress(mappedBuffer) + offsetRelativeToMap;

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
				if (list.isEmpty()) {
					throw new AssertionError();
				}
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
		return offsetRelativeToMap;
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
					throw new IllegalStateException("Particle limit exceeded! particle limit: " + particleLimit + ", queue size: " + queue.size());
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
		pendingAppends.clear();
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

		// Resize target to fit actual particle count
		int needSize = 4 * (tickCount[usingIdx] + appendCount) * GpuParticlePipelines.IDENTITY_PARTICLE.getVertexSize();
		if (needSize > target.getSize()) {
			resizeTarget(needSize);
		}

		sources[usingIdx].bind();
		if (tf <= 0) {
			GLCaps.tfSupport.glBindTransformFeedbackBuffer(target.vbo);
		}
		GLCaps.tfSupport.glBindTransformFeedbackBufferRange(tf,
			0,
			target.vbo,
			0L,
			needSize);

		GL11C.glEnable(GL30C.GL_RASTERIZER_DISCARD);
		GLCaps.tfSupport.glBeginTransformFeedback(GL11C.GL_POINTS);

		if (computeResult == null) {
			buildDrawStuffs(layerBatches[usingIdx]);
		}
		GL14C.glMultiDrawArrays(GL11C.GL_POINTS, multiDrawFirst, multiDrawCount);

		GLCaps.tfSupport.glEndTransformFeedback();
		GL11C.glDisable(GL30C.GL_RASTERIZER_DISCARD);
		ParticleVertexBuffer.unbind();

		if (tf > 0) {
			GLCaps.tfSupport.glBindTransformFeedback(0);
		} else {
			GLCaps.tfSupport.glBindTransformFeedbackBuffer(0);
		}

		computed = true;
		return computeResult;
	}

	protected void buildDrawStuffs(List<LayerBatch> layerBatch) {
		int size = layerBatch.size();
		ComputeResult.ParticleSlice[] slices = new ComputeResult.ParticleSlice[size];
		IntArrayList first = new IntArrayList(size * 2);
		IntArrayList count = new IntArrayList(size * 2);
		int baseCount = 0;
		for (int i = 0, layerBatchSize = layerBatch.size(); i < layerBatchSize; i++) {
			LayerBatch batch = layerBatch.get(i);
			int appendCount = batch.appendCount;
			int tickCount = batch.tickCount;
			if (tickCount > 0) {
				first.add(batch.tickOffset);
				count.add(tickCount);
			}
			if (appendCount > 0) {
				first.add(particleLimit + batch.appendOffset);
				count.add(appendCount);
			}
			int batchCount = tickCount + appendCount;
			slices[i] = new ComputeResult.ParticleSlice(batch.layer, baseCount, batchCount);
			baseCount += batchCount;
		}
		multiDrawFirst = first.toIntArray();
		multiDrawCount = count.toIntArray();
		computeResult = new ComputeResult(targetMoj, baseCount, slices);
	}

	@Override
	public void append(Vec3 camPos, SingleQuadParticle sqp) {
		if (((GpuParticleAddon) sqp).asyncparticles$shouldRender()) {
			pendingAppends.add(sqp);
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

		int proceedSize = 4 * particleLimit * GpuParticlePipelines.IDENTITY_PARTICLE.getVertexSize();
		if (proceedSize != target.getSize()) {
			resizeTarget(proceedSize);
		}
	}

	protected void resizeTarget(int neededBytes) {
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

	@Override
	public void close() {
		reload();
		for (int i = 0; i < 2; i++) {
			sources[i].delete();
			layerBatches[i].clear();
		}
		targetMoj.close();
		target.delete(true, false);
		GLCaps.tfSupport.deleteTransformFeedback(tf);
	}

	@Override
	public void reload() {
		for (int i = 0; i < 2; i++) {
			tickCount[i] = 0;
			camPositions[i] = Vec3.ZERO;
			if (sources[i].isMapped()) {
				sources[i].unmap();
			}
		}
		pendingAppends.clear();
		appendCount = 0;
		shouldSkip = true;
		processingIndex = 0;
		computeResult = null;
		mappedBuffer = null;
	}
}

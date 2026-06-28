package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.opengl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlBuffer;
import com.mojang.blaze3d.opengl.GlFence;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.BackendCaps;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.ComputeResult;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticlePipelines;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.IParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.LayerBatch;
import fun.qu_an.minecraft.asyncparticles.client.util.MemStackUtil;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@SuppressWarnings("resource")
public class GlTfParticleRenderer implements IParticleRenderer {
	private static final int SOURCE_SLOT_COUNT = 3;
	private final ParticleVertexBuffer[] sources = new ParticleVertexBuffer[SOURCE_SLOT_COUNT];
	private final GlFence[] fences = new GlFence[SOURCE_SLOT_COUNT];
	private ByteBuffer mappedBuffer;
	private int processingSrcIdx = 0;
	private int renderSrcIdx = 0;
	private int particleLimit;

	// tick: ordered layer batches
	@SuppressWarnings("unchecked")
	private final List<LayerBatch>[] layerBatches = new List[SOURCE_SLOT_COUNT];
	private final int[] tickCount = new int[SOURCE_SLOT_COUNT];

	// append: temporarily stored particles
	private final List<SingleQuadParticle> pendingAppends = new ReferenceArrayList<>();
	private int appendCount;

	private final Vec3[] camPositions = new Vec3[SOURCE_SLOT_COUNT];

	private final ParticleVertexBuffer target;
	private final GpuBuffer targetMoj;
	private final int tf;
	private int[] multiDrawFirst;
	private int[] multiDrawCount;

	private boolean shouldSkip = true;
	private boolean computed = false;
	private ComputeResult computeResult;

	public GlTfParticleRenderer(int particleLimit) {
		for (int i = 0; i < SOURCE_SLOT_COUNT; i++) {
			sources[i] = new ParticleVertexBuffer(GL15C.GL_DYNAMIC_DRAW);
			GpuParticlePipelines.glBindAttr(GpuParticlePipelines.RAW_PARTICLE, sources[i]);
			layerBatches[i] = new ReferenceArrayList<>();
			tickCount[i] = 0;
			camPositions[i] = Vec3.ZERO;
		}

		target = new ParticleVertexBuffer(-1, GL15C.GL_STREAM_COPY);
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

		tf = BackendCaps.glTfSupport.genTransformFeedback();
		if (tf > 0) {
			BackendCaps.glTfSupport.glBindTransformFeedback(tf);
			BackendCaps.glTfSupport.glBindTransformFeedbackBuffer(target.vbo);
			BackendCaps.glTfSupport.glBindTransformFeedback(0);
		}

		resize(Math.max(particleLimit, AsyncParticlesConfig.MIN_PARTICLE_LIMIT));
	}

	@Override
	public void beginFrame(float deltaPartialTick) {
		computed = false;
	}

	@Override
	public void flushBufferAndSwap(Vec3 prevGpuCamPos) {
		int pi = processingSrcIdx;
		int vertexSize = GpuParticlePipelines.RAW_PARTICLE.getVertexSize();

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
		}
		this.appendCount = appendCount;

		// Reset for next frame
		if (isMapped()) {
			shouldSkip = tickCount + appendCount == 0;
			sources[pi].unmap();
			mappedBuffer = null;
		} else {
			shouldSkip = true;
		}
		if (shouldSkip) {
			renderSrcIdx = -1;
		} else {
			renderSrcIdx = pi;
			processingSrcIdx = acquireSourceSlot(pi);
		}

		computeResult = null;
	}

	private int acquireSourceSlot(int idx) {
		if (fences[idx] == null) {
			fences[idx] = new GlFence();
		}
		int newIdx = (idx + 1) % SOURCE_SLOT_COUNT;
		GlFence fence = fences[newIdx];
		if (fence != null) {
			fence.awaitCompletion(Long.MAX_VALUE);
			fence.close();
			fences[newIdx] = null;
		}
		return newIdx;
	}

	private int extractAppendParticles(Vec3 prevGpuCamPos,
	                                   List<SingleQuadParticle> pending,
	                                   List<LayerBatch> batches) {
		int appendCount = pending.size();
		int vertexSize = GpuParticlePipelines.RAW_PARTICLE.getVertexSize();
		final double cx = prevGpuCamPos.x;
		final double cy = prevGpuCamPos.y;
		final double cz = prevGpuCamPos.z;

		int pi = processingSrcIdx;
		int offsetRelativeToMap;
		if (isMapped()) {
			offsetRelativeToMap = particleLimit * vertexSize;
		} else {
			mappedBuffer = sources[pi].mapRange(particleLimit * vertexSize, appendCount * vertexSize, true);
			offsetRelativeToMap = 0;
		}
		final long bufferAddress = MemoryUtil.memAddress(mappedBuffer) + offsetRelativeToMap;

		Map<SingleQuadParticle.Layer, List<SingleQuadParticle>> layerMap = new Reference2ReferenceOpenHashMap<>();
		for (int i = 0, pendingSize = Math.min(particleLimit, pending.size()); i < pendingSize; i++) {
			SingleQuadParticle p = pending.get(i);
			layerMap.computeIfAbsent(p.getLayer(), _ -> new ReferenceArrayList<>(appendCount / 2)).add(p);
		}

		int baseCount = 0;
		int baseWrite = 0;
		try (MemoryStack stack = MemStackUtil.stackPush()) {
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
					batches.add(batch);
				}
				batch.appendOffset = baseCount;
				batch.appendCount = layerAppend;
				for (SingleQuadParticle particle : list) {
					GpuParticleAddon gpuParticle = (GpuParticleAddon) particle;

					long ptr = address;
					// oPosition (0-11)
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getXo() - cx));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getYo() - cy));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getZo() - cz));
					ptr += 4L;

					// Position (12-23)
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getX() - cx));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getY() - cy));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getZ() - cz));
					ptr += 4L;

					// oSize, size (24-31)
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getQuadSize(0f));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getQuadSize(1f));
					ptr += 4L;

					// UVMinMax (32-47)
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getU0());
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getV0());
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getU1());
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getV1());
					ptr += 4L;

					int oColor = gpuParticle.asyncparticles$getOColor();
					// oColor (48-51)
					MemoryUtil.memPutInt(ptr, oColor);
					ptr += 4L;

					// Color (52-55)
					MemoryUtil.memPutInt(ptr, gpuParticle.asyncparticles$getColor(oColor));
					ptr += 4L;

					// Light (56-59): 2 shorts
					MemoryUtil.memPutInt(ptr, gpuParticle.asyncparticles$getLightCoords(0f));
					ptr += 4L;

					// Rolls (60-67)
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getORoll());
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getRoll());

					gpuParticle.asyncparticles$postTick(address);

					MemoryUtil.memCopy(address, bufferAddress + baseWrite, vertexSize);
					baseWrite += vertexSize;
				}
				baseCount += layerAppend;
			}
		}
		return offsetRelativeToMap;
	}

	@Override
	public void prepareBuffer() {
		if (isMapped()) {
			throw new IllegalStateException("Mapped buffer is not null");
		}
		int vertexSize = GpuParticlePipelines.RAW_PARTICLE.getVertexSize();
		// Map the full source buffer : tick uses [0, particleLimit), append uses [particleLimit, 2*particleLimit)
		mappedBuffer = sources[processingSrcIdx].map(2 * this.particleLimit * vertexSize);
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
	public <T extends Collection<SingleQuadParticle>> void tick(Vec3 camPos, Map<SingleQuadParticle.Layer, T> particles) {
		if (!isMapped()) {
			throw new IllegalStateException("Mapped buffer is null!");
		}
		int pi = processingSrcIdx;
		camPositions[pi] = camPos;

		final double cx = camPos.x;
		final double cy = camPos.y;
		final double cz = camPos.z;
		final long bufferAddress = MemoryUtil.memAddress(mappedBuffer);
		final int vertexSize = GpuParticlePipelines.RAW_PARTICLE.getVertexSize();

		List<LayerBatch> batches = layerBatches[pi];
		batches.clear();

		int position = 0;
		int baseCount = 0;
		try (MemoryStack stack = MemStackUtil.stackPush()) {
			final long address = stack.nmalloc(vertexSize);
			for (Map.Entry<SingleQuadParticle.Layer, T> entry : particles.entrySet()) {
				Collection<SingleQuadParticle> collection = entry.getValue();
				if (baseCount + collection.size() > particleLimit) {
					throw new IllegalStateException("Particle limit exceeded! particle limit: " + particleLimit
						+ ", baseCount: " + baseCount
						+ ", collection size: " + collection.size());
				}

				int tickCount = 0;
				for (SingleQuadParticle particle : collection) {
					GpuParticleAddon gpuParticle = (GpuParticleAddon) particle;
					if (!particle.isAlive() || !gpuParticle.asyncparticles$shouldRender()) {
						continue;
					}

					long ptr = address;
					// oPosition (0-11)
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getXo() - cx));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getYo() - cy));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getZo() - cz));
					ptr += 4L;

					// Position (12-23)
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getX() - cx));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getY() - cy));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, (float) (gpuParticle.asyncparticles$getZ() - cz));
					ptr += 4L;

					// oSize, size (24-31)
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getQuadSize(0f));
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getQuadSize(1f));
					ptr += 4L;

					// UVMinMax (32-47)
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getU0());
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getV0());
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getU1());
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getV1());
					ptr += 4L;

					int oColor = gpuParticle.asyncparticles$getOColor();
					// oColor (48-51)
					MemoryUtil.memPutInt(ptr, oColor);
					ptr += 4L;

					// Color (52-55)
					MemoryUtil.memPutInt(ptr, gpuParticle.asyncparticles$getColor(oColor));
					ptr += 4L;

					// Light (56-59): 2 shorts
					MemoryUtil.memPutInt(ptr, gpuParticle.asyncparticles$getLightCoords(0f));
					ptr += 4L;

					// Rolls (60-67)
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getORoll());
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, gpuParticle.asyncparticles$getRoll());

					gpuParticle.asyncparticles$postTick(address);

					MemoryUtil.memCopy(address, bufferAddress + position, vertexSize);
					position += vertexSize;
					tickCount++;
				}

				if (tickCount > 0) {
					LayerBatch batch = new LayerBatch(entry.getKey());
					batch.tickOffset = baseCount;
					batch.tickCount = tickCount;
					batches.add(batch);
				}
				baseCount += tickCount;
			}
		}
		this.tickCount[pi] = position / vertexSize;

		// Clear to prepare pending list
		pendingAppends.clear();
	}

	@Override
	public void compute(Camera camera, float partialTicks) {
		if (computed) {
			return;
		}
		if (shouldSkip) {
			throw new IllegalStateException("Should skip rendering during this tick!");
		}
		RenderSystem.assertOnRenderThread();

		if (tf > 0) {
			BackendCaps.glTfSupport.glBindTransformFeedback(tf);
		}
		int usingIdx = renderSrcIdx;
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
			BackendCaps.glTfSupport.glBindTransformFeedbackBuffer(target.vbo);
		}
		BackendCaps.glTfSupport.glBindTransformFeedbackBufferRange(tf,
			0,
			target.vbo,
			0L,
			needSize);

		GL11C.glEnable(GL30C.GL_RASTERIZER_DISCARD);
		BackendCaps.glTfSupport.glBeginTransformFeedback(GL11C.GL_POINTS);

		if (computeResult == null) {
			buildDrawStuffs(layerBatches[usingIdx]);
		}
		GL14C.glMultiDrawArrays(GL11C.GL_POINTS, multiDrawFirst, multiDrawCount);

		BackendCaps.glTfSupport.glEndTransformFeedback();
		GL11C.glDisable(GL30C.GL_RASTERIZER_DISCARD);
		ParticleVertexBuffer.unbind();

		if (tf > 0) {
			BackendCaps.glTfSupport.glBindTransformFeedback(0);
		} else {
			BackendCaps.glTfSupport.glBindTransformFeedbackBuffer(0);
		}
		ParticleTransformFeedbackShader.unuse();

		computed = true;
	}

	@Override
	public ComputeResult awaitCompute() {
		if (!computed) {
			return null;
		}
		if (shouldSkip) {
			throw new IllegalStateException("Should skip rendering during this tick!");
		}
		return computeResult;
	}

	private void buildDrawStuffs(List<LayerBatch> layerBatch) {
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
		computeResult = ComputeResult.of(targetMoj, baseCount, slices);
	}

	@Override
	public void append(Vec3 camPos, SingleQuadParticle sqp) {
		if (((GpuParticleAddon) sqp).asyncparticles$shouldRender()) {
			pendingAppends.add(sqp);
		}
	}

	@Override
	public void resize(int particleLimit) {
		int rawSize = 2 * particleLimit * GpuParticlePipelines.RAW_PARTICLE.getVertexSize();
		for (int i = 0; i < SOURCE_SLOT_COUNT; i++) {
			if (sources[i].getSize() != rawSize) {
				sources[i].resize0(rawSize);
			}
		}
		this.particleLimit = particleLimit;

		int proceedSize = 4 * particleLimit * GpuParticlePipelines.IDENTITY_PARTICLE.getVertexSize();
		if (proceedSize != target.getSize()) {
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
		List<LayerBatch> batches = layerBatches[renderSrcIdx];
		List<SingleQuadParticle.Layer> result = new ArrayList<>(batches.size());
		for (LayerBatch batch : batches) {
			result.add(batch.layer);
		}
		return result;
	}

	@Override
	public void close() {
		reload();
		for (int i = 0; i < SOURCE_SLOT_COUNT; i++) {
			sources[i].delete();
			layerBatches[i].clear();
		}
		targetMoj.close();
		target.delete(true, false);
		BackendCaps.glTfSupport.deleteTransformFeedback(tf);
	}

	@Override
	public void reload() {
		for (int i = 0; i < SOURCE_SLOT_COUNT; i++) {
			tickCount[i] = 0;
			camPositions[i] = Vec3.ZERO;
			if (sources[i].isMapped()) {
				sources[i].unmap();
			}
			layerBatches[i].clear();
		}
		pendingAppends.clear();
		appendCount = 0;
		shouldSkip = true;
		processingSrcIdx = 0;
		renderSrcIdx = -1;
		computeResult = null;
		mappedBuffer = null;
	}
}

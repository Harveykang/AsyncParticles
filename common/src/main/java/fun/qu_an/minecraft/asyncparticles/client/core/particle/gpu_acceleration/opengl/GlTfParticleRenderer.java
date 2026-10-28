package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.opengl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormat;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.core.backend.Backends;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.*;
import fun.qu_an.minecraft.asyncparticles.client.util.MemStackUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.*;

import static org.lwjgl.opengl.GL11C.glGetInteger;
import static org.lwjgl.opengl.GL20C.GL_CURRENT_PROGRAM;

public class GlTfParticleRenderer<C> implements IParticleRenderer {
	private static final int SOURCE_SLOT_COUNT = 3;
	private final IGlParticleBatchStrategy<C> batchStrategy;
	private final C renderInfo;
	private final ParticleVertexFormat inputFormat;
	private final ParticleVertexBuffer[] sources = new ParticleVertexBuffer[SOURCE_SLOT_COUNT];
	private ByteBuffer mappedBuffer;
	private int processingSrcIdx = 0;
	private int renderSrcIdx = -1;
	private int particleLimit;

	// tick: ordered layer batches
	@SuppressWarnings("unchecked")
	private final List<LayerBatch>[] layerBatches = new List[SOURCE_SLOT_COUNT];
	private final int[] tickCount = new int[SOURCE_SLOT_COUNT];

	// append: temporarily stored particles
	private final List<TextureSheetParticle> pendingAppends = new ReferenceArrayList<>();

	private final Vec3[] camPositions = new Vec3[SOURCE_SLOT_COUNT];

	private final ParticleVertexBuffer target;
	private final int tf;

	private boolean shouldSkip = true;
	private IComputeReuseCondition reuseCondition;
	private ParticleLayout particleLayout;

	public GlTfParticleRenderer(int particleLimit, IGlParticleBatchStrategy<C> batchStrategy) {
		this.batchStrategy = batchStrategy;
		this.inputFormat = batchStrategy.getInputFormat();
		this.renderInfo = batchStrategy.createGlRenderInfo();
		for (int i = 0; i < SOURCE_SLOT_COUNT; i++) {
			sources[i] = new ParticleVertexBuffer(GL15C.GL_DYNAMIC_DRAW);
			sources[i].bind();
			inputFormat.setupBufferState(); // attributes
			layerBatches[i] = new ReferenceArrayList<>();
			tickCount[i] = 0;
			camPositions[i] = Vec3.ZERO;
		}

		target = new ParticleVertexBuffer(GL15C.GL_STREAM_COPY);
		target.bind();
		ParticleVertexFormats.IDENTITY_PARTICLE.setupBufferState(); // attributes
		ParticleVertexBuffer.unbind();

		tf = Backends.glTf.genTransformFeedback();

		resize(particleLimit);
	}

	@Override
	public void beginFrame(float deltaPartialTick) {
		reuseCondition = null;
	}

	@Override
	public void flushBufferAndSwap(Vec3 prevGpuCamPos) {
		int pi = processingSrcIdx;
		int vertexSize = inputFormat.getVertexSize();

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

		particleLayout = null;
	}

	private int acquireSourceSlot(int idx) {
		return (idx + 1) % SOURCE_SLOT_COUNT;
	}

	private int extractAppendParticles(Vec3 prevGpuCamPos,
	                                   List<TextureSheetParticle> pending,
	                                   List<LayerBatch> batches) {
		int appendCount = pending.size();
		int vertexSize = inputFormat.getVertexSize();

		int pi = processingSrcIdx;
		int offsetRelativeToMap;
		if (isMapped()) {
			offsetRelativeToMap = particleLimit * vertexSize;
		} else {
			mappedBuffer = sources[pi].mapRange(particleLimit * vertexSize, appendCount * vertexSize, true);
			offsetRelativeToMap = 0;
		}
		final long bufferAddress = MemoryUtil.memAddress(mappedBuffer) + offsetRelativeToMap;

		Map<IParticleBatchKey, List<TextureSheetParticle>> layerMap = new Object2ReferenceOpenHashMap<>(batches.size());
		for (int i = 0, pendingSize = Math.min(particleLimit, pending.size()); i < pendingSize; i++) {
			TextureSheetParticle p = pending.get(i);
			layerMap.computeIfAbsent(batchStrategy.keyFor(p), ignored -> new ReferenceArrayList<>()).add(p);
		}

		int baseCount = 0;
		int baseWrite = 0;
		try (MemoryStack stack = MemStackUtil.stackPush()) {
			long address = stack.nmalloc(vertexSize);
			for (Map.Entry<IParticleBatchKey, List<TextureSheetParticle>> entry : layerMap.entrySet()) {
				IParticleBatchKey key = entry.getKey();
				List<TextureSheetParticle> list = entry.getValue();
				LayerBatch batch = null;
				for (LayerBatch b : batches) {
					if (b.key.equals(key)) {
						batch = b;
						break;
					}
				}
				int layerAppend = list.size();
				if (batch == null) {
					batch = batchStrategy.createBatch(key, prevGpuCamPos);
					batch.tickOffset = this.tickCount[pi];
					batches.add(batch);
				}
				batch.appendOffset = baseCount;
				batch.appendCount = layerAppend;
				batchStrategy.extractLayer(renderInfo, list, batch, address, bufferAddress + baseWrite, prevGpuCamPos, true);
				baseWrite += layerAppend * vertexSize;
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
		int vertexSize = inputFormat.getVertexSize();
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
	public <T extends Collection<TextureSheetParticle>> void tick(Vec3 camPos, Map<ParticleRenderType, T> particles) {
		if (!isMapped()) {
			throw new IllegalStateException("Mapped buffer is null!");
		}
		int pi = processingSrcIdx;
		camPositions[pi] = camPos;

		final long bufferAddress = MemoryUtil.memAddress(mappedBuffer);
		final int vertexSize = inputFormat.getVertexSize();

		List<LayerBatch> batches = layerBatches[pi];
		batches.clear();

		int position = 0;
		int baseCount = 0;
		try (MemoryStack stack = MemStackUtil.stackPush()) {
			final long address = stack.nmalloc(vertexSize);
			for (Map.Entry<IParticleBatchKey, Collection<TextureSheetParticle>> entry : batchStrategy.groupParticles(particles).entrySet()) {
				Collection<TextureSheetParticle> collection = entry.getValue();
				if (baseCount + collection.size() > particleLimit) {
					throw new IllegalStateException("Particle limit exceeded! particle limit: " + particleLimit
						+ ", baseCount: " + baseCount
						+ ", collection size: " + collection.size());
				}

				LayerBatch batch = batchStrategy.createBatch(entry.getKey(), camPos);
				int tickCount = batchStrategy.extractLayer(renderInfo, collection, batch, address, bufferAddress + position, camPos, false);
				position += tickCount * vertexSize;

				if (tickCount > 0) {
					batch.tickOffset = baseCount;
					batch.tickCount = tickCount;
					batches.add(batch);
				}
				baseCount += tickCount;
			}
		}
		this.tickCount[pi] = baseCount;

		// Clear to prepare pending list
		pendingAppends.clear();
	}

	@Override
	public void append(Vec3 camPos, TextureSheetParticle particle) {
		if (particle.isAlive() && IParticleRenderer.shouldRenderParticle(((GpuParticleAddon) particle), camPos)) {
			pendingAppends.add(particle);
		}
	}

	@Override
	public void compute(Camera camera, float partialTicks) {
		if (reuseCondition != null && reuseCondition.matches(camera, partialTicks)) {
			return;
		}
		if (shouldSkip) {
			throw new IllegalStateException("Should skip rendering during this tick!");
		}
		ThreadUtil.assertOnMainThread();
		int usingIdx = renderSrcIdx;
		if (usingIdx < 0) {
			throw new IllegalStateException("No published source slot for GPU particle rendering");
		}
		if (particleLayout == null) {
			particleLayout = batchStrategy.buildGlLayout(renderInfo, layerBatches[usingIdx], particleLimit);
		}
		IComputeReuseCondition condition = batchStrategy.prepareView(renderInfo, layerBatches[usingIdx], camera, partialTicks);
		if (condition == null) {
			reuseCondition = null;
			return;
		}

		BufferUploader.invalidate();
		int prevProgram = glGetInteger(GL_CURRENT_PROGRAM);
		sources[usingIdx].bind();
		if (tf != 0) {
			Backends.glTf.glBindTransformFeedback(tf);
		}
		batchStrategy.prepareGlCompute(renderInfo, camera, partialTicks, camPositions[usingIdx], particleLayout.totalCount(), tf, target.vbo);

		GL11C.glEnable(GL30C.GL_RASTERIZER_DISCARD);
		Backends.glTf.glBeginTransformFeedback(GL11C.GL_POINTS);
		batchStrategy.drawGlCompute(renderInfo, camera, partialTicks, camPositions[usingIdx], layerBatches[usingIdx], particleLimit);
		Backends.glTf.glEndTransformFeedback();
		GL11C.glDisable(GL30C.GL_RASTERIZER_DISCARD);
		ParticleVertexBuffer.unbind();

		if (tf != 0) {
			Backends.glTf.glBindTransformFeedback(0);
		}
		GL30C.glUseProgram(prevProgram);
		reuseCondition = condition;
	}

	@Override
	public ParticleLayout awaitCompute() {
		if (shouldSkip || reuseCondition == null) {
			return null;
		}
		return particleLayout;
	}

	@Override
	public void render(ParticleRenderType renderType) {
		if (particleLayout == null || reuseCondition == null) {
			return;
		}
		RenderSystem.assertOnRenderThread();
		ParticleLayout.ParticleSlice particleSlice = particleLayout.slices().get(batchStrategy.keyFor(renderType));
		if (particleSlice == null) {
			return;
		}
		BufferUploader.invalidate();

		target.bind();

		ShaderInstance shader = RenderSystem.getShader();
		Objects.requireNonNull(shader);

		IParticleRenderer.prepareShader(shader);

		shader.apply();

		RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		int indexCount = particleSlice.indexCount();
		autoStorageIndexBuffer.bind(indexCount);
		int vertexOffset = particleSlice.vertexOffset();
		int vertexCount = particleSlice.vertexCount();
		GL32C.glDrawRangeElementsBaseVertex(
			VertexFormat.Mode.QUADS.asGLMode,
			0,
			vertexCount - 1,
			particleSlice.indexCount(),
			autoStorageIndexBuffer.type().asGLType,
			0L,
			vertexOffset
		);
		shader.clear();

		ParticleVertexBuffer.unbind();
	}

	@Override
	public void resize(int particleLimit) {
		int oldRawSize = 2 * this.particleLimit * inputFormat.getVertexSize();
		this.particleLimit = particleLimit;
		int rawSize = 2 * particleLimit * inputFormat.getVertexSize();
		for (int i = 0; i < SOURCE_SLOT_COUNT; i++) {
			ParticleVertexBuffer source = sources[i];
			if (source.isMapped()) {
				source.unmap();
			}
			if (source.getSize() == rawSize) {
				continue;
			}
			if (i == renderSrcIdx) {
				// The slot being rendered this tick still feeds the next compute();
				// migrate its contents through a staging buffer
				long halfBytes = (long) Math.min(oldRawSize, rawSize) / 2;
				int staging = Backends.gl.glGenBuffers();
				Backends.gl.glBufferData(staging, oldRawSize, GL15C.GL_STREAM_COPY);
				try {
					Backends.gl.glCopyBufferSubData(source.vbo, staging, 0L, 0L, oldRawSize);
					source.resize(rawSize);
					Backends.gl.glCopyBufferSubData(staging, source.vbo, 0L, 0L, halfBytes);
					Backends.gl.glCopyBufferSubData(staging, source.vbo, (long) oldRawSize / 2, (long) rawSize / 2, halfBytes);
				} finally {
					GL15C.glDeleteBuffers(staging);
				}
			} else {
				source.resize(rawSize);
			}
		}
		this.mappedBuffer = null;

		int proceedSize = 2 * 4 * particleLimit * ParticleVertexFormats.IDENTITY_PARTICLE.getVertexSize();
		if (proceedSize != target.getSize()) {
			target.resize(proceedSize);
		}

		particleLayout = null;
		reuseCondition = null;
		if (tf != 0) {
			// Attach the whole target buffer once. Transform feedback object state is persistent,
			// so there is no need to re-bind a buffer range every frame.
			Backends.glTf.glBindTransformFeedbackBufferBase(tf, 0, target.vbo);
		}
	}

	@Override
	public Collection<ParticleRenderType> getComputeLayers() {
		List<LayerBatch> batches = renderSrcIdx >= 0 ? layerBatches[renderSrcIdx] : List.of();
		Set<ParticleRenderType> result = new ObjectLinkedOpenHashSet<>();
		for (LayerBatch batch : batches) {
			result.add(batch.key.layer());
		}
		return result;
	}

	@Override
	public void close() {
		reset();
		for (int i = 0; i < SOURCE_SLOT_COUNT; i++) {
			sources[i].delete();
		}
		target.delete();
		Backends.glTf.deleteTransformFeedback(tf);
	}

	@Override
	public void reset() {
		for (int i = 0; i < SOURCE_SLOT_COUNT; i++) {
			tickCount[i] = 0;
			camPositions[i] = Vec3.ZERO;
			layerBatches[i].clear();
			if (sources[i].isMapped()) {
				sources[i].unmap();
			}
		}
		pendingAppends.clear();
		shouldSkip = true;
		processingSrcIdx = 0;
		renderSrcIdx = -1;
		particleLayout = null;
		reuseCondition = null;
		mappedBuffer = null;
	}
}

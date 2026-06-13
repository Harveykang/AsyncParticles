package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.GLCaps;
import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.buffer.ParticleVertexBuffer;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.shader.ParticleTransformFeedbackShader;
import fun.qu_an.minecraft.asyncparticles.client.util.MemStackUtil;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.function.Supplier;

import static fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.render.GpuParticlePipelines.RAW_PARTICLE;

public class ParticleRenderer implements IParticleRenderer {
	protected final ParticleVertexBuffer[] sources = new ParticleVertexBuffer[2];
	protected final ParticleVertexBuffer target;
	protected final GpuBuffer targetMoj;
	protected ByteBuffer mappedBuffer;
	protected final Vec3[] camPositions = {Vec3.ZERO, Vec3.ZERO};
	protected final int[] particleCount = new int[4];
	protected final ComputeData[] computeData = {new ComputeData(), new ComputeData()};
	protected int particleLimit;
	protected int processingIndex = 0;
	protected final int tf;
	protected boolean shouldSkip = true;
	protected boolean computed = false;

	public ParticleRenderer() {
//		sources[0].bind();
//		ParticleVertexFormats.RAW_PARTICLE.setupBufferState(); // attributes
//		sources[1].bind();
//		ParticleVertexFormats.RAW_PARTICLE.setupBufferState(); // attributes
//		target.bind();
//		ParticleVertexFormats.PROCESSED_PARTICLE.setupBufferState(); // attributes
		// ((GlDevice) RenderSystem.getDevice()).vertexArrayCache().bindVertexArray(pipeline.info().getVertexFormat(), (GlBuffer)renderPass.vertexBuffers[0]);
		// TODO reuse the vao
		// see com.mojang.blaze3d.opengl.VertexArrayCache.Separate.bindVertexArray

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

		resize(AsyncParticlesConfig.DEFAULT_PARTICLE_LIMIT); // this.particleLimit = particleLimit;

		tf = GLCaps.tfSupport.genTransformFeedback();
		if (tf > 0) {
			GLCaps.tfSupport.glBindTransformFeedback(tf);
			GLCaps.tfSupport.glBindTransformFeedbackBufferBase(tf, 0, target.vbo);
			GLCaps.tfSupport.glBindTransformFeedback(0);
		}
	}

	@Override
	public void beginFrame() {
		computed = false;
	}

	@Override
	public void unmapBufferAndSwap(Vec3 prevGpuCamPos) {
		if (mappedBuffer != null) {
			unmapBuffer();
			shouldSkip = particleCount[processingIndex] == 0;
			processingIndex ^= 1;
		} else {
			shouldSkip = true;
		}
		this.particleCount[processingIndex] = 0;
		this.particleCount[2 | processingIndex] = 0;
	}

	private void unmapBuffer() {
		RenderSystem.assertOnRenderThread();
		// correct the particle count
		particleCount[2 | processingIndex] = Math.max(0, particleCount[processingIndex] - particleLimit);
		particleCount[processingIndex] = Math.min(particleLimit, this.particleCount[processingIndex]);
		if (mappedBuffer == null) {
			throw new IllegalStateException("Mapped buffer is null!");
		}
		sources[processingIndex].unmap(0, particleCount[processingIndex] * RAW_PARTICLE.getVertexSize());
//		debugPrintBuffer(mappedBuffer);
		mappedBuffer = null;
	}

	@Override
	public void mapBuffer(Supplier<Set<SingleQuadParticle.Layer>> potentialLayer) {
		if (mappedBuffer != null) {
			throw new IllegalStateException("Mapped buffer is not null");
		}
		ParticleVertexBuffer source = sources[processingIndex];
		mappedBuffer = source.map(this.particleLimit * RAW_PARTICLE.getVertexSize());
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
		camPositions[processingIndex] = cameraPos;
		final double cx = cameraPos.x;
		final double cy = cameraPos.y;
		final double cz = cameraPos.z;

		final long bufferAddress = MemoryUtil.memAddress(mappedBuffer);

		int position = 0;
		final int vertexSize = RAW_PARTICLE.getVertexSize();
		ComputeData computeData = this.computeData[processingIndex];
		computeData.clear();
		try (final MemoryStack stack = MemStackUtil.stackPush()) {
			final long address = stack.nmalloc(vertexSize);
			for (Map.Entry<SingleQuadParticle.Layer, Queue<SingleQuadParticle>> entry : particles.entrySet()) {
				Queue<SingleQuadParticle> queue = entry.getValue();
				if (queue.size() > particleLimit) {
					throw new IllegalStateException("Particle limit exceeded!");
				}
				int layerCount = 0;
				for (SingleQuadParticle sqp : queue) {
					if (!((GpuParticleAddon) sqp).asyncparticles$shouldRender()) {
						continue;
					}
//				if (shouldCull(particleCullingMode, frustum, sqp)) {
//					continue;
//				}
					float oSize = sqp.getQuadSize(0f);
					float size = sqp.getQuadSize(1f);
					float minU = sqp.getU0();
					float minV = sqp.getV0();
					float maxU = sqp.getU1();
					float maxV = sqp.getV1();
					int light = sqp.getLightCoords(0f); // TODO lerp

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

					// Light (56-59): 两个 short
					MemoryUtil.memPutInt(ptr, light);
					ptr += 4L;

					// Rolls (60-67)
					MemoryUtil.memPutFloat(ptr, sqp.oRoll);
					ptr += 4L;
					MemoryUtil.memPutFloat(ptr, sqp.roll);

					((GpuParticleAddon) sqp).asyncparticles$postTick(address);

					MemoryUtil.memCopy(address, bufferAddress + (long) position, vertexSize);
					position += vertexSize;
					layerCount++;
				}
				computeData.set(entry.getKey(), layerCount);
			}
		}
		int particleCount = position / vertexSize;
		this.particleCount[processingIndex] = particleCount;
	}

	@Override
	public ComputeResult compute(Camera camera, float partialTicks) {
		if (computed) {
			return computeData[processingIndex ^ 1].getResult(targetMoj);
		}
		if (shouldSkip) {
			throw new IllegalStateException("Should skip rendering during this tick!");
		}
		RenderSystem.assertOnRenderThread();

		if (tf > 0) {
			GLCaps.tfSupport.glBindTransformFeedback(tf);
		}
		Vector3fc camLeftVector = camera.leftVector();
		Vector3fc camUpVector = camera.upVector();
		Vec3 camPos = camera.position();
		Vec3 lastCamPos = camPositions[processingIndex ^ 1];
		ParticleTransformFeedbackShader.INSTANCE.use();
//		if (GLCaps.supportsUniformBufferObject) {
//			TFUniformBuffer.TF_UNIFORM_BUFFER.bindUBO(0);
//		} else {
		ParticleTransformFeedbackShader.INSTANCE.setup(
			partialTicks,
			camLeftVector.x(),
			camLeftVector.y(),
			camLeftVector.z(),
			camUpVector.x(),
			camUpVector.y(),
			camUpVector.z(),
			(float) (lastCamPos.x - camPos.x),
			(float) (lastCamPos.y - camPos.y),
			(float) (lastCamPos.z - camPos.z));
//		}

//		if (maxParticleCount < particleCount[processingIndex ^ 1]) {
//			int nextCount = Math.min(HashCommon.nextPowerOfTwo(particleCount[processingIndex ^ 1]), this.particleLimit);
//			target.resize(nextCount * 4 * GpuParticlePipelines.PLAIN_PARTICLE.getVertexSize());
//			targetMoj.size = target.getSize();
//			maxParticleCount = nextCount;
//		}
		sources[processingIndex ^ 1].bind();

		if (tf <= 0) {
			GLCaps.tfSupport.glBindTransformFeedbackBufferBase(0, 0, target.vbo);
		}

		GL11C.glEnable(GL30C.GL_RASTERIZER_DISCARD);

		GLCaps.tfSupport.glBeginTransformFeedback(GL11C.GL_POINTS);
		int overflow = particleCount[2 | processingIndex ^ 1];
		if (overflow <= 0) {
			GL11C.glDrawArrays(GL11C.GL_POINTS, 0, particleCount[processingIndex ^ 1]);
		} else {
			GpuParticlePipelines.multiDrawFirst[0] = overflow;
			GpuParticlePipelines.multiDrawCount[0] = this.particleLimit - overflow;
			GpuParticlePipelines.multiDrawCount[1] = overflow;
			GL14C.glMultiDrawArrays(GL11C.GL_POINTS,
				GpuParticlePipelines.multiDrawFirst,
				GpuParticlePipelines.multiDrawCount);
		}
		GLCaps.tfSupport.glEndTransformFeedback();

		GL11C.glDisable(GL30C.GL_RASTERIZER_DISCARD);

		ParticleVertexBuffer.unbind();

		if (tf > 0) {
			GLCaps.tfSupport.glBindTransformFeedback(0);
		}

//		readBackProcessedParticles(particleCount[processingIndex ^ 1]);

		computed = true;
		return computeData[processingIndex ^ 1].getResult(targetMoj);
	}

	private void readBackProcessedParticles(int particleCount) {
		if (particleCount <= 0) {
			return;
		}

		int vertexCount = particleCount * 4;
		int vertexSize = GpuParticlePipelines.IDENTITY_PARTICLE.getVertexSize();
		int bufferSize = vertexCount * vertexSize;
		System.out.println("=== DEBUG: Buffer Layout ===");
		System.out.println("Vertex count: " + vertexCount);
		System.out.println("Vertex size: " + vertexSize + " bytes");
		System.out.println("Buffer size: " + bufferSize + " bytes");

		List<VertexFormatElement> elements = GpuParticlePipelines.IDENTITY_PARTICLE.getElements();
		for (int i = 0; i < elements.size(); i++) {
			VertexFormatElement elem = elements.get(i);
			int offset = GpuParticlePipelines.IDENTITY_PARTICLE.getOffset(elem);
			System.out.println("Element " + i + ": "
				+ " indexType=" + elem.type()
				+ " count=" + elem.count()
				+ " byteSize=" + elem.byteSize()
				+ " baseVertex=" + offset);
		}
		System.out.println("===========================");

		ByteBuffer tempBuffer = ByteBuffer.allocateDirect(bufferSize);
		tempBuffer.order(ByteOrder.LITTLE_ENDIAN);

		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, target.vbo);
		GL15C.glGetBufferSubData(GL15C.GL_ARRAY_BUFFER, 0, tempBuffer);
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);

		tempBuffer.rewind();
		System.out.println("=== First 64 bytes (hex) ===");
		byte[] bytes = new byte[64];
		tempBuffer.get(bytes);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			sb.append(String.format("%02X ", bytes[i]));
			if ((i + 1) % 16 == 0) {
				sb.append("\n");
			}
		}
		System.out.println(sb);

		tempBuffer.rewind();

		parseParticleData(tempBuffer, vertexCount, vertexSize);
	}

	private void parseParticleData(ByteBuffer buffer, int vertexCount, int vertexSize) {
		int positionOffset = GpuParticlePipelines.IDENTITY_PARTICLE.getOffset(VertexFormatElement.POSITION);
		int uv0Offset = GpuParticlePipelines.IDENTITY_PARTICLE.getOffset(VertexFormatElement.UV0);
		int colorOffset = GpuParticlePipelines.IDENTITY_PARTICLE.getOffset(GpuParticlePipelines.IDENTITY_COLOR);
		int uv2Offset = GpuParticlePipelines.IDENTITY_PARTICLE.getOffset(GpuParticlePipelines.IDENTITY_UV2);

		for (int i = 0; i < vertexCount; i++) {
			int baseOffset = i * vertexSize;

			float posX = buffer.getFloat(baseOffset + positionOffset);
			float posY = buffer.getFloat(baseOffset + positionOffset + 4);
			float posZ = buffer.getFloat(baseOffset + positionOffset + 8);

			float uv0X = buffer.getFloat(baseOffset + uv0Offset);
			float uv0Y = buffer.getFloat(baseOffset + uv0Offset + 4);

			float colorR = buffer.getFloat(baseOffset + colorOffset);
			float colorG = buffer.getFloat(baseOffset + colorOffset + 4);
			float colorB = buffer.getFloat(baseOffset + colorOffset + 8);
			float colorA = buffer.getFloat(baseOffset + colorOffset + 12);

			int lightU = buffer.getInt(baseOffset + uv2Offset);
			int lightV = buffer.getInt(baseOffset + uv2Offset + 4);

			processParsedParticle(i, posX, posY, posZ, uv0X, uv0Y,
				colorR, colorG, colorB, colorA, lightU, lightV);
		}
	}

	private void processParsedParticle(int index, float x, float y, float z,
	                                   float u, float v, float r, float g, float b, float a,
	                                   int lightU, int lightV) {
		System.out.println("Particle Vertex: " + index);
		System.out.println("Pos: " + x + ", " + y + ", " + z);
		System.out.println("UV: " + u + ", " + v);
		System.out.println("Color: " + r + ", " + g + ", " + b + ", " + a);
		System.out.println("Light: " + lightU + ", " + lightV);
		System.out.println("--------------------------------------------------------------------------------");
	}

	@Override
	public void append(Vec3 prevGpuCamPos, SingleQuadParticle sqp) {
	}

	@Override
	public void resize(int particleLimit) {
		int rawSize = particleLimit * RAW_PARTICLE.getVertexSize();
		if (rawSize != sources[0].getSize()) {
			sources[0].resize0(rawSize);
		}
		if (rawSize != sources[1].getSize()) {
			sources[1].resize0(rawSize);
		}
		int proceedSize = particleLimit * 4 * GpuParticlePipelines.IDENTITY_PARTICLE.getVertexSize();
		if (proceedSize != target.getSize()) {
			GlBuffer.MEMORY_POOl.free(target.vbo);
			target.resize0(proceedSize);
			int newSize = target.getSize();
			targetMoj.size = newSize;
			GlBuffer.MEMORY_POOl.malloc(target.vbo, newSize);
		}
		this.particleLimit = particleLimit;
	}

	@Override
	public Collection<SingleQuadParticle.Layer> getComputeLayers() {
		return computeData[processingIndex ^ 1].getLayers();
	}

	@Override
	public void reload() {
		mappedBuffer = null;
		for (int i = 0; i < 2; i++) {
			camPositions[i] = Vec3.ZERO;
			particleCount[i] = 0;
			computeData[i].clear();
			if (sources[i].isMapped()) {
				sources[i].unmap();
			}
		}
		particleLimit = AsyncParticlesConfig.DEFAULT_PARTICLE_LIMIT;
		processingIndex = 0;
		shouldSkip = true;
		computed = false;
	}

	public void close() {
		reload();
		sources[0].delete();
		sources[1].delete();
		targetMoj.close();
		target.delete(true, false);
	}

	protected static class ComputeData {
		private final Reference2IntMap<SingleQuadParticle.Layer> layerMap = new Reference2IntOpenHashMap<>();
		private ComputeResult result;

		public void clear() {
			layerMap.clear();
			result = null;
		}

		public void add(SingleQuadParticle.Layer layer, int count) {
			if (result != null) {
				throw new IllegalStateException("Cannot update layer while result is present");
			}
			layerMap.merge(layer, count, Integer::sum);
		}

		public void set(SingleQuadParticle.Layer layer, int count) {
			if (result != null) {
				throw new IllegalStateException("Cannot update layer while result is present");
			}
			layerMap.put(layer, count);
		}

		public Collection<SingleQuadParticle.Layer> getLayers() {
			return layerMap.keySet();
		}

		public int getCount(SingleQuadParticle.Layer layer) {
			return layerMap.getInt(layer);
		}

		public int getTotalCount() {
			int count = 0;
			for (int c : layerMap.values()) {
				count += c;
			}
			return count;
		}

		public ComputeResult getResult(GpuBuffer buffer) {
			if (result != null) {
				return result;
			}
			ComputeResult.ParticleSlice[] slices = new ComputeResult.ParticleSlice[layerMap.size()];
	//		VertexFormat.Mode quads = VertexFormat.Mode.QUADS;
			int i = 0;
			int baseCount = 0;
			for (Reference2IntMap.Entry<SingleQuadParticle.Layer> entry : layerMap.reference2IntEntrySet()) {
				int count = entry.getIntValue();
				slices[i++] = new ComputeResult.ParticleSlice(entry.getKey(), baseCount, count);
				baseCount += count;
			}
			return result = new ComputeResult(buffer, getTotalCount(), slices);
		}
	}
}

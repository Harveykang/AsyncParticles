package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlBuffer;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.GLCaps;
import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticlePipelines;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.buffer.ParticleVertexBuffer;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.shader.ParticleTransformFeedbackShader;
import fun.qu_an.minecraft.asyncparticles.client.util.MemStackUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class ParticleRenderer implements IParticleRenderer {
	private static final int[] multiDrawIndex = {0, 0};
	private static final int[] multiDrawCount = {0, 0};
	public static final VertexFormat RAW_PARTICLE = VertexFormat.builder()
		.add("oPosition", VertexFormatElement.POSITION)
		.add("Position", VertexFormatElement.POSITION)
		.add("Sizes", VertexFormatElement.UV0)
		.add("oUV0", VertexFormatElement.UV0)
		.add("UV0", VertexFormatElement.UV0)
		.add("oColor", VertexFormatElement.COLOR)
		.add("Color", VertexFormatElement.COLOR)
		.add("Light", VertexFormatElement.UV2)
		.add("Rolls", VertexFormatElement.UV0)
		.build();
	protected final int[] particleCount = new int[4];
	protected ByteBuffer mappedBuffer;
	protected final ParticleVertexBuffer[] sources = new ParticleVertexBuffer[2];
	protected final ParticleVertexBuffer target;
	protected final GpuBuffer targetMoj;
	protected final Vec3[] camPositions = {Vec3.ZERO, Vec3.ZERO};
	protected int particleLimit;
	protected int processingIndex = 0;
	protected final int tf;
	protected boolean shouldSkip = true;
	protected boolean computed = false;
	protected final ComputeData[] computeData = {new ComputeData(), new ComputeData()};

	/**
	 * @see com.mojang.blaze3d.opengl.VertexArrayCache.Separate#bindVertexArray
	 */
	@SuppressWarnings("JavadocReference")
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
		bindAttr(RAW_PARTICLE, sources[0]);
		sources[1] = new ParticleVertexBuffer(true);
		bindAttr(RAW_PARTICLE, sources[1]);

		target = new ParticleVertexBuffer(-1, true);
		targetMoj = RenderSystem.getDevice().createBuffer(
			() -> "GPU_PARTICLE_BUFFER",
			GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_HINT_CLIENT_STORAGE,
			(long) AsyncParticlesConfig.DEFAULT_PARTICLE_LIMIT * 4 * GpuParticlePipelines.PLAIN_PARTICLE.getVertexSize()
		);
		int handle = ((GlBuffer) targetMoj).handle;
		GL15C.glDeleteBuffers(handle);
		((GlBuffer) targetMoj).handle = target.vbo;
		tf = GLCaps.tfSupport.genTransformFeedback();
		if (tf != -1) {
			GLCaps.tfSupport.glBindTransformFeedback(tf);
			GLCaps.tfSupport.glBindTransformFeedbackBuffer(0, target.vbo);
			GLCaps.tfSupport.glBindTransformFeedback(0);
		}
		resize(AsyncParticlesConfig.MIN_PARTICLE_LIMIT); // this.particleLimit = particleLimit;
	}

	private void bindAttr(VertexFormat format, ParticleVertexBuffer buffer) {
		buffer.bind();
		List<VertexFormatElement> elements = format.getElements();

		for (int i = 0, offset = 0; i < elements.size(); i++) {
			VertexFormatElement element = elements.get(i);
			GlStateManager._enableVertexAttribArray(i);
			if (!element.normalized() && element.type() != VertexFormatElement.Type.FLOAT) {
				ARBVertexAttribBinding.glVertexAttribIFormat(i, element.count(), GlConst.toGl(element.type()), offset);
			} else {
				ARBVertexAttribBinding.glVertexAttribFormat(i, element.count(), GlConst.toGl(element.type()), element.normalized(), offset);
			}
			ARBVertexAttribBinding.glVertexAttribBinding(i, 0);
			offset += element.byteSize();
		}

		ARBVertexAttribBinding.glBindVertexBuffer(0, buffer.vbo, 0L, format.getVertexSize());
		ParticleVertexBuffer.unbind();
	}

	@Override
	public void beginFrame() {
		computed = false;
	}

	@Override
	public void unmapBufferAndSwap() {
		if (mappedBuffer != null) {
			unmapBuffer();
			shouldSkip = false;
			processingIndex ^= 1;
		} else {
			shouldSkip = true;
		}
		this.particleCount[processingIndex] = 0;
		this.particleCount[2 | processingIndex] = 0;
	}

	@Override
	public void mapBuffer() {
		if (mappedBuffer != null) {
			throw new IllegalStateException("Mapped buffer is not null");
		}
		ParticleVertexBuffer source = sources[processingIndex];
		mappedBuffer = source.map(this.particleLimit * RAW_PARTICLE.getVertexSize());
	}

	@Override
	public void unmapBuffer() {
		RenderSystem.assertOnRenderThread();
		// correct the particle count
		particleCount[2 | processingIndex] = Math.max(0, particleCount[processingIndex] - particleLimit);
		particleCount[processingIndex] = Math.min(particleLimit, this.particleCount[processingIndex]);
		if (mappedBuffer == null) {
			throw new IllegalStateException("Mapped buffer is null!");
		}
		sources[processingIndex].unmap(particleCount[processingIndex] * RAW_PARTICLE.getVertexSize());
//		debugPrintBuffer(mappedBuffer);
		mappedBuffer = null;
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
				int layerCount = 0;
				for (SingleQuadParticle sqp : entry.getValue()) {
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

					// oColor (48-51)
					MemoryUtil.memPutByte(ptr, (byte) (sqp.rCol * 255f));
					ptr += 1L;
					MemoryUtil.memPutByte(ptr, (byte) (sqp.gCol * 255f));
					ptr += 1L;
					MemoryUtil.memPutByte(ptr, (byte) (sqp.bCol * 255f));
					ptr += 1L;
					MemoryUtil.memPutByte(ptr, (byte) (sqp.alpha * 255f));
					ptr += 1L;

					// Color (52-55)
					MemoryUtil.memPutByte(ptr, (byte) (sqp.rCol * 255f));
					ptr += 1L;
					MemoryUtil.memPutByte(ptr, (byte) (sqp.gCol * 255f));
					ptr += 1L;
					MemoryUtil.memPutByte(ptr, (byte) (sqp.bCol * 255f));
					ptr += 1L;
					MemoryUtil.memPutByte(ptr, (byte) (sqp.alpha * 255f));
					ptr += 1L;

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

		if (tf != -1) {
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

		if (tf == -1) {
			GLCaps.tfSupport.glBindTransformFeedbackBuffer(0, target.vbo);
		}

		GL11C.glEnable(GL30C.GL_RASTERIZER_DISCARD);

		GLCaps.tfSupport.glBeginTransformFeedback(GL11C.GL_POINTS);
		int overflow = particleCount[2 | processingIndex ^ 1];
		if (overflow <= 0) {
			GL11C.glDrawArrays(GL11C.GL_POINTS, 0, particleCount[processingIndex ^ 1]);
		} else {
			multiDrawIndex[0] = overflow;
			multiDrawCount[0] = this.particleLimit - overflow;
			multiDrawCount[1] = overflow;
			GL14C.glMultiDrawArrays(GL11C.GL_POINTS,
				multiDrawIndex,
				multiDrawCount);
		}
		GLCaps.tfSupport.glEndTransformFeedback();

		GL11C.glDisable(GL30C.GL_RASTERIZER_DISCARD);

		ParticleVertexBuffer.unbind();

		if (tf != -1) {
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
		int vertexSize = GpuParticlePipelines.PLAIN_PARTICLE.getVertexSize();
		int bufferSize = vertexCount * vertexSize;
		System.out.println("=== DEBUG: Buffer Layout ===");
		System.out.println("Vertex count: " + vertexCount);
		System.out.println("Vertex size: " + vertexSize + " bytes");
		System.out.println("Buffer size: " + bufferSize + " bytes");

		List<VertexFormatElement> elements = GpuParticlePipelines.PLAIN_PARTICLE.getElements();
		for (int i = 0; i < elements.size(); i++) {
			VertexFormatElement elem = elements.get(i);
			int offset = GpuParticlePipelines.PLAIN_PARTICLE.getOffset(elem);
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
		int positionOffset = GpuParticlePipelines.PLAIN_PARTICLE.getOffset(VertexFormatElement.POSITION);
		int uv0Offset = GpuParticlePipelines.PLAIN_PARTICLE.getOffset(VertexFormatElement.UV0);
		int colorOffset = GpuParticlePipelines.PLAIN_PARTICLE.getOffset(GpuParticlePipelines.PLAIN_COLOR);
		int uv2Offset = GpuParticlePipelines.PLAIN_PARTICLE.getOffset(GpuParticlePipelines.PLAIN_UV2);

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

	// FIXME incorrect overflow
	@Override
	public void append(Vec3 cameraPos, SingleQuadParticle sqp) {
		if (true) {
			return;
		}
		if (!((GpuParticleAddon) sqp).asyncparticles$shouldRender()) {
			return;
		}
//		ParticleCullingMode particleCullingMode = ConfigHelper.getGpuParticleCullingMode();
//		if (shouldCull(particleCullingMode, gpuBehavior.getFrustum(), sqp)) {
//			return;
//		}
		final double cx;
		final double cy;
		final double cz;
		final int particleCount = this.particleCount[processingIndex];
		final int particleLimit = this.particleLimit;
		final int offset;
		final int vertexSize = RAW_PARTICLE.getVertexSize();
		if (particleCount >= particleLimit) {
//			baseVertex = particleCount % particleLimit * ParticleVertexFormats.RAW_PARTICLE_BYTES;
			// particleCount can not greater than 2 * particleLimit, so '%' is unnecessary.
			offset = (particleCount - particleLimit) * vertexSize;
		} else {
			offset = particleCount * vertexSize;
		}
		if (mappedBuffer != null) {
			cx = camPositions[processingIndex].x;
			cy = camPositions[processingIndex].y;
			cz = camPositions[processingIndex].z;
		} else {
			mapBuffer();
			this.camPositions[processingIndex] = cameraPos;
			cx = cameraPos.x;
			cy = cameraPos.y;
			cz = cameraPos.z;
		}
		float oSize = sqp.getQuadSize(0f);
		float size = sqp.getQuadSize(1f);
		float minU = sqp.getU0();
		float minV = sqp.getV0();
		float maxU = sqp.getU1();
		float maxV = sqp.getV1();
		int light = sqp.getLightCoords(0f); // TODO lerp

		try (MemoryStack stack = MemStackUtil.stackPush()) {
			final long address = stack.nmalloc(vertexSize);
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

			// oColor (48-51)
			MemoryUtil.memPutByte(ptr, (byte) (sqp.rCol * 255f));
			ptr += 1L;
			MemoryUtil.memPutByte(ptr, (byte) (sqp.gCol * 255f));
			ptr += 1L;
			MemoryUtil.memPutByte(ptr, (byte) (sqp.bCol * 255f));
			ptr += 1L;
			MemoryUtil.memPutByte(ptr, (byte) (sqp.alpha * 255f));
			ptr += 1L;

			// Color (52-55)
			MemoryUtil.memPutByte(ptr, (byte) (sqp.rCol * 255f));
			ptr += 1L;
			MemoryUtil.memPutByte(ptr, (byte) (sqp.gCol * 255f));
			ptr += 1L;
			MemoryUtil.memPutByte(ptr, (byte) (sqp.bCol * 255f));
			ptr += 1L;
			MemoryUtil.memPutByte(ptr, (byte) (sqp.alpha * 255f));
			ptr += 1L;

			// Light (56-59): 2 shorts
			MemoryUtil.memPutInt(ptr, light);
			ptr += 4L;

			// Rolls (60-67)
			MemoryUtil.memPutFloat(ptr, sqp.oRoll);
			ptr += 4L;
			MemoryUtil.memPutFloat(ptr, sqp.roll);

			((GpuParticleAddon) sqp).asyncparticles$postTick(address);

			long bufferAddress = MemoryUtil.memAddress(mappedBuffer);
			MemoryUtil.memCopy(address, bufferAddress + (long) offset,
				vertexSize);

			this.particleCount[processingIndex] = particleCount + 1;
		}
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
		int proceedSize = particleLimit * 4 * GpuParticlePipelines.PLAIN_PARTICLE.getVertexSize();
		if (proceedSize != target.getSize()) {
			target.resize0(proceedSize);
			targetMoj.size = target.getSize();
		}
		this.particleLimit = particleLimit;
	}

	@Override
	public Collection<SingleQuadParticle.Layer> getLayers() {
		return computeData[processingIndex ^ 1].getLayers();
	}
}

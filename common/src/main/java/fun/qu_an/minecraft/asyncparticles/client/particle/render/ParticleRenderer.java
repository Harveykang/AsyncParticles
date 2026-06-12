package fun.qu_an.minecraft.asyncparticles.client.particle.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormat;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.GLCaps;
import fun.qu_an.minecraft.asyncparticles.client.particle.buffer.BufferHelper;
import fun.qu_an.minecraft.asyncparticles.client.particle.buffer.ParticleVertexBuffer;
import fun.qu_an.minecraft.asyncparticles.client.particle.shader.ParticleTransformFeedbackShader;
import fun.qu_an.minecraft.asyncparticles.client.util.MemStackUtil;
import it.unimi.dsi.fastutil.HashCommon;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Queue;

@Environment(EnvType.CLIENT)
public class ParticleRenderer implements IParticleRenderer {
	private static final int[] multiDrawIndex = {0, 0};
	private static final int[] multiDrawCount = {0, 0};
	private static final boolean DIRECT_BUFFER = true;
	private final ParticleVertexBuffer[] sources = {new ParticleVertexBuffer(false), new ParticleVertexBuffer(false)};
	private final ParticleVertexBuffer target = new ParticleVertexBuffer(true);
	private int particleLimit;
	private int current = 0;
	private ByteBuffer mappedBuffer;
	private final Vec3[] cameraPositions = {Vec3.ZERO, Vec3.ZERO};
	private final int[] particleCount = {0, 0, 0, 0};
	private int maxParticleCount;
	private boolean shouldSkip = true;
	private final BufferHelper bufferHelper = DIRECT_BUFFER ? null : new BufferHelper();
	private final int tf;
	private boolean computed = false;

	public ParticleRenderer(int particleLimit) {
		sources[0].bind();
		ParticleVertexFormats.RAW_PARTICLE.setupBufferState(); // attributes
		sources[1].bind();
		ParticleVertexFormats.RAW_PARTICLE.setupBufferState(); // attributes
		target.bind();
		ParticleVertexFormats.PROCESSED_PARTICLE.setupBufferState(); // attributes
		RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		autoStorageIndexBuffer.bind(256);
		ParticleVertexBuffer.unbind();
		tf = GLCaps.tfSupport.genTransformFeedback();
		if (tf != -1) {
			GLCaps.tfSupport.glBindTransformFeedback(tf);
			GLCaps.tfSupport.glBindTransformFeedbackBuffer(0, target.vbo);
			GLCaps.tfSupport.glBindTransformFeedback(0);
		}
		resize(particleLimit); // this.particleLimit = particleLimit;
	}

	public void beginFrame() {
		computed = false;
	}

	@Override
	public void unmapBufferAndSwap() {
		if (DIRECT_BUFFER ? mappedBuffer != null : bufferHelper.isBuilding()) {
			unmapBuffer();
			shouldSkip = particleCount[current] == 0;
			current ^= 1;
		} else {
			shouldSkip = true;
		}
		this.particleCount[current] = 0;
	}

	@Override
	public void mapBuffer() {
		if (DIRECT_BUFFER) {
			if (mappedBuffer != null) {
				throw new IllegalStateException("Mapped buffer is not null");
			}
			ParticleVertexBuffer source = sources[current];
			mappedBuffer = source.map(this.particleLimit * ParticleVertexFormats.RAW_PARTICLE_BYTES);
		} else {
			bufferHelper.begin();
		}
	}

	@Override
	public void unmapBuffer() {
		RenderSystem.assertOnRenderThread();
		// correct the particle count
		particleCount[2 | current] = particleCount[current] - particleLimit;
		particleCount[current] = Math.min(particleLimit, this.particleCount[current]);
		if (DIRECT_BUFFER) {
			if (mappedBuffer == null) {
				throw new IllegalStateException("Mapped buffer is null!");
			}
			sources[current].unmap(particleCount[current] * ParticleVertexFormats.RAW_PARTICLE_BYTES);
//		debugPrintBuffer(mappedBuffer);
			mappedBuffer = null;
		} else {
			ByteBuffer data = bufferHelper.endAndFlip();
			if (data.remaining() == 0) {
				data.clear();
				return;
			}
			GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, sources[current].vbo);
			GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, data.remaining(), GL15C.GL_DYNAMIC_DRAW); // 释放旧缓冲
			GL15C.glBufferSubData(GL15C.GL_ARRAY_BUFFER, 0, data);
			data.clear();
			GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
		}
	}

	@Override
	public boolean isShouldSkip() {
		return shouldSkip;
	}

	@Override
	public void tick(Vec3 cameraPos, Queue<TextureSheetParticle> particles) {
		if (DIRECT_BUFFER && mappedBuffer == null) {
			throw new IllegalStateException("Mapped buffer is null!");
		}
		cameraPositions[current] = cameraPos;
		double cx = cameraPos.x;
		double cy = cameraPos.y;
		double cz = cameraPos.z;

		long bufferAddress = 0;
		if (DIRECT_BUFFER) {
			bufferAddress = MemoryUtil.memAddress(mappedBuffer);
		} else {
			bufferHelper.ensureCapacity(particles.size() * ParticleVertexFormats.RAW_PARTICLE_BYTES);
		}
		int position = 0;
//		Frustum frustum = gpuBehavior.getFrustum();
//		ParticleCullingMode particleCullingMode = ConfigHelper.getGpuParticleCullingMode();
		try (MemoryStack stack = MemStackUtil.stackPush()) {
			final long address = stack.nmalloc(ParticleVertexFormats.RAW_PARTICLE_BYTES);
			for (TextureSheetParticle tsp : particles) {
				if (!((GpuParticleAddon) tsp).asyncparticles$shouldRender()) {
					continue;
				}
//				if (shouldCull(particleCullingMode, frustum, tsp)) {
//					continue;
//				}
				float oSize = tsp.getQuadSize(0f);
				float size = tsp.getQuadSize(1f);
				float minU = tsp.getU0();
				float minV = tsp.getV0();
				float maxU = tsp.getU1();
				float maxV = tsp.getV1();
				int light = tsp.getLightColor(0f); // TODO lerp

				long ptr = address;

				// oPosition (0-11)
				MemoryUtil.memPutFloat(ptr, (float) (tsp.xo - cx));
				ptr += 4L;
				MemoryUtil.memPutFloat(ptr, (float) (tsp.yo - cy));
				ptr += 4L;
				MemoryUtil.memPutFloat(ptr, (float) (tsp.zo - cz));
				ptr += 4L;

				// Position (12-23)
				MemoryUtil.memPutFloat(ptr, (float) (tsp.x - cx));
				ptr += 4L;
				MemoryUtil.memPutFloat(ptr, (float) (tsp.y - cy));
				ptr += 4L;
				MemoryUtil.memPutFloat(ptr, (float) (tsp.z - cz));
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

				int color = FastColor.ABGR32.color(
					(int) (tsp.alpha * 255.0f),
					(int) (tsp.bCol * 255.0f),
					(int) (tsp.gCol * 255.0f),
					(int) (tsp.rCol * 255.0f));
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
				MemoryUtil.memPutFloat(ptr, tsp.oRoll);
				ptr += 4L;
				MemoryUtil.memPutFloat(ptr, tsp.roll);

				((GpuParticleAddon) tsp).asyncparticles$postTick(address);

				if (DIRECT_BUFFER) {
					MemoryUtil.memCopy(address, bufferAddress + (long) position, ParticleVertexFormats.RAW_PARTICLE_BYTES);
				} else {
					bufferHelper.copyUnsafeFrom(address, ParticleVertexFormats.RAW_PARTICLE_BYTES);
				}
				position += ParticleVertexFormats.RAW_PARTICLE_BYTES;
			}
		}
		this.particleCount[current] = position / ParticleVertexFormats.RAW_PARTICLE_BYTES;
	}

//	private static boolean shouldCull(ParticleCullingMode particleCullingMode, Frustum frustum, TextureSheetParticle tsp) {
//		ParticleAddon particleAddon = (ParticleAddon) tsp;
//		return particleAddon.asyncparticles$shouldCull() &&
//			switch (particleCullingMode) {
//				case AABB -> !FrustumUtil.isVisible(frustum, tsp.getBoundingBox());
//				case DISABLED -> false;
//				case SPHERE -> !FrustumUtil.isVisible(frustum, tsp);
//				case ASYNC_AABB, ASYNC_SPHERE -> !particleAddon.asyncparticles$isVisibleOnScreen();
//			};
//	}

	@Override
	public void compute(Camera camera, float partialTicks) {
		if (computed) {
			return; // compute once per frame
		}
		if (shouldSkip) {
			throw new IllegalStateException("Should skip rendering during this tick!");
		}
		RenderSystem.assertOnRenderThread();
		BufferUploader.invalidate();
		if (tf != -1) {
			GLCaps.tfSupport.glBindTransformFeedback(tf);
		}
		Vector3f cameraLeftVector = camera.getLeftVector();
		Vector3f cameraUpVector = camera.getUpVector();
		Vec3 camPos = camera.getPosition();
		Vec3 lastCamPos = cameraPositions[current ^ 1];
		ParticleTransformFeedbackShader.INSTANCE.use();
//		if (GLCaps.supportsUniformBufferObject) {
//			TFUniformBuffer.TF_UNIFORM_BUFFER.bindUBO(0);
//		} else {
		ParticleTransformFeedbackShader.INSTANCE.setup(partialTicks,
			cameraLeftVector.x,
			cameraLeftVector.y,
			cameraLeftVector.z,
			cameraUpVector.x,
			cameraUpVector.y,
			cameraUpVector.z,
			(float) (lastCamPos.x - camPos.x),
			(float) (lastCamPos.y - camPos.y),
			(float) (lastCamPos.z - camPos.z));
//		}

		if (maxParticleCount < particleCount[current ^ 1]) {
			int nextCount = Math.min(HashCommon.nextPowerOfTwo(particleCount[current ^ 1]), this.particleLimit);
			target.resize(nextCount * 4 * ParticleVertexFormats.PROCESSED_PARTICLE_VERTEX_BYTES);
			maxParticleCount = nextCount;
		}
		sources[current ^ 1].bind();

		if (tf == -1) {
			GLCaps.tfSupport.glBindTransformFeedbackBuffer(0, target.vbo);
		}

		GL11C.glEnable(GL30C.GL_RASTERIZER_DISCARD);

		GLCaps.tfSupport.glBeginTransformFeedback(GL11C.GL_POINTS);
		int overflow = particleCount[2 | current ^ 1];
		if (overflow <= 0) {
			GL11C.glDrawArrays(GL11C.GL_POINTS, 0, particleCount[current ^ 1]);
		} else {
			multiDrawIndex[0] = overflow;
			multiDrawCount[0] = this.particleLimit - overflow;
			multiDrawCount[1] = overflow;
			GL14C.glMultiDrawArrays(GL11C.GL_POINTS,
				multiDrawIndex,
				multiDrawCount);
		}
		GLCaps.tfSupport.glEndTransformFeedback();
//		debugReadback(Math.min(particleCount[current ^ 1], 16));

		GL11C.glDisable(GL30C.GL_RASTERIZER_DISCARD);

		ParticleVertexBuffer.unbind();

		if (tf != -1) {
			GLCaps.tfSupport.glBindTransformFeedback(0);
		}

		computed = true;
	}

	@Override
	public void render() {
		RenderSystem.assertOnRenderThread();
		BufferUploader.invalidate();

		target.bind();

		int indexCount = VertexFormat.Mode.QUADS.indexCount(particleCount[current ^ 1] * 4);
		RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		if (!autoStorageIndexBuffer.hasStorage(indexCount)) {
			autoStorageIndexBuffer.bind(indexCount);
		}

		ShaderInstance shader = RenderSystem.getShader();
		Objects.requireNonNull(shader);

		IParticleRenderer.prepareShader(shader);

		shader.apply();

		GL11C.glDrawElements(VertexFormat.Mode.QUADS.asGLMode,
			indexCount,
			autoStorageIndexBuffer.type().asGLType,
			0L
		);

		shader.clear();

		ParticleVertexBuffer.unbind();
	}

	@Override
	public void append(Vec3 cameraPos, TextureSheetParticle tsp) {
		if (!((GpuParticleAddon) tsp).asyncparticles$shouldRender()) {
			return;
		}
//		ParticleCullingMode particleCullingMode = ConfigHelper.getGpuParticleCullingMode();
//		if (shouldCull(particleCullingMode, gpuBehavior.getFrustum(), tsp)) {
//			return;
//		}
		double cx;
		double cy;
		double cz;
		int particleCount = this.particleCount[current];
		int particleLimit = this.particleLimit;
		int offset;
		if (particleCount >= particleLimit) {
//			offset = particleCount % particleLimit * ParticleVertexFormats.RAW_PARTICLE_BYTES;
			// particleCount can not greater than 2 * particleLimit, so '%' is unnecessary.
			offset = (particleCount - particleLimit) * ParticleVertexFormats.RAW_PARTICLE_BYTES;
		} else {
			offset = particleCount * ParticleVertexFormats.RAW_PARTICLE_BYTES;
		}
		if (DIRECT_BUFFER ? mappedBuffer != null : bufferHelper.isBuilding()) {
			cx = cameraPositions[current].x;
			cy = cameraPositions[current].y;
			cz = cameraPositions[current].z;
		} else {
			if (DIRECT_BUFFER) {
				mapBuffer();
			} else {
				bufferHelper.begin();
			}
			this.cameraPositions[current] = cameraPos;
			cx = cameraPos.x;
			cy = cameraPos.y;
			cz = cameraPos.z;
		}
		float oSize = tsp.getQuadSize(0f);
		float size = tsp.getQuadSize(1f);
		float minU = tsp.getU0();
		float minV = tsp.getV0();
		float maxU = tsp.getU1();
		float maxV = tsp.getV1();
		int light = tsp.getLightColor(0f); // TODO lerp

		try (MemoryStack stack = MemStackUtil.stackPush()) {
			final long address = stack.nmalloc(ParticleVertexFormats.RAW_PARTICLE_BYTES);
			long ptr = address;

			// oPosition (0-11)
			MemoryUtil.memPutFloat(ptr, (float) (tsp.xo - cx));
			ptr += 4L;
			MemoryUtil.memPutFloat(ptr, (float) (tsp.yo - cy));
			ptr += 4L;
			MemoryUtil.memPutFloat(ptr, (float) (tsp.zo - cz));
			ptr += 4L;

			// Position (12-23)
			MemoryUtil.memPutFloat(ptr, (float) (tsp.x - cx));
			ptr += 4L;
			MemoryUtil.memPutFloat(ptr, (float) (tsp.y - cy));
			ptr += 4L;
			MemoryUtil.memPutFloat(ptr, (float) (tsp.z - cz));
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

			int color = FastColor.ABGR32.color(
				(int) (tsp.alpha * 255.0f),
				(int) (tsp.bCol * 255.0f),
				(int) (tsp.gCol * 255.0f),
				(int) (tsp.rCol * 255.0f));
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
			MemoryUtil.memPutFloat(ptr, tsp.oRoll);
			ptr += 4L;
			MemoryUtil.memPutFloat(ptr, tsp.roll);

			((GpuParticleAddon) tsp).asyncparticles$postTick(address);

			if (DIRECT_BUFFER) {
				long bufferAddress = MemoryUtil.memAddress(mappedBuffer);
				MemoryUtil.memCopy(address, bufferAddress + (long) offset,
					ParticleVertexFormats.RAW_PARTICLE_BYTES);
			} else {
				bufferHelper.copyFrom(address, ParticleVertexFormats.RAW_PARTICLE_BYTES);
			}
			this.particleCount[current] = particleCount + 1;
		}
	}

	@Override
	public void resize(int particleLimit) {
		int rawSize = particleLimit * ParticleVertexFormats.RAW_PARTICLE_BYTES;
		if (rawSize != sources[0].getSize()) {
			sources[0].resize0(rawSize);
		}
		if (rawSize != sources[1].getSize()) {
			sources[1].resize0(rawSize);
		}
		int proceedSize = particleLimit * 4 * ParticleVertexFormats.PROCESSED_PARTICLE_VERTEX_BYTES;
		if (proceedSize != target.getSize()) {
			target.resize0(proceedSize);
		}
		this.particleLimit = particleLimit;
	}
}

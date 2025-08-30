package fun.qu_an.minecraft.asyncparticles.client.particle;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormat;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.GLCaps;
import fun.qu_an.minecraft.asyncparticles.client.util.MemStackUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.Queue;

public class ParticleRenderer {
	private static final int[] multiDrawIndex = {0, 0};
	private static final int[] multiDrawCount = {0, 0};
	private static final boolean DIRECT_BUFFER = true;
	private final ParticleVertexBuffer[] sources = {new ParticleVertexBuffer(), new ParticleVertexBuffer()};
	private final ParticleVertexBuffer target = new ParticleVertexBuffer();
	private int current = 0;
	private ByteBuffer mappedBuffer;
	private final Vec3[] cameraPositions = {Vec3.ZERO, Vec3.ZERO};
	private final int[] particleCount = {0, 0, 0, 0};
	private int maxParticleCount;
	private boolean shouldSkip = true;
	private final BufferHelper bufferHelper = new BufferHelper();
	private final int tf;

	public ParticleRenderer() {
		sources[0].bind();
		ParticleVertexFormats.GPU_PARTICLE.setupBufferState(); // attributes
		sources[1].bind();
		ParticleVertexFormats.GPU_PARTICLE.setupBufferState(); // attributes
		target.bind();
		ParticleVertexFormats.PARTICLE.setupBufferState(); // attributes
		RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		autoStorageIndexBuffer.bind(256);
		ParticleVertexBuffer.unbind();
		if (!GLCaps.supportsTransformFeedback.isSupportsTfo()) {
			tf = -1;
		} else {
			tf = ARBTransformFeedback2.glGenTransformFeedbacks();
			ARBTransformFeedback2.glBindTransformFeedback(ARBTransformFeedback2.GL_TRANSFORM_FEEDBACK, tf);
			GL30C.glBindBufferBase(GL30C.GL_TRANSFORM_FEEDBACK_BUFFER, 0, target.vbo);
			ARBTransformFeedback2.glBindTransformFeedback(ARBTransformFeedback2.GL_TRANSFORM_FEEDBACK, 0);
		}
	}

	public void unmapBufferAndSwap() {
		if (DIRECT_BUFFER ? mappedBuffer != null : bufferHelper.isBuilding()) {
			unmapBuffer();
			shouldSkip = false;
			current ^= 1;
		} else {
			shouldSkip = true;
		}
		this.particleCount[current] = 0;
	}

	public void mapBuffer() {
		if (DIRECT_BUFFER) {
			if (mappedBuffer != null) {
				throw new IllegalStateException("Mapped buffer is not null");
			}
			ParticleVertexBuffer source = sources[current];
			mappedBuffer = source.map(GpuParticles.getParticleLimit() * ParticleVertexFormats.RAW_PARTICLE_BYTES);
		} else {
			bufferHelper.begin();
		}
	}

	public void unmapBuffer() {
		// correct the particle count
		particleCount[2 | current] = particleCount[current] - GpuParticles.getParticleLimit();
		particleCount[current] = Math.min(GpuParticles.getParticleLimit(), this.particleCount[current]);
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
			RenderSystem.assertOnRenderThread();
			GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, sources[current].vbo);
			GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, data.remaining(), GL15C.GL_DYNAMIC_DRAW); // 释放旧缓冲
			GL15C.glBufferSubData(GL15C.GL_ARRAY_BUFFER, 0, data);
			data.clear();
			GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
		}
	}

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
//		Frustum frustum = AsyncRenderer.frustum;
//		ParticleCullingMode particleCullingMode = ConfigHelper.getParticleCullingMode();
		try (MemoryStack stack = MemStackUtil.stackPush()) {
			final long address = stack.nmalloc(ParticleVertexFormats.RAW_PARTICLE_BYTES);
			for (TextureSheetParticle tsp : particles) {
				if (!((GpuParticleAddon) tsp).asyncparticles$shouldRender()) {
					continue;
				}
//				ParticleAddon particleAddon = (ParticleAddon) tsp;
//				if (particleAddon.shouldCull() &&
//					switch (particleCullingMode) {
//						case AABB -> !FrustumUtil.isVisible(frustum, tsp.getBoundingBox());
//						case DISABLED -> false;
//						case SPHERE -> !FrustumUtil.isVisible(frustum, tsp);
//						case ASYNC_AABB, ASYNC_SPHERE -> !particleAddon.asyncparticles$isVisibleOnScreen();
//					}) {
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

				// oColor (48-51)
				MemoryUtil.memPutByte(ptr, (byte) (tsp.rCol * 255f));
				ptr += 1L;
				MemoryUtil.memPutByte(ptr, (byte) (tsp.gCol * 255f));
				ptr += 1L;
				MemoryUtil.memPutByte(ptr, (byte) (tsp.bCol * 255f));
				ptr += 1L;
				MemoryUtil.memPutByte(ptr, (byte) (tsp.alpha * 255f));
				ptr += 1L;

				// Color (52-55)
				MemoryUtil.memPutByte(ptr, (byte) (tsp.rCol * 255f));
				ptr += 1L;
				MemoryUtil.memPutByte(ptr, (byte) (tsp.gCol * 255f));
				ptr += 1L;
				MemoryUtil.memPutByte(ptr, (byte) (tsp.bCol * 255f));
				ptr += 1L;
				MemoryUtil.memPutByte(ptr, (byte) (tsp.alpha * 255f));
				ptr += 1L;

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

	public void runTf(Camera camera, float partialTicks) {
		if (shouldSkip) {
			throw new IllegalStateException("Should skip rendering during this tick!");
		}
		RenderSystem.assertOnRenderThread();
		BufferUploader.invalidate();
		if (tf != -1) {
			ARBTransformFeedback2.glBindTransformFeedback(ARBTransformFeedback2.GL_TRANSFORM_FEEDBACK, tf);
		}
		Vector3f cameraLeftVector = camera.getLeftVector();
		Vector3f cameraUpVector = camera.getUpVector();
		Vec3 camPos = camera.getPosition();
		Vec3 lastCamPos = cameraPositions[current ^ 1];
		TFShader.TF_SHADER.use();
//		if (GLCaps.supportsUniformBufferObject) {
//			TFUniformBuffer.TF_UNIFORM_BUFFER.bindUBO(0);
//		} else {
		TFShader.TF_SHADER.setup(partialTicks,
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
			target.resize(particleCount[current ^ 1] * 4 * ParticleVertexFormats.PROCESSED_PARTICLE_BYTES);
			maxParticleCount = particleCount[current ^ 1];
		}
		sources[current ^ 1].bind();

		if (tf == -1) {
			GL30C.glBindBufferBase(GL30C.GL_TRANSFORM_FEEDBACK_BUFFER, 0, target.vbo);
		}

		GL11C.glEnable(GL30C.GL_RASTERIZER_DISCARD);

		GL30C.glBeginTransformFeedback(GL11C.GL_POINTS);
		int overflow = particleCount[2 | current ^ 1];
		if (overflow <= 0) {
			GL11C.glDrawArrays(GL11C.GL_POINTS, 0, particleCount[current ^ 1]);
		} else {
			multiDrawIndex[0] = overflow;
			multiDrawCount[0] = GpuParticles.getParticleLimit() - overflow;
			multiDrawCount[1] = overflow;
			GL14C.glMultiDrawArrays(GL11C.GL_POINTS,
				multiDrawIndex,
				multiDrawCount);
		}
		GL30C.glEndTransformFeedback();
//		debugReadback(Math.min(particleCount[current ^ 1], 16));

		GL11C.glDisable(GL30C.GL_RASTERIZER_DISCARD);

		ParticleVertexBuffer.unbind();

		if (tf != -1) {
			ARBTransformFeedback2.glBindTransformFeedback(ARBTransformFeedback2.GL_TRANSFORM_FEEDBACK, 0);
		}
	}

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

		prepareShader(shader);

		shader.apply();

		GL11.glDrawElements(VertexFormat.Mode.QUADS.asGLMode,
			indexCount,
			autoStorageIndexBuffer.type().asGLType,
			0L
		);

		shader.clear();

		ParticleVertexBuffer.unbind();
	}

	public boolean isShouldSkip() {
		return shouldSkip;
	}

	public void append(Vec3 cameraPos, TextureSheetParticle tsp) {
		if (!((GpuParticleAddon) tsp).asyncparticles$shouldRender()) {
			return;
		}
//				ParticleAddon particleAddon = (ParticleAddon) tsp;
//				if (particleAddon.shouldCull() &&
//					switch (particleCullingMode) {
//						case AABB -> !FrustumUtil.isVisible(frustum, tsp.getBoundingBox());
//						case DISABLED -> false;
//						case SPHERE -> !FrustumUtil.isVisible(frustum, tsp);
//						case ASYNC_AABB, ASYNC_SPHERE -> !particleAddon.asyncparticles$isVisibleOnScreen();
//					}) {
//					continue;
//				}
		double cx;
		double cy;
		double cz;
		int particleCount = this.particleCount[current];
		int particleLimit = GpuParticles.getParticleLimit();
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

			// oColor (48-51)
			MemoryUtil.memPutByte(ptr, (byte) (tsp.rCol * 255f));
			ptr += 1L;
			MemoryUtil.memPutByte(ptr, (byte) (tsp.gCol * 255f));
			ptr += 1L;
			MemoryUtil.memPutByte(ptr, (byte) (tsp.bCol * 255f));
			ptr += 1L;
			MemoryUtil.memPutByte(ptr, (byte) (tsp.alpha * 255f));
			ptr += 1L;

			// Color (52-55)
			MemoryUtil.memPutByte(ptr, (byte) (tsp.rCol * 255f));
			ptr += 1L;
			MemoryUtil.memPutByte(ptr, (byte) (tsp.gCol * 255f));
			ptr += 1L;
			MemoryUtil.memPutByte(ptr, (byte) (tsp.bCol * 255f));
			ptr += 1L;
			MemoryUtil.memPutByte(ptr, (byte) (tsp.alpha * 255f));
			ptr += 1L;

			// Light (56-59): 两个 short
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

	public void resize(int particleLimit) {
		if (particleLimit != sources[0].size) {
			sources[0].resize0(particleLimit);
		}
		if (particleLimit != sources[1].size) {
			sources[1].resize0(particleLimit);
		}
	}

	private void debugPrintBuffer(ByteBuffer buffer) {
		System.out.println("=== Buffer 内容 (flip 后) ===");
		System.out.println("position=" + buffer.position() + ", limit=" + buffer.limit() + ", capacity=" + buffer.capacity());
		System.out.println("remaining=" + particleCount[current] + " bytes");

		long address = MemoryUtil.memAddress(buffer);
		int count = particleCount[current];
		for (int i = 0; i < count; i++) {
			System.out.println("\n--- 粒子 " + i + " ---");

			// oPosition (vec3, float)
			float oPosX = MemoryUtil.memGetFloat(address + i * 68 + 0);
			float oPosY = MemoryUtil.memGetFloat(address + i * 68 + 4);
			float oPosZ = MemoryUtil.memGetFloat(address + i * 68 + 8);
			System.out.printf("oPosition: (%.3f, %.3f, %.3f)%n", oPosX, oPosY, oPosZ);

			// Position (vec3, float)
			float posX = MemoryUtil.memGetFloat(address + i * 68 + 12);
			float posY = MemoryUtil.memGetFloat(address + i * 68 + 16);
			float posZ = MemoryUtil.memGetFloat(address + i * 68 + 20);
			System.out.printf("Position:  (%.3f, %.3f, %.3f)%n", posX, posY, posZ);

			// Sizes (vec2, float)
			float oSize = MemoryUtil.memGetFloat(address + i * 68 + 24);
			float size = MemoryUtil.memGetFloat(address + i * 68 + 28);
			System.out.printf("Sizes:     oSize=%.3f, size=%.3f%n", oSize, size);

			// UVMinMax (vec4, float)
			float u0 = MemoryUtil.memGetFloat(address + i * 68 + 32);
			float v0 = MemoryUtil.memGetFloat(address + i * 68 + 36);
			float u1 = MemoryUtil.memGetFloat(address + i * 68 + 40);
			float v1 = MemoryUtil.memGetFloat(address + i * 68 + 44);
			System.out.printf("UVMinMax:  (%.3f, %.3f) -> (%.3f, %.3f)%n", u0, v0, u1, v1);

			// Color (ubyte4)
			int ocolor = MemoryUtil.memGetInt(address + i * 68 + 48);
			System.out.printf("oColor:     (%d)%n", ocolor);

			// Color (ubyte4)
			int color = MemoryUtil.memGetInt(address + i * 68 + 48);
			System.out.printf("Color:     (%d)%n", color);

			// Light (SHORT2) → 两个 short
			int light = MemoryUtil.memGetInt(address + i * 68 + 52);
			System.out.printf("Light: %d%n", light);

			// Rolls (vec2, float)
			float oRoll = MemoryUtil.memGetFloat(address + i * 68 + 56);
			float roll = MemoryUtil.memGetFloat(address + i * 68 + 60);
			System.out.printf("Rolls:     oRoll=%.3f, roll=%.3f%n", oRoll, roll);
		}

		// 恢复 position，避免影响 upload
		buffer.rewind();
	}

	private void debugReadback(int particleCount) {
		// 1. 绑定 tVbo
		GL15.glBindBuffer(GL15C.GL_ARRAY_BUFFER, target.vbo);

		// 2. 映射缓冲区（读取）
		ByteBuffer data = GL30C.glMapBufferRange(GL15C.GL_ARRAY_BUFFER, 0, particleCount * ParticleVertexFormats.PARTICLE.getVertexSize() * 4L, GL30C.GL_MAP_READ_BIT);
		if (data != null) {
			data.order(ByteOrder.LITTLE_ENDIAN);
			System.out.println("=== TF 输出数据 ===");
			for (int i = 0; i < particleCount * 4; i++) {
				System.out.println("粒子 " + i + ":");
				int index = i * ParticleVertexFormats.PARTICLE.getVertexSize();
				float posX = data.getFloat(index);
				index += 4;
				float posY = data.getFloat(index);
				index += 4;
				float posZ = data.getFloat(index);
//				index += 4;
//				data.getFloat(index); // padding
				index += 4;
				float u = data.getFloat(index);
				index += 4;
				float v = data.getFloat(index);
//				index += 4;
//				data.getFloat(index); // padding
//				index += 4;
//				data.getFloat(index); // padding
				index += 4;
				float r = data.getFloat(index);
				index += 4;
				float g = data.getFloat(index);
				index += 4;
				float b = data.getFloat(index);
				index += 4;
				float a = data.getFloat(index);
				index += 4;
				int block = data.getInt(index);
				index += 4;
				int sky = data.getInt(index);
//				index += 4;
//				data.getInt(index); // padding
//				index += 4;
//				data.getInt(index); // padding

				System.out.printf("  Pos=(%.2f, %.2f, %.2f)%n", posX, posY, posZ);
				System.out.printf("  UV=(%.2f, %.2f)%n", u, v);
				System.out.printf("  Color=(%.2f, %.2f, %.2f, %.2f)%n", r, g, b, a);
				System.out.printf("  Light=(%d, %d)%n", block, sky);
			}

			// 3. 解除映射
			GL15.glUnmapBuffer(GL15C.GL_ARRAY_BUFFER);
		}

		GL15.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
	}

	private static void prepareShader(ShaderInstance shader) {
		for (int i = 0; i < 12; ++i) {
			int j = RenderSystem.getShaderTexture(i);
			shader.setSampler("Sampler" + i, j);
		}

		if (shader.MODEL_VIEW_MATRIX != null) {
			shader.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
		}

		if (shader.PROJECTION_MATRIX != null) {
			shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
		}

		if (shader.INVERSE_VIEW_ROTATION_MATRIX != null) {
			shader.INVERSE_VIEW_ROTATION_MATRIX.set(RenderSystem.getInverseViewRotationMatrix());
		}

		if (shader.COLOR_MODULATOR != null) {
			shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
		}

		if (shader.GLINT_ALPHA != null) {
			shader.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
		}

		if (shader.FOG_START != null) {
			shader.FOG_START.set(RenderSystem.getShaderFogStart());
		}

		if (shader.FOG_END != null) {
			shader.FOG_END.set(RenderSystem.getShaderFogEnd());
		}

		if (shader.FOG_COLOR != null) {
			shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
		}

		if (shader.FOG_SHAPE != null) {
			shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
		}

		if (shader.TEXTURE_MATRIX != null) {
			shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
		}

		if (shader.GAME_TIME != null) {
			shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
		}

		if (shader.SCREEN_SIZE != null) {
			Window window = Minecraft.getInstance().getWindow();
			shader.SCREEN_SIZE.set((float) window.getWidth(), (float) window.getHeight());
		}

//		if (shader.LINE_WIDTH != null && (this.mode == VertexFormat.Mode.LINES || this.mode == VertexFormat.Mode.LINE_STRIP)) {
//			shader.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
//		}

		RenderSystem.setupShaderLights(shader);
	}
}

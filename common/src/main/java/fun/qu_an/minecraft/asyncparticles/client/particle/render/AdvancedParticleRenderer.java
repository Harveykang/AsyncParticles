package fun.qu_an.minecraft.asyncparticles.client.particle.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormat;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncRenderBehavior;
import fun.qu_an.minecraft.asyncparticles.client.particle.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.particle.buffer.ParticleStorageBuffer;
import fun.qu_an.minecraft.asyncparticles.client.particle.buffer.ParticleVertexBuffer;
import fun.qu_an.minecraft.asyncparticles.client.particle.shader.ParticleComputeShader;
import fun.qu_an.minecraft.asyncparticles.client.particle.shader.ParticleCounterToIndirectShader;
import fun.qu_an.minecraft.asyncparticles.client.util.BufferHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.MemStackUtil;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.joml.FrustumIntersection;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.Queue;

public class AdvancedParticleRenderer implements IParticleRenderer {
	private static final VarHandle frustumPlanesHandle;

	static {
		try {
			Field frustumPlanes = FrustumIntersection.class.getDeclaredField("planes");
			frustumPlanes.setAccessible(true);
			frustumPlanesHandle = MethodHandles.privateLookupIn(FrustumIntersection.class, MethodHandles.lookup())
				.unreflectVarHandle(frustumPlanes);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static final int[] multiDrawIndex = {0, 0};
	private static final int[] multiDrawCount = {0, 0};
	private static final boolean DIRECT_BUFFER = true;
	private ByteBuffer mappedBuffer;
	private int maxParticleCount;
	private boolean shouldSkip = true;
	private int current = 0;
	private final ParticleStorageBuffer[] sources = {new ParticleStorageBuffer(), new ParticleStorageBuffer()};
	private final ParticleStorageBuffer counters = new ParticleStorageBuffer(8);
	private final ParticleStorageBuffer target = new ParticleStorageBuffer();
	private final ParticleVertexBuffer targetVertex = new ParticleVertexBuffer(GL30C.glGenVertexArrays(), target.ssbo);
	private final ParticleStorageBuffer indirect = new ParticleStorageBuffer(40); // {index, count, instanceCount, first, baseInstance}
	private final Vec3[] cameraPositions = {Vec3.ZERO, Vec3.ZERO};
	private final int[] particleCount = {0, 0, 0, 0};
	private final BufferHelper bufferHelper = new BufferHelper();
	private static final ParticleComputeShader computeShader = ParticleComputeShader.INSTANCE;
	private static final ParticleCounterToIndirectShader counterShader = ParticleCounterToIndirectShader.INSTANCE;

	public AdvancedParticleRenderer() {
		targetVertex.bind();
		ParticleVertexFormats.COMPUTE_SHADER_PROCESSED_PARTICLE.setupBufferState();
		RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		autoStorageIndexBuffer.bind(256);
		ParticleVertexBuffer.unbind();
	}

	@Override
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

	@Override
	public void mapBuffer() {
		if (DIRECT_BUFFER) {
			if (mappedBuffer != null) {
				throw new IllegalStateException("Mapped buffer is not null");
			}
			ParticleStorageBuffer source = sources[current];
			mappedBuffer = source.map(GpuParticleBehavior.getParticleLimit() * ParticleVertexFormats.COMPUTE_SHADER_RAW_PARTICLE_BYTES);
		} else {
			bufferHelper.begin();
		}
	}

	@Override
	public void unmapBuffer() {
		RenderSystem.assertOnRenderThread();
		// correct the particle count
		particleCount[2 | current] = particleCount[current] - GpuParticleBehavior.getParticleLimit();
		particleCount[current] = Math.min(GpuParticleBehavior.getParticleLimit(), this.particleCount[current]);
		if (DIRECT_BUFFER) {
			if (mappedBuffer == null) {
				throw new IllegalStateException("Mapped buffer is null!");
			}
			sources[current].unmap(particleCount[current] * ParticleVertexFormats.COMPUTE_SHADER_RAW_PARTICLE_BYTES);
//		debugPrintBuffer(mappedBuffer);
			mappedBuffer = null;
		} else {
			ByteBuffer data = bufferHelper.endAndFlip();
			if (data.remaining() == 0) {
				data.clear();
				return;
			}
			GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, sources[current].ssbo);
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
			bufferHelper.ensureCapacity(particles.size() * ParticleVertexFormats.COMPUTE_SHADER_RAW_PARTICLE_BYTES);
		}
		int position = 0;
//		Frustum frustum = AsyncRenderBehavior.INSTANCE.getFrustum();
//		ParticleCullingMode particleCullingMode = ConfigHelper.getParticleCullingMode();
		try (MemoryStack stack = MemStackUtil.stackPush()) {
			final long address = stack.nmalloc(ParticleVertexFormats.COMPUTE_SHADER_RAW_PARTICLE_BYTES);
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
				ptr += 4L;

				// Position (12-23)
				MemoryUtil.memPutFloat(ptr, (float) (tsp.x - cx));
				ptr += 4L;
				MemoryUtil.memPutFloat(ptr, (float) (tsp.y - cy));
				ptr += 4L;
				MemoryUtil.memPutFloat(ptr, (float) (tsp.z - cz));
				ptr += 4L;
				ptr += 4L;

				// oSize, size (24-31)
				MemoryUtil.memPutFloat(ptr, oSize);
				ptr += 4L;
				MemoryUtil.memPutFloat(ptr, size);
				ptr += 4L;
				ptr += 4L;
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
				ptr += 4L;

				// Rolls (60-67)
				MemoryUtil.memPutFloat(ptr, tsp.oRoll);
				ptr += 4L;
				MemoryUtil.memPutFloat(ptr, tsp.roll);

				((GpuParticleAddon) tsp).asyncparticles$postTick(address);

				if (DIRECT_BUFFER) {
					MemoryUtil.memCopy(address, bufferAddress + (long) position, ParticleVertexFormats.COMPUTE_SHADER_RAW_PARTICLE_BYTES);
				} else {
					bufferHelper.copyUnsafeFrom(address, ParticleVertexFormats.COMPUTE_SHADER_RAW_PARTICLE_BYTES);
				}
				position += ParticleVertexFormats.COMPUTE_SHADER_RAW_PARTICLE_BYTES;
			}
		}
		this.particleCount[current] = position / ParticleVertexFormats.COMPUTE_SHADER_RAW_PARTICLE_BYTES;
	}

	@Override
	public void compute(Camera camera, float partialTicks) {
		if (shouldSkip) {
			throw new IllegalStateException("Should skip rendering during this tick!");
		}
		RenderSystem.assertOnRenderThread();
		BufferUploader.invalidate();

		// Reset atomic counter
		counters.bind();
		GL30C.glBufferSubData(GL43C.GL_SHADER_STORAGE_BUFFER, 0, new int[]{0, 0});
		ParticleStorageBuffer.unbind();

		if (maxParticleCount < particleCount[current ^ 1]) {
			int nextCount = Math.min(HashCommon.nextPowerOfTwo(particleCount[current ^ 1]), GpuParticleBehavior.getParticleLimit());
			target.resize(nextCount * 4 * ParticleVertexFormats.COMPUTE_SHADER_PROCESSED_PARTICLE_VERTEX_BYTES);
			maxParticleCount = nextCount;
		}

		// Compute camera vectors
		Vec3 cameraPos = camera.getPosition();
		Vec3 prevCameraPos = cameraPositions[current ^ 1]; // previous frame camera

		Vector3f left = camera.getLeftVector();
		Vector3f up = camera.getUpVector();

		// Use compute shader
		computeShader.use();
		computeShader.setup(
			partialTicks,
			left.x, left.y, left.z,
			up.x, up.y, up.z,
			(float) (cameraPos.x - prevCameraPos.x), (float) (cameraPos.y - prevCameraPos.y), (float) (cameraPos.z - prevCameraPos.z),
			particleCount[current ^ 1],
			particleCount[2 | current ^ 1],
			(Vector4f[]) frustumPlanesHandle.get(AsyncRenderBehavior.INSTANCE.getFrustum().intersection)
		);

		ParticleComputeShader.bindBuffers(sources[current ^ 1].ssbo, target.ssbo, counters.ssbo);

		// Dispatch
		int numGroups = (particleCount[current ^ 1] + 255) / 256; // 256 threads per group
		GL43C.glDispatchCompute(numGroups, 1, 1);
		GL43C.glMemoryBarrier(GL43C.GL_SHADER_STORAGE_BARRIER_BIT | GL43C.GL_ATOMIC_COUNTER_BARRIER_BIT);

//		debugReadback(8);

		// Convert counter to indirect draw command
//		counterShader.use();
//		ParticleCounterToIndirectShader.bindBuffers(counters.ssbo, indirect.ssbo);
//		GL43C.glDispatchCompute(1, 1, 1);
		GL43C.glMemoryBarrier(GL43C.GL_COMMAND_BARRIER_BIT | GL43C.GL_BUFFER_UPDATE_BARRIER_BIT);
	}

	@Override
	public void render() {
		RenderSystem.assertOnRenderThread();
		BufferUploader.invalidate();

		targetVertex.bind();

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
//		if (shouldSkip) {
//			throw new IllegalStateException("Should skip rendering during this tick!");
//		}
//		RenderSystem.assertOnRenderThread();
//		BufferUploader.invalidate();
//
//		// Bind VAO, SSBO(as VBO)
//		targetVertex.bind();
//		GL15C.glBindBuffer(GL40C.GL_DRAW_INDIRECT_BUFFER, indirect.ssbo);
//
//		// Bind static index buffer
//		int indexCount = VertexFormat.Mode.QUADS.indexCount(particleCount[current ^ 1] * 4);
//		RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
//		if (!autoStorageIndexBuffer.hasStorage(indexCount)) {
//			autoStorageIndexBuffer.bind(indexCount);
//		}
//
//		ShaderInstance shader = RenderSystem.getShader();
//		Objects.requireNonNull(shader);
//
//		IParticleRenderer.prepareShader(shader);
//
//		shader.apply();
//
//		// Draw!
//		GL40C.glDrawElementsIndirect(GL11C.GL_TRIANGLES, GL11C.GL_UNSIGNED_INT, 0);
////		GL43C.glMultiDrawElementsIndirect(GL11C.GL_TRIANGLES, GL11C.GL_UNSIGNED_INT, new int[]{1, 0}, 2, 0);
//		GL40C.glDrawElementsIndirect(GL11C.GL_TRIANGLES, GL11C.GL_UNSIGNED_INT, 20);
//
//		shader.clear();
//
//		// Cleanup
//		ParticleVertexBuffer.unbind();
	}

	@Override
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
		int particleLimit = GpuParticleBehavior.getParticleLimit();
		int offset;
		if (particleCount >= particleLimit) {
//			offset = particleCount % particleLimit * ParticleVertexFormats.SSBO_PARTICLE_BYTES;
			// particleCount can not greater than 2 * particleLimit, so '%' is unnecessary.
			offset = (particleCount - particleLimit) * ParticleVertexFormats.COMPUTE_SHADER_RAW_PARTICLE_BYTES;
		} else {
			offset = particleCount * ParticleVertexFormats.COMPUTE_SHADER_RAW_PARTICLE_BYTES;
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
			final long address = stack.nmalloc(ParticleVertexFormats.COMPUTE_SHADER_RAW_PARTICLE_BYTES);
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
					ParticleVertexFormats.COMPUTE_SHADER_RAW_PARTICLE_BYTES);
			} else {
				bufferHelper.copyFrom(address, ParticleVertexFormats.COMPUTE_SHADER_RAW_PARTICLE_BYTES);
			}
			this.particleCount[current] = particleCount + 1;
		}
	}

	@Override
	public void resize(int particleLimit) {
		if (particleLimit != sources[0].getSize()) {
			sources[0].resize0(particleLimit);
		}
		if (particleLimit != sources[1].getSize()) {
			sources[1].resize0(particleLimit);
		}
		if (particleLimit < target.getSize()) {
			target.resize0(particleLimit * 4 * ParticleVertexFormats.COMPUTE_SHADER_PROCESSED_PARTICLE_VERTEX_BYTES);
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
			float oPosX = MemoryUtil.memGetFloat(address + i * 68L);
			float oPosY = MemoryUtil.memGetFloat(address + i * 68L + 4);
			float oPosZ = MemoryUtil.memGetFloat(address + i * 68L + 8);
			System.out.printf("oPosition: (%.3f, %.3f, %.3f)%n", oPosX, oPosY, oPosZ);

			// Position (vec3, float)
			float posX = MemoryUtil.memGetFloat(address + i * 68L + 12);
			float posY = MemoryUtil.memGetFloat(address + i * 68L + 16);
			float posZ = MemoryUtil.memGetFloat(address + i * 68L + 20);
			System.out.printf("Position:  (%.3f, %.3f, %.3f)%n", posX, posY, posZ);

			// Sizes (vec2, float)
			float oSize = MemoryUtil.memGetFloat(address + i * 68L + 24);
			float size = MemoryUtil.memGetFloat(address + i * 68L + 28);
			System.out.printf("Sizes:     oSize=%.3f, size=%.3f%n", oSize, size);

			// UVMinMax (vec4, float)
			float u0 = MemoryUtil.memGetFloat(address + i * 68L + 32);
			float v0 = MemoryUtil.memGetFloat(address + i * 68L + 36);
			float u1 = MemoryUtil.memGetFloat(address + i * 68L + 40);
			float v1 = MemoryUtil.memGetFloat(address + i * 68L + 44);
			System.out.printf("UVMinMax:  (%.3f, %.3f) -> (%.3f, %.3f)%n", u0, v0, u1, v1);

			// Color (ubyte4)
			int ocolor = MemoryUtil.memGetInt(address + i * 68L + 48);
			System.out.printf("oColor:     (%d)%n", ocolor);

			// Color (ubyte4)
			int color = MemoryUtil.memGetInt(address + i * 68L + 48);
			System.out.printf("Color:     (%d)%n", color);

			// Light (SHORT2) → 两个 short
			int light = MemoryUtil.memGetInt(address + i * 68L + 52);
			System.out.printf("Light: %d%n", light);

			// Rolls (vec2, float)
			float oRoll = MemoryUtil.memGetFloat(address + i * 68L + 56);
			float roll = MemoryUtil.memGetFloat(address + i * 68L + 60);
			System.out.printf("Rolls:     oRoll=%.3f, roll=%.3f%n", oRoll, roll);
		}

		// 恢复 position，避免影响 upload
		buffer.rewind();
	}

	private void debugReadback(int particleCount) {
		// 1. 绑定 tVbo
		GL15.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, target.ssbo);

		// 2. 映射缓冲区（读取）
		ByteBuffer data = GL30C.glMapBufferRange(GL43C.GL_SHADER_STORAGE_BUFFER, 0, particleCount * ParticleVertexFormats.COMPUTE_SHADER_PROCESSED_PARTICLE_VERTEX_BYTES * 4L, GL30C.GL_MAP_READ_BIT);
		if (data != null) {
			data.order(ByteOrder.LITTLE_ENDIAN);
			System.out.println("=== CS 输出数据 ===");
			for (int i = 0; i < particleCount * 4; i++) {
				System.out.println("粒子 " + i + ":");
				int index = i * ParticleVertexFormats.COMPUTE_SHADER_PROCESSED_PARTICLE_VERTEX_BYTES;
				float posX = data.getFloat(index);
				index += 4;
				float posY = data.getFloat(index);
				index += 4;
				float posZ = data.getFloat(index);
				index += 4;
				data.getFloat(index); // padding
				index += 4;
				float u = data.getFloat(index);
				index += 4;
				float v = data.getFloat(index);
				index += 4;
				data.getFloat(index); // padding
				index += 4;
				data.getFloat(index); // padding
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
				index += 4;
				data.getInt(index); // padding
				index += 4;
				data.getInt(index); // padding

				System.out.printf("  Pos=(%.2f, %.2f, %.2f)%n", posX, posY, posZ);
				System.out.printf("  UV=(%.2f, %.2f)%n", u, v);
				System.out.printf("  Color=(%.2f, %.2f, %.2f, %.2f)%n", r, g, b, a);
				System.out.printf("  Light=(%d, %d)%n", block, sky);
			}

			// 3. 解除映射
			GL15.glUnmapBuffer(GL43C.GL_SHADER_STORAGE_BUFFER);
		}

		GL15.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, 0);
	}

}

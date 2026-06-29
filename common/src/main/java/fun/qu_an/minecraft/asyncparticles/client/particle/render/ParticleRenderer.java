package fun.qu_an.minecraft.asyncparticles.client.particle.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormat;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.GLCaps;
import fun.qu_an.minecraft.asyncparticles.client.particle.buffer.ParticleVertexBuffer;
import fun.qu_an.minecraft.asyncparticles.client.particle.shader.ParticleTransformFeedbackShader;
import fun.qu_an.minecraft.asyncparticles.client.util.MemStackUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Queue;

@Environment(EnvType.CLIENT)
public class ParticleRenderer implements IParticleRenderer {
	private static final int SOURCE_SLOT_COUNT = 3;
	private static final int[] multiDrawIndex = {0, 0};
	private static final int[] multiDrawCount = {0, 0};
	private final ParticleVertexBuffer[] sources = new ParticleVertexBuffer[SOURCE_SLOT_COUNT];
	private final long[] fences = new long[SOURCE_SLOT_COUNT];
	private final ParticleVertexBuffer target = new ParticleVertexBuffer(true);
	private int particleLimit;
	private int processingSrcIdx = 0;
	private int renderSrcIdx = 0;
	private ByteBuffer mappedBuffer;
	private final Vec3[] cameraPositions = new Vec3[SOURCE_SLOT_COUNT];
	private final int[] particleCount = new int[SOURCE_SLOT_COUNT * 2];
	private boolean shouldSkip = true;
	private final int tf;
	private boolean computed = false;

	public ParticleRenderer(int particleLimit) {
		for (int i = 0; i < SOURCE_SLOT_COUNT; i++) {
			sources[i] = new ParticleVertexBuffer(false);
			sources[i].bind();
			ParticleVertexFormats.RAW_PARTICLE.setupBufferState(); // attributes
			cameraPositions[i] = Vec3.ZERO;
		}

		target.bind();
		ParticleVertexFormats.PROCESSED_PARTICLE.setupBufferState(); // attributes
		RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		autoStorageIndexBuffer.bind(256);
		ParticleVertexBuffer.unbind();

		tf = GLCaps.tfSupport.genTransformFeedback();
		if (tf > 0){
			GLCaps.tfSupport.glBindTransformFeedback(tf);
			GLCaps.tfSupport.glBindTransformFeedbackBuffer(target.vbo);
			GLCaps.tfSupport.glBindTransformFeedback(0);
		}
		resize(particleLimit); // this.particleLimit = particleLimit;
	}

	public void beginFrame() {
		computed = false;
	}

	@Override
	public void unmapBufferAndSwap() {
		if (mappedBuffer != null) {
			unmapBuffer();
			shouldSkip = particleCount[processingSrcIdx] == 0;
			renderSrcIdx = processingSrcIdx;
			processingSrcIdx = acquireSourceSlot(processingSrcIdx);
		} else {
			shouldSkip = true;
		}
		this.particleCount[processingSrcIdx] = 0;
	}

	private int acquireSourceSlot(int idx) {
		if (fences[idx] == 0) {
			fences[idx] = GL32C.glFenceSync(GL32C.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
		}
		int newIdx = (idx + 1) % SOURCE_SLOT_COUNT;
		long fence = fences[newIdx];
		if (fence != 0) {
			GL32C.glWaitSync(fence, 0, GL32C.GL_TIMEOUT_IGNORED);
			GL32C.glDeleteSync(fence);
			fences[newIdx] = 0;
		}
		return newIdx;
	}

	@Override
	public void mapBuffer() {
		if (mappedBuffer != null) {
			throw new IllegalStateException("Mapped buffer is not null");
		}
		ParticleVertexBuffer source = sources[processingSrcIdx];
		mappedBuffer = source.map(this.particleLimit * ParticleVertexFormats.RAW_PARTICLE_BYTES);
	}

	@Override
	public void unmapBuffer() {
		RenderSystem.assertOnRenderThread();
		// correct the particle count
		particleCount[processingSrcIdx + 3] = particleCount[processingSrcIdx] - particleLimit;
		particleCount[processingSrcIdx] = Math.min(particleLimit, this.particleCount[processingSrcIdx]);
		if (mappedBuffer == null) {
			throw new IllegalStateException("Mapped buffer is null!");
		}
		sources[processingSrcIdx].unmap(particleCount[processingSrcIdx] * ParticleVertexFormats.RAW_PARTICLE_BYTES);
//		debugPrintBuffer(mappedBuffer);
		mappedBuffer = null;
	}

	@Override
	public boolean isShouldSkip() {
		return shouldSkip;
	}

	@Override
	public void tick(Vec3 cameraPos, Queue<TextureSheetParticle> particles) {
		if (mappedBuffer == null) {
			throw new IllegalStateException("Mapped buffer is null!");
		}
		cameraPositions[processingSrcIdx] = cameraPos;
		double cx = cameraPos.x;
		double cy = cameraPos.y;
		double cz = cameraPos.z;

		long bufferAddress = 0;
		bufferAddress = MemoryUtil.memAddress(mappedBuffer);
		int position = 0;
//		Frustum frustum = gpuBehavior.getFrustum();
//		ParticleCullingMode particleCullingMode = ConfigHelper.getGpuParticleCullingMode();
		try (MemoryStack stack = MemStackUtil.stackPush()) {
			final long address = stack.nmalloc(ParticleVertexFormats.RAW_PARTICLE_BYTES);
			for (TextureSheetParticle particle : particles) {
				GpuParticleAddon gpuParticle = (GpuParticleAddon) particle;
				if (!gpuParticle.asyncparticles$shouldRender()) {
					continue;
				}
//				if (shouldCull(particleCullingMode, frustum, particle)) {
//					continue;
//				}

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

				MemoryUtil.memCopy(address, bufferAddress + (long) position, ParticleVertexFormats.RAW_PARTICLE_BYTES);
				position += ParticleVertexFormats.RAW_PARTICLE_BYTES;
			}
		}
		this.particleCount[processingSrcIdx] = position / ParticleVertexFormats.RAW_PARTICLE_BYTES;
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
		if (tf > 0) {
			GLCaps.tfSupport.glBindTransformFeedback(tf);
		}
		Vector3f cameraLeftVector = camera.getLeftVector();
		Vector3f cameraUpVector = camera.getUpVector();
		Vec3 camPos = camera.getPosition();
		Vec3 lastCamPos = cameraPositions[renderSrcIdx];
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

		int needSize = 4 * particleCount[renderSrcIdx] * ParticleVertexFormats.PROCESSED_PARTICLE_VERTEX_BYTES;
		if (needSize > target.getSize()) {
			target.resize(needSize);
		}
		sources[renderSrcIdx].bind();

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
		int overflow = particleCount[renderSrcIdx + 3];
		if (overflow <= 0) {
			GL11C.glDrawArrays(GL11C.GL_POINTS, 0, particleCount[renderSrcIdx]);
		} else {
			multiDrawIndex[0] = overflow;
			multiDrawCount[0] = this.particleLimit - overflow;
			multiDrawCount[1] = overflow;
			GL14C.glMultiDrawArrays(GL11C.GL_POINTS,
				multiDrawIndex,
				multiDrawCount);
		}
		GLCaps.tfSupport.glEndTransformFeedback();
//		debugReadback(Math.min(particleCount[renderSrcIdx], 16));

		GL11C.glDisable(GL30C.GL_RASTERIZER_DISCARD);

		ParticleVertexBuffer.unbind();

		if (tf > 0) {
			GLCaps.tfSupport.glBindTransformFeedback(0);
		} else {
			GLCaps.tfSupport.glBindTransformFeedbackBuffer(0);
		}

		computed = true;
	}

	@Override
	public void render() {
		RenderSystem.assertOnRenderThread();
		BufferUploader.invalidate();

		target.bind();

		int indexCount = VertexFormat.Mode.QUADS.indexCount(particleCount[renderSrcIdx] * 4);
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
	public void append(Vec3 cameraPos, TextureSheetParticle particle) {
		GpuParticleAddon gpuParticle = (GpuParticleAddon) particle;
		if (!gpuParticle.asyncparticles$shouldRender()) {
			return;
		}
//		ParticleCullingMode particleCullingMode = ConfigHelper.getGpuParticleCullingMode();
//		if (shouldCull(particleCullingMode, gpuBehavior.getFrustum(), tsp)) {
//			return;
//		}
		double cx;
		double cy;
		double cz;
		int particleCount = this.particleCount[processingSrcIdx];
		int particleLimit = this.particleLimit;
		int offset;
		if (particleCount >= particleLimit) {
//			offset = particleCount % particleLimit * ParticleVertexFormats.RAW_PARTICLE_BYTES;
			// particleCount can not greater than 2 * particleLimit, so '%' is unnecessary.
			offset = (particleCount - particleLimit) * ParticleVertexFormats.RAW_PARTICLE_BYTES;
		} else {
			offset = particleCount * ParticleVertexFormats.RAW_PARTICLE_BYTES;
		}
		if (mappedBuffer != null) {
			cx = cameraPositions[processingSrcIdx].x;
			cy = cameraPositions[processingSrcIdx].y;
			cz = cameraPositions[processingSrcIdx].z;
		} else {
			mapBuffer();
			this.cameraPositions[processingSrcIdx] = cameraPos;
			cx = cameraPos.x;
			cy = cameraPos.y;
			cz = cameraPos.z;
		}

		try (MemoryStack stack = MemStackUtil.stackPush()) {
			final long address = stack.nmalloc(ParticleVertexFormats.RAW_PARTICLE_BYTES);
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

			long bufferAddress = MemoryUtil.memAddress(mappedBuffer);
			MemoryUtil.memCopy(address, bufferAddress + (long) offset,
				ParticleVertexFormats.RAW_PARTICLE_BYTES);
			this.particleCount[processingSrcIdx] = particleCount + 1;
		}
	}

	@Override
	public void resize(int particleLimit) {
		int rawSize = particleLimit * ParticleVertexFormats.RAW_PARTICLE_BYTES;
		for (int i = 0; i < SOURCE_SLOT_COUNT; i++) {
			if (rawSize != sources[i].getSize()) {
				sources[i].resize0(rawSize);
			}
		}
		int proceedSize = particleLimit * 4 * ParticleVertexFormats.PROCESSED_PARTICLE_VERTEX_BYTES;
		if (proceedSize != target.getSize()) {
			target.resize0(proceedSize);
		}
		this.particleLimit = particleLimit;
	}
}

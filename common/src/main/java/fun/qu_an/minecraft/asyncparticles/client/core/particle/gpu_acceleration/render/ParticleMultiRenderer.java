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
import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.UnknownNullability;
import org.joml.Vector3fc;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Supplier;

import static fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.render.GpuParticlePipelines.*;

public class ParticleMultiRenderer implements IParticleRenderer {
	protected final int tf;
	Map<SingleQuadParticle.Layer, SubRenderer> rendererMap = new Reference2ReferenceOpenHashMap<>();
	protected final ParticleVertexBuffer target;
	protected final GpuBuffer targetMoj;
	protected final ComputeResult[] computeResults = new ComputeResult[2];
	protected final Vec3[] camPositions = {Vec3.ZERO, Vec3.ZERO};
	protected boolean computed = false;
	protected int processingIndex = 0;
	protected int particleLimit;
	protected final Set<SingleQuadParticle.Layer>[] potentialLayers = new Set[]{Set.of(), Set.of()};

	public ParticleMultiRenderer() {
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
		resize(AsyncParticlesConfig.DEFAULT_PARTICLE_LIMIT); // this.particleLimit = particleLimit;
	}

	@Override
	public void beginFrame() {
		rendererMap.values().forEach(SubRenderer::beginFrame);
		computed = false;
	}

	@Override
	public void unmapBufferAndSwap(Vec3 prevGpuCamPos) {
		rendererMap.values().forEach(SubRenderer::unmapBufferAndSwap);
		processingIndex ^= 1;
	}

	@Override
	public void mapBuffer(@UnknownNullability Supplier<Set<SingleQuadParticle.Layer>> potentialLayer) {
		assert potentialLayer != null;
		Set<SingleQuadParticle.Layer> layers = potentialLayer.get();
		for (SingleQuadParticle.Layer layer : layers) {
			rendererMap.computeIfAbsent(layer, _ -> new SubRenderer(particleLimit)).mapBuffer();
		}
		potentialLayers[processingIndex] = layers;
	}

	@Override
	public boolean isMapped() {
		return true;
	}

	@Override
	public boolean isShouldSkip() {
		return false;
	}

	@Override
	public void tick(Vec3 cameraPos, Map<SingleQuadParticle.Layer, Queue<SingleQuadParticle>> particles) {
		camPositions[processingIndex] = cameraPos;
		for (Map.Entry<SingleQuadParticle.Layer, Queue<SingleQuadParticle>> entry : particles.entrySet()) {
			SingleQuadParticle.Layer key = entry.getKey();
			SubRenderer renderer = rendererMap.get(key);
			renderer.tick(cameraPos, entry.getValue());
		}
	}

	@Override
	public ComputeResult compute(Camera camera, float partialTicks) {
		if (computed) {
			return computeResults[processingIndex ^ 1];
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

		List<ComputeResult.ParticleSlice> slices = new ArrayList<>(rendererMap.size());
		int baseCount = 0;
		int vertexSize = GpuParticlePipelines.IDENTITY_PARTICLE.getVertexSize();
		int summed = 0;
		for (SubRenderer r : rendererMap.values()) {
			if (!r.shouldSkip()){
				summed += r.getParticleCount(true);
			}
		}
		if (summed * 4 * vertexSize > target.getSize()) {
			resizeTarget(summed);
		}
		for (Map.Entry<SingleQuadParticle.Layer, SubRenderer> entry : rendererMap.entrySet()) {
			SubRenderer renderer = entry.getValue();
			if (!renderer.shouldSkip()) {
				ComputeResult.ParticleSlice slice = renderer.compute(tf, target, baseCount, entry.getKey());
				slices.add(slice);
				baseCount += slice.count();
			}
		}

		if (tf > 0) {
			GLCaps.tfSupport.glBindTransformFeedback(0);
		}

		computed = true;
		return computeResults[processingIndex ^ 1] = new ComputeResult(targetMoj, baseCount, slices.toArray(ComputeResult.ParticleSlice[]::new));
	}

	@Override
	public void append(Vec3 cameraPos, SingleQuadParticle sqp) {
		SubRenderer renderer = rendererMap.computeIfAbsent(sqp.getLayer(), _ -> new SubRenderer(particleLimit));
		renderer.append(cameraPos, sqp);
	}

	@Override
	public void resize(int particleLimit) {
		rendererMap.values().forEach(renderer -> renderer.resize(particleLimit));
		resizeTarget(particleLimit);
		this.particleLimit = particleLimit;
	}

	private void resizeTarget(int particleLimit) {
		int proceedSize = 4 * particleLimit * GpuParticlePipelines.IDENTITY_PARTICLE.getVertexSize();
		int size = target.getSize();
		if (proceedSize >= size || proceedSize < size * 0.33f) {
			GlBuffer.MEMORY_POOl.free(target.vbo);
			target.resize0(proceedSize);
			int newSize = target.getSize();
			targetMoj.size = newSize;
			GlBuffer.MEMORY_POOl.malloc(target.vbo, newSize);
		}
	}

	@Override
	public Collection<SingleQuadParticle.Layer> getComputeLayers() {
		return potentialLayers[processingIndex ^ 1];
	}

	private static class SubRenderer {
		protected final int[] particleCount = new int[4];
		protected ByteBuffer mappedBuffer;
		protected final ParticleVertexBuffer[] sources = new ParticleVertexBuffer[2];
		protected final ComputeResult.ParticleSlice[] slices = new ComputeResult.ParticleSlice[2];
		protected int particleLimit;
		protected int processingIndex = 0;
		protected boolean shouldSkip = true;
		protected boolean computed = false;

		public SubRenderer(int particleLimit) {
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

			resize(particleLimit); // this.particleLimit = particleLimit;
		}

		public void beginFrame() {
			computed = false;
		}

		public void unmapBufferAndSwap() {
			if (isMapped()) {
				unmapBuffer();
				shouldSkip = particleCount[processingIndex] == 0;
				processingIndex ^= 1; // Swap processing index.
			} else {
				shouldSkip = true;
			}
			this.particleCount[processingIndex] = 0;
			this.particleCount[2 | processingIndex] = 0;
		}

		public void mapBuffer() {
			if (isMapped()) {
				throw new IllegalStateException("Mapped buffer is not null");
			}
			ParticleVertexBuffer source = sources[processingIndex];
			mappedBuffer = source.map(this.particleLimit * RAW_PARTICLE.getVertexSize());
		}

		public boolean isMapped() {
			return mappedBuffer != null;
		}

		public void unmapBuffer() {
			RenderSystem.assertOnRenderThread();
			// correct the particle count
			particleCount[2 | processingIndex] = Math.max(0, particleCount[processingIndex] - particleLimit);
			particleCount[processingIndex] = Math.min(particleLimit, this.particleCount[processingIndex]);
			if (!isMapped()) {
				throw new IllegalStateException("Mapped buffer is null!");
			}
			sources[processingIndex].unmap(0, particleCount[processingIndex] * RAW_PARTICLE.getVertexSize());
	//		debugPrintBuffer(mappedBuffer);
			mappedBuffer = null;
		}

		public boolean shouldSkip() {
			return shouldSkip;
		}

		public void tick(Vec3 cameraPos, Queue<SingleQuadParticle> particles) {
			if (!isMapped()) {
				throw new IllegalStateException("Mapped buffer is null!");
			}
			if (particles.size() > particleLimit) {
				throw new IllegalStateException("Particle limit exceeded!");
			}
			final double cx = cameraPos.x;
			final double cy = cameraPos.y;
			final double cz = cameraPos.z;

			final long bufferAddress = MemoryUtil.memAddress(mappedBuffer);

			int position = 0;
			final int vertexSize = RAW_PARTICLE.getVertexSize();
			try (final MemoryStack stack = MemStackUtil.stackPush()) {
				final long address = stack.nmalloc(vertexSize);
				for (SingleQuadParticle sqp : particles) {
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
				}
			}
			int particleCount = position / vertexSize;
			this.particleCount[processingIndex] = particleCount;
		}

		public ComputeResult.ParticleSlice compute(int tf, ParticleVertexBuffer target, int baseCount, SingleQuadParticle.Layer layer) {
			if (computed) {
				return slices[processingIndex ^ 1];
			}
			if (shouldSkip) {
				throw new IllegalStateException("Should skip rendering during this tick!");
			}
			sources[processingIndex ^ 1].bind();

			int vertexSize = IDENTITY_PARTICLE.getVertexSize();
			GLCaps.tfSupport.glBindTransformFeedbackBufferRange(Math.max(tf, 0), 0, target.vbo,
				(long) baseCount * 4 * vertexSize,
				(long) particleCount[processingIndex ^ 1] * 4 * vertexSize);

			GL11C.glEnable(GL30C.GL_RASTERIZER_DISCARD);

			GLCaps.tfSupport.glBeginTransformFeedback(GL11C.GL_POINTS);
			int overflow = particleCount[2 | processingIndex ^ 1];
			if (overflow <= 0) {
				GL11C.glDrawArrays(GL11C.GL_POINTS, 0, particleCount[processingIndex ^ 1]);
			} else {
				multiDrawFirst[0] = overflow;
				multiDrawCount[0] = this.particleLimit - overflow;
				multiDrawCount[1] = overflow;
				GL14C.glMultiDrawArrays(GL11C.GL_POINTS,
					multiDrawFirst,
					multiDrawCount);
			}
			GLCaps.tfSupport.glEndTransformFeedback();

			GL11C.glDisable(GL30C.GL_RASTERIZER_DISCARD);

			ParticleVertexBuffer.unbind();

			computed = true;
			return slices[processingIndex ^ 1] = new ComputeResult.ParticleSlice(layer, baseCount, particleCount[processingIndex ^ 1]);
		}

		public void append(Vec3 cameraPos, SingleQuadParticle sqp) {
			if (!((GpuParticleAddon) sqp).asyncparticles$shouldRender()) {
				return;
			}
	//		ParticleCullingMode particleCullingMode = ConfigHelper.getGpuParticleCullingMode();
	//		if (shouldCull(particleCullingMode, gpuBehavior.getFrustum(), sqp)) {
	//			return;
	//		}
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
			final double cx = cameraPos.x;
			final double cy = cameraPos.y;
			final double cz = cameraPos.z;
			if (!isMapped()) {
				mapBuffer();
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

				long bufferAddress = MemoryUtil.memAddress(mappedBuffer);
				MemoryUtil.memCopy(address, bufferAddress + (long) offset,
					vertexSize);
			}
			this.particleCount[processingIndex] = particleCount + 1;
			this.shouldSkip = false;
		}

		public void resize(int particleLimit) {
			int rawSize = particleLimit * RAW_PARTICLE.getVertexSize();
			if (rawSize != sources[0].getSize()) {
				sources[0].resize0(rawSize);
			}
			if (rawSize != sources[1].getSize()) {
				sources[1].resize0(rawSize);
			}
			this.particleLimit = particleLimit;
		}

		public int getParticleCount(boolean using) {
			return particleCount[processingIndex ^ (using ? 1 : 0)];
		}
	}
}

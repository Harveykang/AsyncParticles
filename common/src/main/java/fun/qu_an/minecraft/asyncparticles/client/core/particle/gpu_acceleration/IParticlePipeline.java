package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IParticlePipeline<C> {
	IParticleBatchKey batchKey(TextureSheetParticle particle);

	IParticleRenderKey renderKey(ParticleRenderType layer);

	default ParticleVertexFormat getInputFormat() {
		return ParticleVertexFormats.RAW_PARTICLE;
	}

	/**
	 * Groups particles for extraction, may return the original map.
	 */
	<T extends Collection<TextureSheetParticle>> Map<IParticleBatchKey, Collection<TextureSheetParticle>>
	groupParticles(Map<ParticleRenderType, T> particles);

	LayerBatch createBatch(IParticleBatchKey key, Vec3 snapshotCamPos);

	/**
	 * Returns the existing origin used to encode this batch.
	 */
	Vec3 positionOrigin(LayerBatch batch, Vec3 snapshotCamPos);

	/**
	 * Writes batch snapshots to bufferAddress and returns their count.
	 */
	default int extractBatch(C pipelineState,
							 Collection<TextureSheetParticle> particles,
							 LayerBatch batch,
							 long address,
							 long bufferAddress,
							 Vec3 cameraPos,
							 boolean appending) {
		int vertexSize = ParticleVertexFormats.RAW_PARTICLE.getVertexSize();
		Vec3 origin = positionOrigin(batch, cameraPos);
		final double cx = origin.x;
		final double cy = origin.y;
		final double cz = origin.z;
		int count = 0;
		for (TextureSheetParticle particle : particles) {
			GpuParticleAddon gpuParticle = (GpuParticleAddon) particle;
			// Appends keep the visibility decision made when queued.
			if (!appending && (!particle.isAlive() || !IParticleRenderer.shouldRenderParticle(gpuParticle, cameraPos))) {
				continue;
			}

			// Preserve getter order: some modded particles depend on it.
			MemoryUtil.memPutFloat(address, (float) (gpuParticle.asyncparticles$getXo() - cx));
			MemoryUtil.memPutFloat(address + 4L, (float) (gpuParticle.asyncparticles$getYo() - cy));
			MemoryUtil.memPutFloat(address + 8L, (float) (gpuParticle.asyncparticles$getZo() - cz));
			MemoryUtil.memPutFloat(address + 12L, (float) (gpuParticle.asyncparticles$getX() - cx));
			MemoryUtil.memPutFloat(address + 16L, (float) (gpuParticle.asyncparticles$getY() - cy));
			MemoryUtil.memPutFloat(address + 20L, (float) (gpuParticle.asyncparticles$getZ() - cz));
			MemoryUtil.memPutFloat(address + 24L, gpuParticle.asyncparticles$getQuadSize(0f));
			int oColor = gpuParticle.asyncparticles$getOColor();
			MemoryUtil.memPutInt(address + 48L, oColor);
			MemoryUtil.memPutFloat(address + 60L, gpuParticle.asyncparticles$getORoll());
			MemoryUtil.memPutInt(address + 56L, appending ? gpuParticle.asyncparticles$getCachedLight()
				: gpuParticle.asyncparticles$getGpuLightCoords(0f));
			MemoryUtil.memPutFloat(address + 32L, gpuParticle.asyncparticles$getU0());
			MemoryUtil.memPutFloat(address + 36L, gpuParticle.asyncparticles$getV0());
			MemoryUtil.memPutFloat(address + 40L, gpuParticle.asyncparticles$getU1());
			MemoryUtil.memPutFloat(address + 44L, gpuParticle.asyncparticles$getV1());
			MemoryUtil.memPutFloat(address + 28L, gpuParticle.asyncparticles$getQuadSize(1f));
			MemoryUtil.memPutInt(address + 52L, gpuParticle.asyncparticles$getColor(oColor));
			MemoryUtil.memPutFloat(address + 64L, gpuParticle.asyncparticles$getRoll());
			gpuParticle.asyncparticles$postTick(address, appending);
			MemoryUtil.memCopy(address, bufferAddress, vertexSize);
			bufferAddress += vertexSize;
			count++;
		}
		return count;
	}

	/**
	 * Prepares view state after layout building and returns its reuse condition, null skips computation.
	 */
	@Nullable
	IComputeReuseCondition prepareView(C context, List<LayerBatch> batches, Camera camera, float partialTicks);
}

package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.vulkan;

import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.IParticleBatchStrategy;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.LayerBatch;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

import java.nio.ByteBuffer;
import java.util.List;

public interface IVkParticleBatchStrategy<C> extends IParticleBatchStrategy<C> {
	/**
	 * Creates renderer-local CPU state, or null when the strategy needs none.
	 */
	C createVkRenderInfo();

	VkLayoutResult buildVkLayout(List<LayerBatch> batches, int particleLimit);

	void dispatchVkBatches(C context,
						   long pipelineLayout,
						   ByteBuffer constants,
						   List<LayerBatch> batches,
						   Vec3 snapshotCamPos,
						   Camera camera,
						   float partialTicks,
						   int groupCount);
}

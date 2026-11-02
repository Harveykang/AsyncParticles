package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.vulkan;

import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.IParticlePipeline;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.LayerBatch;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.opengl.ShaderCompiler;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

import java.nio.ByteBuffer;
import java.util.List;

public interface IVkParticlePipeline<C> extends IParticlePipeline<C> {
	/**
	 * Creates renderer-local CPU state, or null when the strategy needs none.
	 */
	C createVkState();

	default byte[] getSpirv() {
		return ShaderCompiler.readSpirv("/assets/asyncparticles/particle_gpu_acceleration/vk_particle_compute.spv");
	}

	default int getVkPushConstantSize() {
		return 44;
	}

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

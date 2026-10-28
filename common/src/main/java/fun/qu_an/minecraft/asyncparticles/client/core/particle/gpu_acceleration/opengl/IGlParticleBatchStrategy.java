package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.opengl;

import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.ParticleLayout;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.IParticleBatchStrategy;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.LayerBatch;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.ParticleVertexFormats;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public interface IGlParticleBatchStrategy<C> extends IParticleBatchStrategy<C> {
	/**
	 * Creates renderer-local CPU state, or null when the strategy needs none.
	 */
	C createGlRenderInfo();

	default ParticleVertexFormat getInputFormat() {
		return ParticleVertexFormats.RAW_PARTICLE;
	}

	ParticleLayout buildGlLayout(C context, List<LayerBatch> batches, int particleLimit);

	void prepareGlCompute(C context,
						  Camera camera,
						  float partialTicks,
						  Vec3 snapshotCamPos,
						  int particleCount,
						  int transformFeedback,
						  int targetBuffer);

	void drawGlCompute(C context,
					   Camera camera,
					   float partialTicks,
					   Vec3 snapshotCamPos,
					   List<LayerBatch> batches,
					   int particleLimit);
}

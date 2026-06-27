package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import net.minecraft.client.particle.SingleQuadParticle;

public final class LayerBatch {
	public final SingleQuadParticle.Layer layer;
	public int tickOffset;    // particle index in source buffer
	public int tickCount;
	public int appendOffset;  // particle index in append region (relative to particleLimit)
	public int appendCount;

	public LayerBatch(SingleQuadParticle.Layer layer) {
		this.layer = layer;
	}
}

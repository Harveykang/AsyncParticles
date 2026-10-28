package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

public class LayerBatch {
	public final IParticleBatchKey key;
	public int tickOffset;
	public int tickCount;
	public int appendOffset;
	public int appendCount;
	public int targetOffset;

	public LayerBatch(IParticleBatchKey key) {
		this.key = key;
	}

	public int count() {
		return tickCount + appendCount;
	}
}

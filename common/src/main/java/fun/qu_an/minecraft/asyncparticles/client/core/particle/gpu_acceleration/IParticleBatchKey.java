package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration;

import net.minecraft.client.particle.ParticleRenderType;

public interface IParticleBatchKey {
	default ParticleRenderType layer() {
		return (ParticleRenderType) this;
	}
}

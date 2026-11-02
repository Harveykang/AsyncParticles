package fun.qu_an.minecraft.asyncparticles.client.compat.particlestorm;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;

public interface ParticleStormParticle extends GpuParticleAddon {
	long asyncparticles$getGpuGeneration();

	void asyncparticles$setGpuGeneration(long generation);

	float[] asyncparticles$getBillboardSize();

	float asyncparticles$getXRot();

	float asyncparticles$getYRot();

	float asyncparticles$getXRotO();

	float asyncparticles$getYRotO();

	int asyncparticles$getParticleStormLight(float partialTickTime);
}
